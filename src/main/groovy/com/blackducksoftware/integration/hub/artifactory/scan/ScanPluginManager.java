package com.blackducksoftware.integration.hub.artifactory.scan;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryConfig;
import com.blackducksoftware.integration.hub.artifactory.DateTimeManager;

public class ScanPluginManager {
    private final Logger logger = LoggerFactory.getLogger(ScanPluginManager.class);

    private final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig;

    private final String artifactCutoffDate;
    private final String blackDuckScanCron;
    private final String blackDuckAddPolicyStatusCron;
    private final DateTimeManager dateTimeManager;

    private File cliDirectory;

    public ScanPluginManager(final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig) {
        this.blackDuckArtifactoryConfig = blackDuckArtifactoryConfig;

        artifactCutoffDate = blackDuckArtifactoryConfig.getProperty(ScanPluginProperty.CUTOFF_DATE);
        blackDuckScanCron = blackDuckArtifactoryConfig.getProperty(ScanPluginProperty.SCAN_CRON);
        blackDuckAddPolicyStatusCron = blackDuckArtifactoryConfig.getProperty(ScanPluginProperty.ADD_POLICY_STATUS_CRON);
        dateTimeManager = new DateTimeManager(blackDuckArtifactoryConfig.getProperty(ScanPluginProperty.DATE_TIME_PATTERN));
    }

    public void setUpBlackDuckDirectory() {
        File cliDirectory = null;
        try {
            final String scanBinariesDirectory = blackDuckArtifactoryConfig.getProperty(ScanPluginProperty.BINARIES_DIRECTORY_PATH);
            if (StringUtils.isNotEmpty(scanBinariesDirectory)) {
                blackDuckArtifactoryConfig.setBlackDuckDirectory(FilenameUtils.concat(blackDuckArtifactoryConfig.getHomeDirectory().getCanonicalPath(), scanBinariesDirectory));
            } else {
                blackDuckArtifactoryConfig.setBlackDuckDirectory(FilenameUtils.concat(blackDuckArtifactoryConfig.getEtcDirectory().getCanonicalPath(), "blackducksoftware"));
            }
            cliDirectory = new File(blackDuckArtifactoryConfig.getBlackDuckDirectory(), "cli");
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

    public DateTimeManager getDateTimeManager() { return dateTimeManager; }
}
