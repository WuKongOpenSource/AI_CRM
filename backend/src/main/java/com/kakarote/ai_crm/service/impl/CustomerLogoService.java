package com.kakarote.ai_crm.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.kakarote.ai_crm.entity.PO.Customer;
import com.kakarote.ai_crm.mapper.CustomerMapper;
import com.kakarote.ai_crm.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.IDN;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerLogoService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);
    private static final int MAX_REDIRECTS = 5;
    private static final int MAX_RESPONSE_BYTES = 1024 * 1024;
    private static final String USER_AGENT = "AI-CRM Logo Fetcher/1.0";
    private static final String DEFAULT_IMAGE_CONTENT_TYPE = "image/png";
    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final Pattern LINK_TAG_PATTERN = Pattern.compile("<link\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile(
            "([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s>]+))",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SCHEME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://");
    private static final Set<String> SUPPORTED_IMAGE_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/gif",
            "image/webp",
            "image/x-icon",
            "image/vnd.microsoft.icon",
            "image/ico"
    );
    private static final Set<String> SUPPORTED_IMAGE_EXTENSIONS = Set.of(
            "png",
            "jpg",
            "jpeg",
            "gif",
            "webp",
            "ico"
    );

    private final CustomerMapper customerMapper;
    private final FileStorageService fileStorageService;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    /**
     * 标准化网站。
     */
    public String normalizeWebsite(String website) {
        String trimmed = StrUtil.trimToNull(website);
        if (trimmed == null) {
            return null;
        }
        try {
            URI uri = buildWebsiteUri(trimmed);
            String scheme = StrUtil.blankToDefault(uri.getScheme(), "https").toLowerCase(Locale.ROOT);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                return trimmed;
            }
            String host = uri.getHost();
            if (StrUtil.isBlank(host)) {
                return trimmed;
            }
            String normalizedPath = "/".equals(uri.getRawPath()) ? null : uri.getRawPath();
            URI normalized = new URI(
                    scheme,
                    null,
                    host.toLowerCase(Locale.ROOT),
                    uri.getPort(),
                    normalizedPath,
                    null,
                    null
            );
            return normalized.toString();
        } catch (Exception exception) {
            return trimmed;
        }
    }

    /**
     * 解析Logo地址。
     */
    public String resolveLogoUrl(String logo) {
        String normalizedLogo = StrUtil.trimToNull(logo);
        if (normalizedLogo == null) {
            return null;
        }
        if (isExternalUrl(normalizedLogo)) {
            return normalizedLogo;
        }
        try {
            return fileStorageService.getUrl(normalizedLogo);
        } catch (Exception exception) {
            log.warn("resolve customer logo url failed, logo={}", normalizedLogo, exception);
            return null;
        }
    }

    /**
     * 删除已存储LogoQuietly。
     */
    public void deleteStoredLogoQuietly(String logo) {
        String normalizedLogo = StrUtil.trimToNull(logo);
        if (normalizedLogo == null || isExternalUrl(normalizedLogo)) {
            return;
        }
        try {
            fileStorageService.delete(normalizedLogo);
        } catch (Exception exception) {
            log.warn("delete customer logo failed, logo={}", normalizedLogo, exception);
        }
    }

    /**
     * 处理populateCustomerLogo方法逻辑。
     */
    public void populateCustomerLogo(Long customerId, Date expectedRequestedAt) {
        if (customerId == null) {
            return;
        }
        Customer customer = customerMapper.selectByIdIgnoreDataPermission(customerId);
        if (customer == null || !Objects.equals(customer.getStatus(), 1)) {
            return;
        }
        if (StrUtil.isNotEmpty(customer.getLogo())) {
            return;
        }

        String normalizedWebsite = normalizeWebsite(customer.getWebsite());
        if (StrUtil.isBlank(normalizedWebsite)) {
            return;
        }

        Optional<DownloadedLogo> logoOptional = downloadLogo(normalizedWebsite);
        if (logoOptional.isEmpty()) {
            return;
        }

        DownloadedLogo downloadedLogo = logoOptional.get();
        String objectKey = buildLogoObjectKey(customerId, downloadedLogo.extension());
        String storedLogo = fileStorageService.upload(
                new ByteArrayInputStream(downloadedLogo.bytes()),
                downloadedLogo.bytes().length,
                objectKey,
                downloadedLogo.contentType()
        );

        LambdaUpdateWrapper<Customer> updateWrapper = Wrappers.lambdaUpdate(Customer.class)
                .eq(Customer::getCustomerId, customerId)
                .and(wrapper -> wrapper.isNull(Customer::getLogo).or().eq(Customer::getLogo, ""))
                .set(Customer::getLogo, storedLogo);
        if (expectedRequestedAt != null) {
            updateWrapper.eq(Customer::getAiAnalysisRequestedAt, expectedRequestedAt);
        }
        if (!Objects.equals(customer.getWebsite(), normalizedWebsite)) {
            updateWrapper.set(Customer::getWebsite, normalizedWebsite);
        }

        int updated = customerMapper.update(null, updateWrapper);
        if (updated <= 0) {
            deleteStoredLogoQuietly(storedLogo);
            log.info("skip stale customer logo update, customerId={}", customerId);
        }
    }

    /**
     * 下载Logo。
     */
    private Optional<DownloadedLogo> downloadLogo(String website) {
        try {
            URI websiteUri = buildWebsiteUri(website);
            FetchResult websiteResult = fetch(websiteUri, "text/html,application/xhtml+xml,image/*;q=0.8,*/*;q=0.5");
            if (websiteResult == null) {
                return Optional.empty();
            }

            Optional<DownloadedLogo> directImage = toDownloadedLogo(websiteResult);
            if (directImage.isPresent()) {
                return directImage;
            }

            List<URI> iconCandidates = new ArrayList<>();
            if (isHtmlResponse(websiteResult.contentType())) {
                String html = new String(websiteResult.body(), StandardCharsets.UTF_8);
                iconCandidates.addAll(extractIconCandidates(websiteResult.finalUri(), html));
            }
            iconCandidates.add(buildOriginUri(websiteResult.finalUri()).resolve("/favicon.ico"));

            for (URI candidate : iconCandidates) {
                try {
                    FetchResult candidateResult = fetch(candidate, "image/*,*/*;q=0.5");
                    if (candidateResult == null) {
                        continue;
                    }
                    Optional<DownloadedLogo> downloadedLogo = toDownloadedLogo(candidateResult);
                    if (downloadedLogo.isPresent()) {
                        return downloadedLogo;
                    }
                } catch (Exception candidateException) {
                    log.debug("skip logo candidate: {}", candidate, candidateException);
                }
            }
        } catch (Exception exception) {
            log.warn("fetch customer logo failed, website={}", website, exception);
        }
        return Optional.empty();
    }

    /**
     * 处理fetch方法逻辑。
     */
    private FetchResult fetch(URI uri, String acceptHeader) throws Exception {
        URI current = uri;
        for (int redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount++) {
            validateFetchUri(current);
            HttpRequest request = HttpRequest.newBuilder(current)
                    .timeout(REQUEST_TIMEOUT)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", acceptHeader)
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int statusCode = response.statusCode();

            try (InputStream bodyStream = response.body()) {
                if (isRedirect(statusCode)) {
                    String location = response.headers().firstValue("Location").orElse(null);
                    if (StrUtil.isBlank(location)) {
                        return null;
                    }
                    current = current.resolve(location);
                    continue;
                }
                if (statusCode < 200 || statusCode >= 300) {
                    return null;
                }
                byte[] body = readBody(bodyStream, MAX_RESPONSE_BYTES);
                String contentType = response.headers()
                        .firstValue("Content-Type")
                        .map(CustomerLogoService::normalizeContentType)
                        .orElse(null);
                return new FetchResult(current, contentType, body);
            }
        }
        return null;
    }

    /**
     * 转换为DownloadedLogo。
     */
    private Optional<DownloadedLogo> toDownloadedLogo(FetchResult fetchResult) {
        if (fetchResult == null || fetchResult.body().length == 0) {
            return Optional.empty();
        }
        Optional<DetectedImage> imageType = detectImage(fetchResult.contentType(), fetchResult.finalUri(), fetchResult.body());
        if (imageType.isEmpty()) {
            return Optional.empty();
        }
        DetectedImage detectedImage = imageType.get();
        return Optional.of(new DownloadedLogo(fetchResult.body(), detectedImage.contentType(), detectedImage.extension()));
    }

    /**
     * 处理detectImage方法逻辑。
     */
    private Optional<DetectedImage> detectImage(String contentType, URI sourceUri, byte[] body) {
        String normalizedContentType = normalizeContentType(contentType);
        if (SUPPORTED_IMAGE_CONTENT_TYPES.contains(normalizedContentType)) {
            return Optional.of(new DetectedImage(toCanonicalContentType(normalizedContentType), extensionFromContentType(normalizedContentType)));
        }

        Optional<DetectedImage> signatureImage = detectBySignature(body);
        if (signatureImage.isPresent()) {
            return signatureImage;
        }

        String extension = FileUtil.extName(sourceUri == null ? null : sourceUri.getPath()).toLowerCase(Locale.ROOT);
        if (SUPPORTED_IMAGE_EXTENSIONS.contains(extension)) {
            return Optional.of(new DetectedImage(contentTypeFromExtension(extension), extension));
        }
        return Optional.empty();
    }

    /**
     * 处理detectBySignature方法逻辑。
     */
    private Optional<DetectedImage> detectBySignature(byte[] body) {
        if (body.length >= 8
                && (body[0] & 0xFF) == 0x89
                && body[1] == 0x50
                && body[2] == 0x4E
                && body[3] == 0x47) {
            return Optional.of(new DetectedImage("image/png", "png"));
        }
        if (body.length >= 3
                && (body[0] & 0xFF) == 0xFF
                && (body[1] & 0xFF) == 0xD8
                && (body[2] & 0xFF) == 0xFF) {
            return Optional.of(new DetectedImage("image/jpeg", "jpg"));
        }
        if (body.length >= 6
                && body[0] == 'G'
                && body[1] == 'I'
                && body[2] == 'F') {
            return Optional.of(new DetectedImage("image/gif", "gif"));
        }
        if (body.length >= 12
                && body[0] == 'R'
                && body[1] == 'I'
                && body[2] == 'F'
                && body[3] == 'F'
                && body[8] == 'W'
                && body[9] == 'E'
                && body[10] == 'B'
                && body[11] == 'P') {
            return Optional.of(new DetectedImage("image/webp", "webp"));
        }
        if (body.length >= 4
                && body[0] == 0x00
                && body[1] == 0x00
                && body[2] == 0x01
                && body[3] == 0x00) {
            return Optional.of(new DetectedImage("image/x-icon", "ico"));
        }
        return Optional.empty();
    }

    /**
     * 处理extractIconCandidates方法逻辑。
     */
    private List<URI> extractIconCandidates(URI baseUri, String html) {
        LinkedHashSet<URI> candidates = new LinkedHashSet<>();
        Matcher matcher = LINK_TAG_PATTERN.matcher(html);
        while (matcher.find()) {
            Map<String, String> attributes = parseAttributes(matcher.group());
            String rel = StrUtil.blankToDefault(attributes.get("rel"), "").toLowerCase(Locale.ROOT);
            if (!rel.contains("icon")) {
                continue;
            }
            String href = StrUtil.trimToNull(attributes.get("href"));
            if (href == null || href.startsWith("data:")) {
                continue;
            }
            try {
                candidates.add(baseUri.resolve(href));
            } catch (Exception ignored) {
                // Ignore invalid href and keep trying other candidates.
            }
        }
        return new ArrayList<>(candidates);
    }

    /**
     * 解析Attributes。
     */
    private Map<String, String> parseAttributes(String tag) {
        Map<String, String> attributes = new LinkedHashMap<>();
        Matcher matcher = ATTRIBUTE_PATTERN.matcher(tag);
        while (matcher.find()) {
            String value = matcher.group(3);
            if (value == null) {
                value = matcher.group(4);
            }
            if (value == null) {
                value = matcher.group(5);
            }
            attributes.put(matcher.group(1).toLowerCase(Locale.ROOT), value);
        }
        return attributes;
    }

    /**
     * 读取内容。
     */
    private byte[] readBody(InputStream inputStream, int maxBytes) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new IllegalStateException("response body too large");
            }
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }

    /**
     * 构建网站URI。
     */
    private URI buildWebsiteUri(String website) throws URISyntaxException {
        String normalized = StrUtil.trim(website);
        if (!SCHEME_PATTERN.matcher(normalized).find()) {
            normalized = "https://" + normalized;
        }
        return new URI(normalized);
    }

    /**
     * 构建OriginURI。
     */
    private URI buildOriginUri(URI uri) throws URISyntaxException {
        return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), null, null, null);
    }

    /**
     * 校验FetchURI。
     */
    private void validateFetchUri(URI uri) throws UnknownHostException {
        if (uri == null) {
            throw new IllegalArgumentException("uri is null");
        }
        String scheme = StrUtil.blankToDefault(uri.getScheme(), "").toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new IllegalArgumentException("unsupported scheme");
        }
        if (StrUtil.isNotBlank(uri.getUserInfo())) {
            throw new IllegalArgumentException("userinfo is not allowed");
        }
        String host = StrUtil.trimToNull(uri.getHost());
        if (host == null) {
            throw new IllegalArgumentException("host is missing");
        }
        String asciiHost = IDN.toASCII(host, IDN.ALLOW_UNASSIGNED);
        if ("localhost".equalsIgnoreCase(asciiHost) || asciiHost.endsWith(".local")) {
            throw new IllegalArgumentException("private host is not allowed");
        }
        InetAddress[] addresses = InetAddress.getAllByName(asciiHost);
        if (addresses.length == 0) {
            throw new IllegalArgumentException("host can not be resolved");
        }
        for (InetAddress address : addresses) {
            if (!isPublicAddress(address)) {
                throw new IllegalArgumentException("private address is not allowed");
            }
        }
    }

    /**
     * 判断是否公开Address。
     */
    private boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }
        if (address instanceof Inet4Address ipv4Address) {
            byte[] bytes = ipv4Address.getAddress();
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            if (first == 0
                    || first == 10
                    || first == 127
                    || (first == 100 && second >= 64 && second <= 127)
                    || (first == 169 && second == 254)
                    || (first == 172 && second >= 16 && second <= 31)
                    || (first == 192 && second == 168)
                    || (first == 198 && (second == 18 || second == 19))) {
                return false;
            }
        }
        if (address instanceof Inet6Address ipv6Address) {
            byte[] bytes = ipv6Address.getAddress();
            int first = Byte.toUnsignedInt(bytes[0]);
            if ((first & 0xFE) == 0xFC) {
                return false;
            }
        }
        return true;
    }

    /**
     * 构建LogoObject键。
     */
    private String buildLogoObjectKey(Long customerId, String extension) {
        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);
        String fileName = "customer-" + customerId + "-" + IdUtil.fastSimpleUUID() + "." + extension;
        return "customer-logo/" + datePath + "/" + fileName;
    }

    /**
     * 判断是否HTML响应。
     */
    private boolean isHtmlResponse(String contentType) {
        return "text/html".equals(contentType) || "application/xhtml+xml".equals(contentType);
    }

    /**
     * 判断是否Redirect。
     */
    private boolean isRedirect(int statusCode) {
        return statusCode == 301
                || statusCode == 302
                || statusCode == 303
                || statusCode == 307
                || statusCode == 308;
    }

    /**
     * 判断是否External地址。
     */
    private boolean isExternalUrl(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }

    /**
     * 标准化内容类型。
     */
    private static String normalizeContentType(String contentType) {
        if (StrUtil.isBlank(contentType)) {
            return null;
        }
        int separator = contentType.indexOf(';');
        String normalized = separator >= 0 ? contentType.substring(0, separator) : contentType;
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 处理extensionFromContentType方法逻辑。
     */
    private String extensionFromContentType(String contentType) {
        return switch (normalizeContentType(contentType)) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            case "image/vnd.microsoft.icon", "image/ico", "image/x-icon" -> "ico";
            default -> "png";
        };
    }

    /**
     * 处理contentTypeFromExtension方法逻辑。
     */
    private String contentTypeFromExtension(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "ico" -> "image/x-icon";
            default -> DEFAULT_IMAGE_CONTENT_TYPE;
        };
    }

    /**
     * 转换为Canonical内容类型。
     */
    private String toCanonicalContentType(String contentType) {
        return "image/ico".equals(contentType) ? "image/x-icon" : contentType;
    }

    private record FetchResult(URI finalUri, String contentType, byte[] body) {}

    private record DetectedImage(String contentType, String extension) {}

    private record DownloadedLogo(byte[] bytes, String contentType, String extension) {}
}
