package com.kakarote.ai_crm.utils;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;

import java.io.InputStream;
import java.util.Locale;

/**
 * Shared document text extraction helpers.
 */
public final class DocumentTextExtractor {

    private static final Tika TIKA = new Tika();

    /**
     * 初始化文档文本Extractor实例。
     */
    private DocumentTextExtractor() {
    }

    /**
     * 解析TOString。
     */
    public static String parseToString(InputStream inputStream, String mimeType, String fileName) throws Exception {
        if (isPdf(mimeType, fileName)) {
            return parsePdfWithoutOcr(inputStream);
        }
        return TIKA.parseToString(inputStream);
    }

    /**
     * 解析PDF包含outOCR。
     */
    private static String parsePdfWithoutOcr(InputStream inputStream) throws Exception {
        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();

        PDFParserConfig pdfParserConfig = new PDFParserConfig();
        pdfParserConfig.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.NO_OCR);
        // Many exported PDFs contain overlapping text layers; suppress duplicates by default.
        pdfParserConfig.setSuppressDuplicateOverlappingText(true);
        context.set(PDFParserConfig.class, pdfParserConfig);

        TesseractOCRConfig ocrConfig = new TesseractOCRConfig();
        ocrConfig.setSkipOcr(true);
        context.set(TesseractOCRConfig.class, ocrConfig);

        BodyContentHandler handler = new BodyContentHandler(-1);
        parser.parse(inputStream, handler, metadata, context);
        return handler.toString();
    }

    /**
     * 判断是否PDF。
     */
    private static boolean isPdf(String mimeType, String fileName) {
        if ("application/pdf".equalsIgnoreCase(mimeType)) {
            return true;
        }
        if (fileName == null) {
            return false;
        }
        return fileName.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }
}
