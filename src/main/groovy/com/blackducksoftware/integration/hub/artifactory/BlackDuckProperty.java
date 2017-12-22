package com.blackducksoftware.integration.hub.artifactory;

public enum BlackDuckProperty {
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
    INSPECTION_TIME("inspectionTime"),
    INSPECTION_STATUS("inspectionStatus"),
    SCAN_TIME("scanTime"),
    SCAN_RESULT("scanResult"),
    PROJECT_VERSION_URL("apiUrl");

    private final String name;

    private BlackDuckProperty(final String name) {
        this.name = "blackduck." + name;
    }

    public String getName() {
        return name;
    }

}
