package com.synopsys.integration.blackduck.artifactory.analytics;

import java.util.HashMap;
import java.util.Map;

/**
 * An {@link com.synopsys.integration.blackduck.artifactory.analytics.AnalyticsCollector} that records the number of times a particular function or event is triggered
 */
public class FunctionAnalyticsCollector extends AnalyticsCollector {
    private final Class analyzableClass;
    private final Map<String, Integer> statisticCounter = new HashMap<>();

    public FunctionAnalyticsCollector(final Class analyzableClass) {
        this.analyzableClass = analyzableClass;
    }

    public void logFunction(final String functionName, final Object value) {
        logFunction(functionName, value.toString());
    }

    public void logFunction(final String functionName, final String value) {
        final String statisticName = String.format("function:%s.%s:%s", analyzableClass.getSimpleName(), functionName, value);
        incrementStatistic(statisticName);
    }

    public Map<String, String> getMetadataMap() {
        return convertMapValueToString(statisticCounter);
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
