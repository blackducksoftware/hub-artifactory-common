package com.synopsys.integration.blackduck.artifactory;

public enum PluginType {
    SCANNER("blackDuckScan"),
    INSPECTOR("blackDuckCacheInspector"),
    POLICY_ENFORCER("blackDuckPolicyEnforcer");

    private final String name;

    PluginType(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
