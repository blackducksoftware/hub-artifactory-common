package com.blackducksoftware.integration.hub.artifactory.scan;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.Repositories;
import org.artifactory.search.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.hub.api.generated.view.ProjectVersionView;
import com.blackducksoftware.integration.hub.artifactory.ArtifactoryPropertyService;
import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryConfig;
import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryProperty;
import com.blackducksoftware.integration.hub.artifactory.BlackDuckHubProperty;

public class ArtifactoryScanPropertyService extends ArtifactoryPropertyService {
    private final Logger logger = LoggerFactory.getLogger(ArtifactoryScanPropertyService.class);

    private final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig;
    private final ScanPluginManager scanPluginManager;

    private final String propertiesFilePathOverride;

    public ArtifactoryScanPropertyService(final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig, final ScanPluginManager scanPluginManager, final Repositories repositories, final Searches searches,
    final String propertiesFilePathOverride) throws IOException {
        super(repositories, searches, scanPluginManager.getDateTimeManager());
        this.blackDuckArtifactoryConfig = blackDuckArtifactoryConfig;
        this.scanPluginManager = scanPluginManager;
        this.propertiesFilePathOverride = propertiesFilePathOverride;

        loadProperties();
    }

    private void loadProperties() throws IOException {
        final File propertiesFile;
        if (StringUtils.isNotBlank(propertiesFilePathOverride)) {
            propertiesFile = new File(propertiesFilePathOverride);
        } else {
            propertiesFile = new File(blackDuckArtifactoryConfig.getPluginsLibDirectory(), blackDuckArtifactoryConfig.getDefaultPropertiesFileName());
        }

        try {
            blackDuckArtifactoryConfig.loadProperties(propertiesFile);
        } catch (final Exception e) {
            logger.error(String.format("Black Duck Scanner encountered an unexpected error when trying to load its properties file at %s", propertiesFile.getAbsolutePath()), e);
            throw (e);
        }
    }

    void writeScanProperties(final RepoPath repoPath, final ProjectVersionView projectVersionView) {
        logger.info(String.format("%s was successfully scanned by the BlackDuck CLI.", repoPath.getName()));
        setProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_RESULT, "SUCCESS");

        if (projectVersionView != null) {
            try {
                final String projectVersionUrl = scanPluginManager.getHubConnectionService().getProjectVersionUrlFromView(projectVersionView);
                if (StringUtils.isNotEmpty(projectVersionUrl)) {
                    setProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_URL, projectVersionUrl);
                    logger.info(String.format("Added %s to %s", projectVersionUrl, repoPath.getName()));
                }
                final String projectVersionUIUrl = scanPluginManager.getHubConnectionService().getProjectVersionUIUrlFromView(projectVersionView);
                if (StringUtils.isNotEmpty(projectVersionUIUrl)) {
                    setProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_UI_URL, projectVersionUIUrl);
                    logger.info(String.format("Added %s to %s", projectVersionUIUrl, repoPath.getName()));
                }
            } catch (final Exception e) {
                logger.error("Exception getting code location url", e);
            }
        } else {
            logger.warn("No scan summaries were available for a successful scan. This is expected if this was a dry run, but otherwise there should be summaries.");
        }
    }

    public void deleteAllBlackDuckProperties(final RepoPath repoPath) {
        deleteAllBlackDuckPropertiesFrom(repoPath.getRepoKey());
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
}
