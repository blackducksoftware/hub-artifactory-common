package com.synopsys.integration.blackduck.artifactory.scan;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.BlackDuckPropertyManager;
import com.synopsys.integration.exception.IntegrationException;

public class ScanModuleConfig {
    private final Logger logger = LoggerFactory.getLogger(ScanModuleConfig.class);

    private final String artifactCutoffDate;
    private final String blackDuckScanCron;
    private final String blackDuckAddPolicyStatusCron;

    private File cliDirectory;

    public ScanModuleConfig(final BlackDuckPropertyManager blackDuckPropertyManager) {
        this.artifactCutoffDate = blackDuckPropertyManager.getProperty(ScanModuleProperty.CUTOFF_DATE);
        this.blackDuckScanCron = blackDuckPropertyManager.getProperty(ScanModuleProperty.SCAN_CRON);
        this.blackDuckAddPolicyStatusCron = blackDuckPropertyManager.getProperty(ScanModuleProperty.ADD_POLICY_STATUS_CRON);
    }

    public void setUpCliDuckDirectory(final File blackDuckDirectory) {
        try {
            final File cliDirectory = new File(blackDuckDirectory, "cli");
            if (!cliDirectory.exists() && !cliDirectory.mkdir()) {
                throw new IntegrationException(String.format("Failed to create cliDirectory: %s", cliDirectory.getCanonicalPath()));
            }
            this.cliDirectory = cliDirectory;
        } catch (final IOException | IntegrationException e) {
            logger.error(String.format("Exception while setting up the Black Duck directory %s", cliDirectory), e);
        }
    }

    public File getCliDirectory() { return cliDirectory; }

    public String getArtifactCutoffDate() { return artifactCutoffDate; }

    public String getBlackDuckScanCron() { return blackDuckScanCron; }

    public String getBlackDuckAddPolicyStatusCron() { return blackDuckAddPolicyStatusCron; }
}
