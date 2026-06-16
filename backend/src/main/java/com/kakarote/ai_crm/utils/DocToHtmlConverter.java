package com.kakarote.ai_crm.utils;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.converter.PicturesManager;
import org.apache.poi.hwpf.converter.WordToHtmlConverter;
import org.apache.poi.hwpf.usermodel.PictureType;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Converts legacy .doc (HWPF) files to HTML using Apache POI.
 * Embedded images are inlined as base64 data URIs.
 */
public final class DocToHtmlConverter {

    /**
     * 初始化DOCTOHTMLConverter实例。
     */
    private DocToHtmlConverter() {
    }

    /**
     * 转换TOHTML。
     */
    public static String convertToHtml(InputStream inputStream) throws Exception {
        HWPFDocument document = new HWPFDocument(inputStream);

        Document htmlDoc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .newDocument();

        WordToHtmlConverter converter = new WordToHtmlConverter(htmlDoc);
        converter.setPicturesManager(new Base64PicturesManager());
        converter.processDocument(document);

        Document resultDoc = converter.getDocument();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.METHOD, "html");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(
                new DOMSource(resultDoc),
                new StreamResult(new OutputStreamWriter(out, StandardCharsets.UTF_8))
        );

        return out.toString(StandardCharsets.UTF_8);
    }

    private static class Base64PicturesManager implements PicturesManager {
        /**
         * 保存Picture。
         */
        @Override
        public String savePicture(byte[] content, PictureType pictureType,
                                  String suggestedName, float widthInches, float heightInches) {
            String mime = resolveMime(pictureType);
            String encoded = Base64.getEncoder().encodeToString(content);
            return "data:" + mime + ";base64," + encoded;
        }

        /**
         * 解析Mime。
         */
        private String resolveMime(PictureType type) {
            return switch (type) {
                case JPEG -> "image/jpeg";
                case PNG -> "image/png";
                case GIF -> "image/gif";
                case BMP -> "image/bmp";
                case TIFF -> "image/tiff";
                case WMF -> "image/x-wmf";
                case EMF -> "image/x-emf";
                default -> "application/octet-stream";
            };
        }
    }
}
