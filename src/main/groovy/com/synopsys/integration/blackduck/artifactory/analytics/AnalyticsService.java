package com.synopsys.integration.blackduck.artifactory.analytics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.synopsys.integration.blackduck.artifactory.BlackDuckConnectionService;

public class AnalyticsService {
    private final BlackDuckConnectionService blackDuckConnectionService;
    private final List<AnalyticsCollector> analyticsCollectors = new ArrayList<>();

    public AnalyticsService(final BlackDuckConnectionService blackDuckConnectionService) {
        this.blackDuckConnectionService = blackDuckConnectionService;
    }

    public void registerModule(final Analyzable module) {
        Optional.ofNullable(module.getAnalyticsCollectors())
            .map(analyticsCollectors::addAll);
    }

    /**
     * Submits a payload to phone home with data from all the collectors ({@link AnalyticsCollector})
     * This should be used infrequently such as once a day due to quota
     */
    public void submitAnalytics() {
        // Flatten the metadata maps from all of the collectors
        final Map<String, String> metadataMap = analyticsCollectors.stream()
                                                    .map(AnalyticsCollector::getMetadataMap)
                                                    .map(Map::entrySet)
                                                    .flatMap(Collection::stream)
                                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (blackDuckConnectionService.phoneHome(metadataMap)) {
            analyticsCollectors.forEach(AnalyticsCollector::clear);
        }
    }
}
