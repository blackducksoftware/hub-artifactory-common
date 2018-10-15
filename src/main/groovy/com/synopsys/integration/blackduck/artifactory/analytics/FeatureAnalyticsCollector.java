package com.synopsys.integration.blackduck.artifactory.analytics;

import java.util.HashMap;
import java.util.Map;

/**
 * An {@link com.synopsys.integration.blackduck.artifactory.analytics.AnalyticsCollector} that records the number of times a particular function or event is triggered
 */
public class FeatureAnalyticsCollector extends AnalyticsCollector {
    private final Class analyzableClass;
    private final Map<String, Integer> statisticCounter = new HashMap<>();

    public FeatureAnalyticsCollector(final Class analyzableClass) {
        this.analyzableClass = analyzableClass;
    }

    public void logFeatureHit(final String featureName, final Object value) {
        logFeatureHit(featureName, value.toString());
    }

    public void logFeatureHit(final String featureName, final String value) {
        final String statisticName = String.format("feature:%s.%s:%s", analyzableClass.getSimpleName(), featureName, value);
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
