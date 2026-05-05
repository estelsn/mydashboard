package com.aifomo.dashboard.collector.threads;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ThreadsPostParser {

    private static final Pattern THREADS_POST_ARTICLE = Pattern.compile(
            "(?is)<article\\b([^>]*)\\bdata-threads-post\\b([^>]*)>(.*?)</article>"
    );
    private static final Pattern ATTRIBUTE = Pattern.compile(
            "(?is)([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s\"'>]+))"
    );
    private static final Pattern SCRIPT_OR_STYLE = Pattern.compile("(?is)<(script|style)\\b[^>]*>.*?</\\1>");
    private static final Pattern HTML_TAG = Pattern.compile("(?is)<[^>]+>");

    private final ObjectMapper objectMapper;

    public ThreadsPostParser() {
        this(new ObjectMapper());
    }

    ThreadsPostParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<ThreadsParsedPost> parse(String snapshot) {
        if (snapshot == null || snapshot.isBlank()) {
            return List.of();
        }

        String trimmed = snapshot.trim();
        if (looksLikeJson(trimmed)) {
            return parseJson(trimmed);
        }
        return parseHtml(trimmed);
    }

    private List<ThreadsParsedPost> parseHtml(String html) {
        List<ThreadsParsedPost> posts = new ArrayList<>();
        Matcher matcher = THREADS_POST_ARTICLE.matcher(html);

        while (matcher.find()) {
            Map<String, String> attributes = attributes(matcher.group(1) + " " + matcher.group(2));
            String innerHtml = matcher.group(3);
            String body = firstPresent(attributes, "data-body", "data-text", "aria-label")
                    .orElseGet(() -> textFromHtml(innerHtml));
            if (body == null || body.isBlank()) {
                continue;
            }

            String authorIdentifier = firstPresent(attributes, "data-author", "data-author-identifier", "data-username")
                    .orElse(null);
            String postUrl = firstPresent(attributes, "data-url", "data-post-url", "href")
                    .orElseGet(() -> firstLink(innerHtml).orElse(null));
            String displayTime = firstPresent(attributes, "data-time", "data-display-time", "datetime")
                    .orElse(null);
            String rawContent = rawContent(authorIdentifier, body, postUrl, displayTime);
            posts.add(new ThreadsParsedPost(authorIdentifier, body, postUrl, displayTime, rawContent));
        }

        return List.copyOf(posts);
    }

    private List<ThreadsParsedPost> parseJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<ThreadsParsedPost> posts = new ArrayList<>();
            collectJsonPosts(root, posts);
            return List.copyOf(posts);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private void collectJsonPosts(JsonNode node, List<ThreadsParsedPost> posts) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectJsonPosts(child, posts));
            return;
        }
        if (!node.isObject()) {
            return;
        }

        String body = fieldText(node, "body", "text", "caption", "content").orElse(null);
        if (body != null && !body.isBlank()) {
            String authorIdentifier = fieldText(node, "authorIdentifier", "author", "username", "handle")
                    .or(() -> nestedFieldText(node, "author", "username", "handle", "identifier"))
                    .orElse(null);
            String postUrl = fieldText(node, "postUrl", "url", "permalink", "href").orElse(null);
            String displayTime = fieldText(node, "displayTime", "createdAt", "timestamp", "time").orElse(null);
            String rawContent = fieldText(node, "rawContent")
                    .filter(value -> !value.isBlank())
                    .orElseGet(() -> rawContent(authorIdentifier, body, postUrl, displayTime));
            posts.add(new ThreadsParsedPost(authorIdentifier, body, postUrl, displayTime, rawContent));
        }

        node.fields().forEachRemaining(entry -> collectJsonPosts(entry.getValue(), posts));
    }

    private static boolean looksLikeJson(String value) {
        return value.startsWith("{") || value.startsWith("[");
    }

    private static Map<String, String> attributes(String value) {
        Map<String, String> attributes = new LinkedHashMap<>();
        Matcher matcher = ATTRIBUTE.matcher(value);
        while (matcher.find()) {
            String rawValue = matcher.group(3);
            if (rawValue == null) {
                rawValue = matcher.group(4);
            }
            if (rawValue == null) {
                rawValue = matcher.group(5);
            }
            attributes.put(matcher.group(1).toLowerCase(Locale.ROOT), decode(rawValue));
        }
        return attributes;
    }

    private static Optional<String> firstPresent(Map<String, String> attributes, String... names) {
        for (String name : names) {
            String value = attributes.get(name.toLowerCase(Locale.ROOT));
            if (value != null && !value.isBlank()) {
                return Optional.of(value.trim());
            }
        }
        return Optional.empty();
    }

    private static Optional<String> firstLink(String html) {
        return attributes(html).entrySet().stream()
                .filter(entry -> "href".equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    private static String textFromHtml(String html) {
        String withoutScripts = SCRIPT_OR_STYLE.matcher(html).replaceAll(" ");
        String withoutTags = HTML_TAG.matcher(withoutScripts).replaceAll("\n");
        String decoded = decode(withoutTags);
        Set<String> lines = new LinkedHashSet<>();
        for (String line : decoded.split("\\R+")) {
            String trimmed = line.trim();
            if (!trimmed.isBlank()) {
                lines.add(trimmed);
            }
        }
        return String.join("\n", lines);
    }

    private static Optional<String> fieldText(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && value.isValueNode()) {
                String text = value.asText();
                if (text != null && !text.isBlank()) {
                    return Optional.of(text.trim());
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<String> nestedFieldText(JsonNode node, String objectName, String... names) {
        JsonNode object = node.get(objectName);
        if (object == null || !object.isObject()) {
            return Optional.empty();
        }
        return fieldText(object, names);
    }

    private static String rawContent(String authorIdentifier, String body, String postUrl, String displayTime) {
        List<String> parts = new ArrayList<>();
        addPart(parts, authorIdentifier);
        addPart(parts, body);
        addPart(parts, postUrl);
        addPart(parts, displayTime);
        return String.join("\n", parts);
    }

    private static void addPart(List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value.trim());
        }
    }

    private static String decode(String value) {
        return HtmlUtils.htmlUnescape(value == null ? "" : value);
    }
}
