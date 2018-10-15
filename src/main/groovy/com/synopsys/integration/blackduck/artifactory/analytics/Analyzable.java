package com.synopsys.integration.blackduck.artifactory.analytics;

import java.util.List;

public interface Analyzable {
    List<AnalyticsCollector> getAnalyticsCollectors();
}
