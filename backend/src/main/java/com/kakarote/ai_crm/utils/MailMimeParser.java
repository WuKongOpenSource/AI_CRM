package com.kakarote.ai_crm.utils;

import cn.hutool.core.util.StrUtil;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class MailMimeParser {

    private static final int MAX_INLINE_TEXT_BYTES = 2_000_000;

    private MailMimeParser() {
    }

    public static ParsedMail parse(MimeMessage message) throws Exception {
        StringBuilder text = new StringBuilder();
        StringBuilder html = new StringBuilder();
        List<ParsedAttachment> attachments = new ArrayList<>();
        parsePart(message, text, html, attachments);
        String textValue = StrUtil.trimToNull(text.toString());
        if (textValue == null && !html.isEmpty()) {
            textValue = htmlToText(html.toString());
        }
        return new ParsedMail(textValue, StrUtil.trimToNull(html.toString()), attachments);
    }

    public static String toAddressList(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return null;
        }
        List<String> values = new ArrayList<>();
        for (Address address : addresses) {
            values.add(formatAddress(address));
        }
        return String.join(", ", values);
    }

    public static String firstAddress(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return null;
        }
        return extractAddress(addresses[0]);
    }

    public static String firstName(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return null;
        }
        if (addresses[0] instanceof InternetAddress internetAddress) {
            return StrUtil.trimToNull(internetAddress.getPersonal());
        }
        return null;
    }

    public static byte[] toRawBytes(MimeMessage message) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        message.writeTo(outputStream);
        return outputStream.toByteArray();
    }

    private static void parsePart(Part part, StringBuilder text, StringBuilder html, List<ParsedAttachment> attachments) throws Exception {
        String fileName = StrUtil.trimToNull(part.getFileName());
        String disposition = StrUtil.nullToEmpty(part.getDisposition());
        boolean attachment = Part.ATTACHMENT.equalsIgnoreCase(disposition)
                || (fileName != null && !part.isMimeType("text/plain") && !part.isMimeType("text/html"));

        if (attachment) {
            byte[] bytes = readPartBytes(part);
            String contentType = normalizeContentType(part.getContentType());
            String attachmentText = contentType != null && contentType.startsWith("text/")
                    ? new String(bytes, StandardCharsets.UTF_8)
                    : null;
            attachments.add(new ParsedAttachment(
                    StrUtil.blankToDefault(fileName, "attachment"),
                    contentType,
                    bytes,
                    StrUtil.trimToNull(attachmentText)
            ));
            return;
        }

        if (part.isMimeType("text/plain")) {
            Object content = part.getContent();
            if (content != null) {
                text.append(content).append('\n');
            }
            return;
        }

        if (part.isMimeType("text/html")) {
            Object content = part.getContent();
            if (content != null) {
                html.append(content).append('\n');
            }
            return;
        }

        Object content = part.getContent();
        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                parsePart(bodyPart, text, html, attachments);
            }
        }
    }

    private static byte[] readPartBytes(Part part) throws Exception {
        try (InputStream inputStream = part.getInputStream()) {
            return inputStream.readNBytes(MAX_INLINE_TEXT_BYTES);
        }
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        int separator = contentType.indexOf(';');
        return (separator >= 0 ? contentType.substring(0, separator) : contentType)
                .trim()
                .toLowerCase();
    }

    private static String formatAddress(Address address) {
        if (address instanceof InternetAddress internetAddress) {
            String email = internetAddress.getAddress();
            String name = StrUtil.trimToNull(internetAddress.getPersonal());
            return name == null ? email : name + " <" + email + ">";
        }
        return address == null ? "" : address.toString();
    }

    private static String extractAddress(Address address) {
        if (address instanceof InternetAddress internetAddress) {
            return internetAddress.getAddress();
        }
        return address == null ? null : address.toString();
    }

    private static String htmlToText(String html) {
        String withoutScripts = html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ");
        String withoutTags = withoutScripts.replaceAll("(?is)<br\\s*/?>", "\n").replaceAll("(?is)<[^>]+>", " ");
        return StrUtil.trimToNull(withoutTags.replace("&nbsp;", " ").replace("&amp;", "&").replaceAll("\\s+", " "));
    }

    public record ParsedMail(String text, String html, List<ParsedAttachment> attachments) {
    }

    public record ParsedAttachment(String fileName, String contentType, byte[] bytes, String text) {
    }
}
