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
package com.blackducksoftware.integration.hub.artifactory;

public enum BlackDuckArtifactoryProperty {
    HUB_ORIGIN_ID("hubOriginId"),
    HUB_FORGE("hubForge"),
    PROJECT_NAME("hubProjectName"),
    HUB_PROJECT_VERSION_NAME("hubProjectVersionName"),
    HIGH_VULNERABILITIES("highVulnerabilities"),
    MEDIUM_VULNERABILITIES("mediumVulnerabilities"),
    LOW_VULNERABILITIES("lowVulnerabilities"),
    POLICY_STATUS("policyStatus"),
    COMPONENT_VERSION_URL("componentVersionUrl"),
    PROJECT_VERSION_UI_URL("uiUrl"),
    OVERALL_POLICY_STATUS("overallPolicyStatus"),
    LAST_INSPECTION("lastInspection"),
    INSPECTION_STATUS("inspectionStatus"),
    LAST_UPDATE("lastUpdate"),
    UPDATE_STATUS("updateStatus"),
    SCAN_TIME("scanTime"),
    SCAN_RESULT("scanResult"),
    PROJECT_VERSION_URL("apiUrl");

    private final String name;

    private BlackDuckArtifactoryProperty(final String name) {
        this.name = "blackduck." + name;
    }

    public String getName() {
        return name;
    }

}
