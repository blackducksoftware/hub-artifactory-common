package com.synopsys.integration.blackduck.artifactory.analytics;

import java.util.HashMap;
import java.util.Map;

public class SimpleAnalyticsCollector extends AnalyticsCollector {
    private final Map<String, String> metadataMap = new HashMap<>();

    public void putMetadata(final String key, final Object value) {
        metadataMap.put(key, value.toString());
    }

    @Override
    public Map<String, String> getMetadataMap() {
        return metadataMap;
    }

    @Override
    public void clear() {
        metadataMap.clear();
    }
}
