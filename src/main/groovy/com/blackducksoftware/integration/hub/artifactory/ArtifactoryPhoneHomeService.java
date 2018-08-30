package com.blackducksoftware.integration.hub.artifactory;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;

public class ArtifactoryPhoneHomeService {
    private final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig;
    private final HubConnectionService hubConnectionService;

    public ArtifactoryPhoneHomeService(final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig, final HubConnectionService hubConnectionService) {
        this.blackDuckArtifactoryConfig = blackDuckArtifactoryConfig;
        this.hubConnectionService = hubConnectionService;
    }

    public void phoneHome() {
        try {
            String pluginVersion = null;
            final File versionFile = blackDuckArtifactoryConfig.getVersionFile();
            if (versionFile != null) {
                pluginVersion = FileUtils.readFileToString(versionFile, StandardCharsets.UTF_8);
            }

            hubConnectionService.phoneHome(pluginVersion, blackDuckArtifactoryConfig.getThirdPartyVersion(), blackDuckArtifactoryConfig.getPluginName());
        } catch (final Exception ignored) {
        }
    }
}
