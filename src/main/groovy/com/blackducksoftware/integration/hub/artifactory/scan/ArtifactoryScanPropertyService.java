package com.blackducksoftware.integration.hub.artifactory.scan;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.Repositories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryConfig;
import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryProperty;
import com.blackducksoftware.integration.hub.artifactory.BlackDuckHubProperty;
import com.blackducksoftware.integration.hub.artifactory.DateTimeManager;
import com.blackducksoftware.integration.hub.artifactory.HubConnectionService;

public class ArtifactoryScanPropertyService {
    private final Logger logger = LoggerFactory.getLogger(ArtifactoryScanPropertyService.class);

    private final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig;
    private final Repositories repositories;
    private final String propertiesFilePathOverride;
    private final String defaultPropertiesFileName;

    // TODO: Move the to BlackDuckArtifactoryConfig
    private String artifactCutoffDate;
    private String blackDuckScanCron;
    private String blackDuckAddPolicyStatusCron;
    private DateTimeManager dateTimeManager;
    private HubConnectionService hubConnectionService;

    public ArtifactoryScanPropertyService(final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig, final Repositories repositories, final String propertiesFilePathOverride, final String defaultPropertiesFileName) throws IOException {
        this.blackDuckArtifactoryConfig = blackDuckArtifactoryConfig;
        this.repositories = repositories;
        this.propertiesFilePathOverride = propertiesFilePathOverride;
        this.defaultPropertiesFileName = defaultPropertiesFileName;

        loadProperties();
    }

    private void loadProperties() throws IOException {
        final File propertiesFile;
        if (StringUtils.isNotBlank(propertiesFilePathOverride)) {
            propertiesFile = new File(propertiesFilePathOverride);
        } else {
            propertiesFile = new File(blackDuckArtifactoryConfig.getPluginsLibDirectory(), defaultPropertiesFileName);
        }

        try {
            blackDuckArtifactoryConfig.loadProperties(propertiesFile);
            artifactCutoffDate = blackDuckArtifactoryConfig.getProperty(ScanPluginProperty.CUTOFF_DATE);
            blackDuckScanCron = blackDuckArtifactoryConfig.getProperty(ScanPluginProperty.SCAN_CRON);
            blackDuckAddPolicyStatusCron = blackDuckArtifactoryConfig.getProperty(ScanPluginProperty.ADD_POLICY_STATUS_CRON);
            dateTimeManager = new DateTimeManager(blackDuckArtifactoryConfig.getProperty(ScanPluginProperty.DATE_TIME_PATTERN));
            hubConnectionService = new HubConnectionService(blackDuckArtifactoryConfig);
        } catch (final Exception e) {
            logger.error(String.format("Black Duck Scanner encountered an unexpected error when trying to load its properties file at %s", propertiesFile.getAbsolutePath()), e);
            throw (e);
        }
    }

    public void deleteAllBlackDuckProperties(final RepoPath repoPath) {
        repositories.deleteProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_TIME.getName());
        repositories.deleteProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_RESULT.getName());
        repositories.deleteProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_URL.getName());
        repositories.deleteProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_UI_URL.getName());
        repositories.deleteProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS.getName());
        repositories.deleteProperty(repoPath, BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS.getName());
    }

    /**
     * If the hub server being used changes, the existing properties in artifactory could be inaccurate so we will update them when they differ from the hub url established in the properties file.
     */
    public String updateUrlPropertyToCurrentHubServer(final String urlProperty) throws MalformedURLException {
        final String hubUrl = blackDuckArtifactoryConfig.getProperty(BlackDuckHubProperty.URL);
        if (urlProperty.startsWith(hubUrl)) {
            return urlProperty;
        }

        // Get the old hub url from the existing property
        final URL urlFromProperty = new URL(urlProperty);
        // TODO: Test with '/' at the end of hubUrl
        final URL updatedPropertyUrl = new URL(hubUrl + urlFromProperty.getPath());

        return updatedPropertyUrl.toString();
    }

    public String getArtifactCutoffDate() {
        return artifactCutoffDate;
    }

    public String getBlackDuckScanCron() {
        return blackDuckScanCron;
    }

    public String getBlackDuckAddPolicyStatusCron() {
        return blackDuckAddPolicyStatusCron;
    }

    public DateTimeManager getDateTimeManager() {
        return dateTimeManager;
    }

    public HubConnectionService getHubConnectionService() {
        return hubConnectionService;
    }
}
