/**
 * hub-artifactory-common
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.artifactory.analytics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.synopsys.integration.blackduck.artifactory.Module;
import com.synopsys.integration.blackduck.artifactory.ModuleConfig;

public class AnalyticsModule implements Analyzable, Module {
    public final static String SUBMIT_ANALYTICS_CRON = "0 0 0 ? * * *"; // Every day at 12 am

    private final AnalyticsModuleConfig analyticsModuleConfig;
    private final AnalyticsService analyticsService;
    private final SimpleAnalyticsCollector simpleAnalyticsCollector;
    private List<ModuleConfig> moduleConfigs = new ArrayList<>();

    public AnalyticsModule(final AnalyticsModuleConfig analyticsModuleConfig, final AnalyticsService analyticsService, final SimpleAnalyticsCollector simpleAnalyticsCollector) {
        this.analyticsModuleConfig = analyticsModuleConfig;
        this.analyticsService = analyticsService;
        this.simpleAnalyticsCollector = simpleAnalyticsCollector;
    }

    @Override
    public List<AnalyticsCollector> getAnalyticsCollectors() {
        return Arrays.asList(simpleAnalyticsCollector);
    }

    @Override
    public AnalyticsModuleConfig getModuleConfig() {
        return analyticsModuleConfig;
    }

    public void setModuleConfigs(final List<ModuleConfig> moduleConfigs) {
        this.moduleConfigs = moduleConfigs;
    }

    /**
     * Submits a payload to phone home with data from all the collectors ({@link FeatureAnalyticsCollector})
     * This should be used infrequently such as once a day due to quota
     */
    public void submitAnalytics() {
        moduleConfigs.forEach(this::updateModuleStatus);
        analyticsService.submitAnalytics();
    }

    private void updateModuleStatus(final ModuleConfig moduleConfig) {
        final String key = String.format("modules.%s.enabled");
        simpleAnalyticsCollector.putMetadata(key, moduleConfig.isEnabled());
    }

}
