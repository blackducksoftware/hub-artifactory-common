package com.synopsys.integration.blackduck.artifactory.analytics;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.synopsys.integration.blackduck.artifactory.Module;

public class AnalyticsCollector {
    private final Class<? extends Module> moduleClass;
    private final Map<String, Integer> statisticCounter = new HashMap<>();

    public AnalyticsCollector(final Class<? extends Module> moduleClass) {
        this.moduleClass = moduleClass;
    }

    public void logFunction(final String functionName, final Object value) {
        logFunction(functionName, value.toString());
    }

    public void logFunction(final String functionName, final String value) {
        final String statisticName = String.format("function:%s.%s:%s", moduleClass.getSimpleName(), functionName, value);
        incrementStatistic(statisticName);
    }

    public Map<String, String> getMetadataMap() {
        return statisticCounter.entrySet().stream()
                   .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toString()));
    }

    public void clear() {
        statisticCounter.clear();
    }

    private void incrementStatistic(final String statisticName) {
        int count = 1;
        if (statisticCounter.containsKey(statisticName)) {
            count = statisticCounter.get(statisticName) + 1;
        }

        statisticCounter.put(statisticName, count);
    }
}
