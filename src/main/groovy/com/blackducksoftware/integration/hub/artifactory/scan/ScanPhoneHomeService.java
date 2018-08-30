package com.blackducksoftware.integration.hub.artifactory.scan;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;

import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryConfig;
import com.blackducksoftware.integration.hub.artifactory.HubConnectionService;

public class ScanPhoneHomeService {
    private final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig;
    private final HubConnectionService hubConnectionService;

    public ScanPhoneHomeService(final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig, final HubConnectionService hubConnectionService) {
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

            hubConnectionService.phoneHome(pluginVersion, blackDuckArtifactoryConfig.getThirdPartyVersion(), "blackDuckScanForHub");
        } catch (final Exception e) {
        }
    }
}
