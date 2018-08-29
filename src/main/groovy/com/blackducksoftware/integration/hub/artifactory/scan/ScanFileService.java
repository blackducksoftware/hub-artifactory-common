package com.blackducksoftware.integration.hub.artifactory.scan;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.Repositories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.api.generated.view.ProjectVersionView;
import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryConfig;
import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryProperty;
import com.blackducksoftware.integration.hub.artifactory.HubConnectionService;

// TODO: Remove if all methods have moved. If not then rename this
public class ScanFileService {
    private final Logger logger = LoggerFactory.getLogger(ScanFileService.class);

    private final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig;
    private final HubConnectionService hubConnectionService;
    private final Repositories repositories;

    private File cliDirectory;

    public ScanFileService(final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig, final HubConnectionService hubConnectionService, final Repositories repositories) {
        this.blackDuckArtifactoryConfig = blackDuckArtifactoryConfig;
        this.hubConnectionService = hubConnectionService;
        this.repositories = repositories;
    }

    // TODO: Move to ArtifcctScanService
    public void deletePathArtifact(final String fileName) {
        try {
            final boolean deleteOk = new File(blackDuckArtifactoryConfig.getBlackDuckDirectory(), fileName).delete();
            logger.info(String.format("Successfully deleted temporary %s: %s", fileName, Boolean.toString(deleteOk)));
        } catch (final Exception e) {
            logger.error(String.format("Exception deleting %s", fileName), e);
        }
    }

    // TODO: Move to a ScanPluginInitializer?
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

    // TODO: Move to a new ScanPropertyManager?
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

    public File getCliDirectory() { return cliDirectory; }
}
