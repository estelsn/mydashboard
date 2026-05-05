package com.aifomo.dashboard.collector.rss;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class RssFeedParser {

    private static final DateTimeFormatter RFC_1123 = DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.ENGLISH);

    public List<RssFeedItem> parse(String xml) {
        if (xml == null || xml.isBlank()) {
            return List.of();
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            return parseItems(document);
        } catch (Exception exception) {
            throw new IllegalArgumentException("RSS XML parse failed", exception);
        }
    }

    private List<RssFeedItem> parseItems(Document document) {
        NodeList itemNodes = document.getElementsByTagName("item");
        List<RssFeedItem> items = new ArrayList<>();
        for (int i = 0; i < itemNodes.getLength(); i++) {
            Node node = itemNodes.item(i);
            if (node instanceof Element itemElement) {
                RssFeedItem item = parseItem(itemElement);
                if (hasRequiredFields(item)) {
                    items.add(item);
                }
            }
        }
        return items;
    }

    private RssFeedItem parseItem(Element itemElement) {
        return new RssFeedItem(
                text(itemElement, "title"),
                text(itemElement, "link"),
                text(itemElement, "description"),
                parsePublishedAt(text(itemElement, "pubDate"))
        );
    }

    private boolean hasRequiredFields(RssFeedItem item) {
        return item.title() != null && !item.title().isBlank()
                && item.link() != null && !item.link().isBlank();
    }

    private String text(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return "";
        }
        String value = nodes.item(0).getTextContent();
        return value == null ? "" : value.trim();
    }

    private LocalDateTime parsePublishedAt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ZonedDateTime.parse(value, RFC_1123)
                    .withZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(value)
                        .atZoneSameInstant(ZoneId.systemDefault())
                        .toLocalDateTime();
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }
}
