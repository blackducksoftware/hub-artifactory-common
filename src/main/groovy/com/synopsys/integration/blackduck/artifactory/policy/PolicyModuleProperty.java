package com.synopsys.integration.blackduck.artifactory.policy;

import com.synopsys.integration.blackduck.artifactory.ConfigurationProperty;

public enum PolicyModuleProperty implements ConfigurationProperty {
    METADATA_BLOCK("metadata.block");

    private final String key;

    PolicyModuleProperty(final String key) {
        this.key = "blackduck.artifactory.policy." + key;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getOldKey() {
        return null;
    }
}
