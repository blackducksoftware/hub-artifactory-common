package com.synopsys.integration.blackduck.artifactory.analytics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.BlackDuckConnectionService;
import com.synopsys.integration.blackduck.artifactory.Module;
import com.synopsys.integration.blackduck.artifactory.TriggerType;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class AnalyticsModule extends Module {
    public final static String SUBMIT_ANALYTICS_CRON = "0 0 0 ? * * *"; // Every day at 12 am

    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final BlackDuckConnectionService blackDuckConnectionService;
    private final List<AnalyticsCollector> analyticsCollectors;

    public AnalyticsModule(final BlackDuckConnectionService blackDuckConnectionService, final AnalyticsCollector analyticsCollector) {
        super(analyticsCollector);
        this.blackDuckConnectionService = blackDuckConnectionService;
        analyticsCollectors = new ArrayList<>();
    }

    public void registerModules(final Module... modules) {
        Arrays.stream(modules)
            .map(Module::getAnalyticsCollector)
            .forEach(analyticsCollectors::add);
    }

    /**
     * Submits a payload to phone home with data from all the collectors ({@link com.synopsys.integration.blackduck.artifactory.analytics.AnalyticsCollector})
     * This should be used infrequently such as once a day due to quota
     */
    public void submitAnalytics(final TriggerType triggerType) {
        analyticsCollector.logFunction("submitAnalytics", triggerType);
        
        // Flatten the metadata maps from all of the collectors
        final Map<String, String> metadataMap = analyticsCollectors.stream()
                                                    .map(AnalyticsCollector::getMetadataMap)
                                                    .map(Map::entrySet)
                                                    .flatMap(Collection::stream)
                                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (blackDuckConnectionService.phoneHome(metadataMap)) {
            analyticsCollectors.forEach(AnalyticsCollector::clear);
            logger.debug("Post of analytics data successful");
        } else {
            logger.debug("Failed to post analytics data");
        }
    }
}
