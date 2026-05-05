package com.aifomo.dashboard.collector.rss;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

@Component
public class UrlConnectionRssFeedClient implements RssFeedClient {

    @Override
    public String fetch(String url) throws IOException {
        URLConnection connection = URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(5_000);
        connection.setReadTimeout(10_000);
        try (var inputStream = connection.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
