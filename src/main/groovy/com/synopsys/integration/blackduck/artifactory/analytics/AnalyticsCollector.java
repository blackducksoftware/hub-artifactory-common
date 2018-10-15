package com.synopsys.integration.blackduck.artifactory.analytics;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AnalyticsCollector {

    protected static Map<String, String> convertMapValueToString(final Map<String, ?> map) {
        return map.entrySet().stream()
                   .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toString()));
    }

    /**
     * A utility method for combining maps where the key and value are strings.
     * @param map1
     * @param map2
     * @return A combined map. Throws an {@code IllegalStateException} if there are duplicate keys. See {@link Collectors#toMap(java.util.function.Function, java.util.function.Function)}
     */
    protected static Map<String, String> joinMaps(final Map<String, String> map1, final Map<String, String> map2) {
        return Stream.of(map1, map2)
                   .map(Map::entrySet)
                   .flatMap(Collection::stream)
                   .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public abstract Map<String, String> getMetadataMap();

    public abstract void clear();
}
