package com.synopsys.integration.blackduck.artifactory.inspect;

import java.util.List;

import com.synopsys.integration.blackduck.artifactory.BlackDuckPropertyManager;

public class InspectionModuleConfig {
    private final String blackDuckIdentifyArtifactsCron;
    private final String blackDuckPopulateMetadataCron;
    private final String blackDuckUpdateMetadataCron;

    private final List<String> repoKeys;

    public InspectionModuleConfig(final BlackDuckPropertyManager blackDuckPropertyManager, final List<String> repoKeys) {
        this.blackDuckIdentifyArtifactsCron = blackDuckPropertyManager.getProperty(InspectModuleProperty.IDENTIFY_ARTIFACTS_CRON);
        this.blackDuckPopulateMetadataCron = blackDuckPropertyManager.getProperty(InspectModuleProperty.POPULATE_METADATA_CRON);
        this.blackDuckUpdateMetadataCron = blackDuckPropertyManager.getProperty(InspectModuleProperty.UPDATE_METADATA_CRON);
        this.repoKeys = repoKeys;
    }

    public String getBlackDuckIdentifyArtifactsCron() {
        return blackDuckIdentifyArtifactsCron;
    }

    public String getBlackDuckPopulateMetadataCron() {
        return blackDuckPopulateMetadataCron;
    }

    public String getBlackDuckUpdateMetadataCron() {
        return blackDuckUpdateMetadataCron;
    }

    public List<String> getRepoKeys() {
        return repoKeys;
    }
}
