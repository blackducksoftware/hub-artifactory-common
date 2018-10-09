package com.synopsys.integration.blackduck.artifactory.policy;

import org.apache.commons.lang3.BooleanUtils;

import com.synopsys.integration.blackduck.artifactory.BlackDuckPropertyManager;

public class PolicyModuleConfig {
    private final BlackDuckPropertyManager blackDuckPropertyManager;

    private final boolean metadataBlockEnabled;

    public PolicyModuleConfig(final BlackDuckPropertyManager blackDuckPropertyManager) {
        this.blackDuckPropertyManager = blackDuckPropertyManager;
        metadataBlockEnabled = BooleanUtils.toBoolean(blackDuckPropertyManager.getProperty(PolicyModuleProperty.METADATA_BLOCK));
    }

    public boolean isMetadataBlockEnabled() {
        return metadataBlockEnabled;
    }
}
