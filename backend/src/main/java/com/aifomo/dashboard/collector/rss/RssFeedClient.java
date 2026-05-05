package com.aifomo.dashboard.collector.rss;

import java.io.IOException;

public interface RssFeedClient {

    String fetch(String url) throws IOException;
}
