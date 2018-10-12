package com.synopsys.integration.blackduck.artifactory;

import com.synopsys.integration.blackduck.artifactory.analytics.AnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.analytics.Analyzable;

public class Module implements Analyzable {
    protected final AnalyticsCollector analyticsCollector;

    public Module(final AnalyticsCollector analyticsCollector) {
        this.analyticsCollector = analyticsCollector;
    }

    @Override
    public AnalyticsCollector getAnalyticsCollector() {
        return analyticsCollector;
    }
}
