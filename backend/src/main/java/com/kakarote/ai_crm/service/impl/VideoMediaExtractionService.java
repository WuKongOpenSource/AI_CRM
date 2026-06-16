package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.kakarote.ai_crm.common.exception.BusinessException;
import com.kakarote.ai_crm.common.result.SystemCodeEnum;
import com.kakarote.ai_crm.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Extracts lightweight AI-analysis inputs from stored video files.
 */
@Slf4j
@Service
public class VideoMediaExtractionService {

    private static final int MAX_VIDEO_FRAME_COUNT = 10;
    private static final String VIDEO_PROCESSOR_MISSING_MESSAGE =
        "视频处理组件未配置，请配置 ffmpeg/ffprobe 后重试";

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${media-analysis.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${media-analysis.ffprobe.path:ffprobe}")
    private String ffprobePath;

    @Value("${media-analysis.video.max-frames:5}")
    private int maxFrames;

    @Value("${media-analysis.video.timeout-seconds:60}")
    private long timeoutSeconds;

    /**
     * Extract audio bytes and representative video frames.
     */
    public VideoExtractionResult extract(String filePath, String fileName) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("aicrm-video-analysis-");
            Path sourcePath = tempDir.resolve("source" + resolveVideoSuffix(fileName));
            try (InputStream inputStream = fileStorageService.getFileStream(filePath)) {
                if (inputStream == null) {
                    throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "视频文件无法读取，请重新上传后重试");
                }
                Files.copy(inputStream, sourcePath, StandardCopyOption.REPLACE_EXISTING);
            }

            Duration timeout = Duration.ofSeconds(Math.max(5L, timeoutSeconds));
            OptionalDouble duration = probeDuration(sourcePath, timeout);
            byte[] audioBytes = extractAudio(sourcePath, tempDir, timeout);
            List<VideoFrame> frames = extractFrames(sourcePath, tempDir, duration, timeout);
            if (frames.isEmpty()) {
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "视频无法抽取有效画面，请确认文件可播放后重试");
            }
            String audioFileName = FileUtil.mainName(StrUtil.blankToDefault(fileName, "video")) + "-audio.mp3";
            return new VideoExtractionResult(audioBytes, audioFileName, "audio/mpeg", frames);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Video media extraction failed: fileName={}", fileName, ex);
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "视频处理失败，请稍后重试");
        } finally {
            deleteQuietly(tempDir);
        }
    }

    private OptionalDouble probeDuration(Path sourcePath, Duration timeout) {
        List<String> command = List.of(
            resolveToolPath(ffprobePath, "ffprobe"),
            "-v", "error",
            "-show_entries", "format=duration",
            "-of", "default=noprint_wrappers=1:nokey=1",
            sourcePath.toString()
        );
        CommandResult result = runCommand(command, timeout);
        if (result.exitCode() != 0) {
            log.warn("ffprobe failed, fallback to default frame timestamps: {}", result.output());
            return OptionalDouble.empty();
        }
        try {
            double seconds = Double.parseDouble(result.output().trim());
            return seconds > 0 ? OptionalDouble.of(seconds) : OptionalDouble.empty();
        } catch (Exception ex) {
            log.warn("Failed to parse ffprobe duration output: {}", result.output());
            return OptionalDouble.empty();
        }
    }

    private byte[] extractAudio(Path sourcePath, Path tempDir, Duration timeout) {
        Path audioPath = tempDir.resolve("audio.mp3");
        List<String> command = List.of(
            resolveToolPath(ffmpegPath, "ffmpeg"),
            "-hide_banner", "-loglevel", "error",
            "-y",
            "-i", sourcePath.toString(),
            "-map", "0:a:0",
            "-vn",
            "-ac", "1",
            "-ar", "16000",
            "-f", "mp3",
            audioPath.toString()
        );
        CommandResult result = runCommand(command, timeout);
        if (result.exitCode() != 0 || !Files.isRegularFile(audioPath)) {
            log.info("No usable audio track extracted from video: {}", result.output());
            return null;
        }
        try {
            byte[] bytes = Files.readAllBytes(audioPath);
            return bytes.length == 0 ? null : bytes;
        } catch (IOException ex) {
            log.warn("Failed to read extracted video audio", ex);
            return null;
        }
    }

    private List<VideoFrame> extractFrames(Path sourcePath, Path tempDir, OptionalDouble duration, Duration timeout) {
        List<Double> timestamps = resolveFrameTimestamps(duration);
        List<VideoFrame> frames = new ArrayList<>();
        int index = 1;
        for (Double timestamp : timestamps) {
            Path framePath = tempDir.resolve("frame-" + index + ".jpg");
            List<String> command = List.of(
                resolveToolPath(ffmpegPath, "ffmpeg"),
                "-hide_banner", "-loglevel", "error",
                "-y",
                "-ss", String.format(Locale.ROOT, "%.3f", timestamp),
                "-i", sourcePath.toString(),
                "-frames:v", "1",
                "-q:v", "3",
                framePath.toString()
            );
            CommandResult result = runCommand(command, timeout);
            if (result.exitCode() == 0 && Files.isRegularFile(framePath)) {
                try {
                    byte[] bytes = Files.readAllBytes(framePath);
                    if (bytes.length > 0) {
                        frames.add(new VideoFrame(framePath.getFileName().toString(), "image/jpeg", bytes));
                    }
                } catch (IOException ex) {
                    log.warn("Failed to read extracted video frame: {}", framePath, ex);
                }
            } else {
                log.debug("Video frame extraction skipped at {}s: {}", timestamp, result.output());
            }
            index++;
        }
        return frames;
    }

    private List<Double> resolveFrameTimestamps(OptionalDouble duration) {
        int count = Math.max(1, Math.min(maxFrames, MAX_VIDEO_FRAME_COUNT));
        List<Double> timestamps = new ArrayList<>();
        if (duration.isPresent() && duration.getAsDouble() > 0) {
            double seconds = duration.getAsDouble();
            count = Math.min(count, Math.max(1, (int) Math.ceil(seconds)));
            for (int i = 1; i <= count; i++) {
                timestamps.add(Math.max(0D, seconds * i / (count + 1D)));
            }
            return timestamps;
        }

        double[] defaults = {0D, 2D, 5D, 10D, 15D, 30D, 45D, 60D, 90D, 120D};
        for (int i = 0; i < count; i++) {
            timestamps.add(defaults[i]);
        }
        return timestamps;
    }

    private CommandResult runCommand(List<String> command, Duration timeout) {
        try {
            Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, "视频处理超时，请上传更短的视频后重试");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            return new CommandResult(process.exitValue(), output);
        } catch (IOException ex) {
            throw new BusinessException(SystemCodeEnum.SYSTEM_NO_VALID, VIDEO_PROCESSOR_MISSING_MESSAGE);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(SystemCodeEnum.SYSTEM_ERROR, "视频处理被中断，请稍后重试");
        }
    }

    private String resolveToolPath(String configuredPath, String fallback) {
        return StrUtil.blankToDefault(configuredPath, fallback).trim();
    }

    private String resolveVideoSuffix(String fileName) {
        String extension = FileUtil.extName(fileName);
        return StrUtil.isBlank(extension) ? ".video" : "." + extension;
    }

    private void deleteQuietly(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(path)) {
            paths.sorted(Comparator.reverseOrder()).forEach(item -> {
                try {
                    Files.deleteIfExists(item);
                } catch (IOException ex) {
                    log.debug("Failed to delete temp media file: {}", item, ex);
                }
            });
        } catch (IOException ex) {
            log.debug("Failed to cleanup temp video analysis directory: {}", path, ex);
        }
    }

    private record CommandResult(int exitCode, String output) {
    }

    public record VideoExtractionResult(
        byte[] audioBytes,
        String audioFileName,
        String audioContentType,
        List<VideoFrame> frames
    ) {
        public boolean hasAudio() {
            return audioBytes != null && audioBytes.length > 0;
        }
    }

    public record VideoFrame(String fileName, String mimeType, byte[] bytes) {
    }
}
