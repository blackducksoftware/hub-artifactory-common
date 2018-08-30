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

import com.blackducksoftware.integration.hub.api.generated.view.ProjectVersionView;
import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryConfig;
import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryProperty;
import com.blackducksoftware.integration.hub.artifactory.BlackDuckHubProperty;
import com.blackducksoftware.integration.hub.artifactory.HubConnectionService;

public class ArtifactoryScanPropertyService {
    private final Logger logger = LoggerFactory.getLogger(ArtifactoryScanPropertyService.class);

    private final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig;
    private final HubConnectionService hubConnectionService;
    private final Repositories repositories;
    private final String propertiesFilePathOverride;
    private final String defaultPropertiesFileName;

    public ArtifactoryScanPropertyService(final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig, final HubConnectionService hubConnectionService, final Repositories repositories,
    final String propertiesFilePathOverride, final String defaultPropertiesFileName) throws IOException {
        this.blackDuckArtifactoryConfig = blackDuckArtifactoryConfig;
        this.hubConnectionService = hubConnectionService;
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
        } catch (final Exception e) {
            logger.error(String.format("Black Duck Scanner encountered an unexpected error when trying to load its properties file at %s", propertiesFile.getAbsolutePath()), e);
            throw (e);
        }
    }

    // TODO: Use ArtifactoryPropertyService for managing properties instead of Repositories
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

    // TODO: Use ArtifactoryPropertyService for managing properties instead of Repositories
    public void writeScanProperties(final RepoPath repoPath, final ProjectVersionView projectVersionView) {
        logger.info("${repoPath.name} was successfully scanned by the BlackDuck CLI.");
        repositories.setProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_RESULT.getName(), "SUCCESS");

        if (projectVersionView != null) {
            try {
                final String projectVersionUrl = hubConnectionService.getProjectVersionUrlFromView(projectVersionView);
                if (StringUtils.isNotEmpty(projectVersionUrl)) {
                    repositories.setProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_URL.getName(), projectVersionUrl);
                    logger.info("Added ${projectVersionUrl} to ${repoPath.name}");
                }
                final String projectVersionUIUrl = hubConnectionService.getProjectVersionUIUrlFromView(projectVersionView);
                if (StringUtils.isNotEmpty(projectVersionUIUrl)) {
                    repositories.setProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_UI_URL.getName(), projectVersionUIUrl);
                    logger.info(String.format("Added %s} to %s", projectVersionUIUrl, repoPath.getName()));
                }
            } catch (final Exception e) {
                logger.error("Exception getting code location url", e);
            }
        } else {
            logger.warn("No scan summaries were available for a successful scan. This is expected if this was a dry run, but otherwise there should be summaries.");
        }
    }
}
