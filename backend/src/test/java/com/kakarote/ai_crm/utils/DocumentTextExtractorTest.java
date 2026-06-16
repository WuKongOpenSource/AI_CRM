package com.kakarote.ai_crm.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentTextExtractorTest {

    @Test
    void shouldSuppressDuplicateOverlappingTextWhenParsingPdf() throws Exception {
        byte[] pdfBytes = createPdfWithOverlappingText("SAAS workflow");

        String extracted = DocumentTextExtractor.parseToString(
            new ByteArrayInputStream(pdfBytes),
            "application/pdf",
            "sample.pdf"
        );

        String normalized = extracted.replaceAll("\\s+", " ").trim();
        assertThat(normalized).contains("SAAS workflow");
        assertThat(normalized).doesNotContain("SAAS workflow SAAS workflow");
    }

    private byte[] createPdfWithOverlappingText(String text) throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 14);
                contentStream.newLineAtOffset(72, 720);
                contentStream.showText(text);
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 14);
                contentStream.newLineAtOffset(72, 720);
                contentStream.showText(text);
                contentStream.endText();
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}
