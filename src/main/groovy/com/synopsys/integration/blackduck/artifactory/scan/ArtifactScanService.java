package com.synopsys.integration.blackduck.artifactory.scan;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.Repositories;
import org.artifactory.resource.ResourceStreamHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryConfig;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.BlackDuckConnectionService;
import com.synopsys.integration.blackduck.cli.summary.ScanServiceOutput;
import com.synopsys.integration.blackduck.configuration.HubScanConfig;
import com.synopsys.integration.blackduck.configuration.HubScanConfigBuilder;
import com.synopsys.integration.blackduck.service.model.ProjectNameVersionGuess;
import com.synopsys.integration.blackduck.service.model.ProjectNameVersionGuesser;
import com.synopsys.integration.blackduck.service.model.ProjectRequestBuilder;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.util.ResourceUtil;

public class ArtifactScanService {
    private final Logger logger = LoggerFactory.getLogger(ArtifactScanService.class);

    private final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig;
    private final RepositoryIdentificationService repositoryIdentificationService;
    private final ScanArtifactoryConfig scanArtifactoryConfig;
    private final BlackDuckConnectionService blackDuckConnectionService;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final Repositories repositories;

    public ArtifactScanService(final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig, final RepositoryIdentificationService repositoryIdentificationService,
    final ScanArtifactoryConfig scanArtifactoryConfig, final BlackDuckConnectionService blackDuckConnectionService, final ArtifactoryPropertyService artifactoryPropertyService, final Repositories repositories) {
        this.blackDuckArtifactoryConfig = blackDuckArtifactoryConfig;
        this.repositoryIdentificationService = repositoryIdentificationService;
        this.scanArtifactoryConfig = scanArtifactoryConfig;
        this.blackDuckConnectionService = blackDuckConnectionService;
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.repositories = repositories;
    }

    // TODO: Use try-with-resources statement instead (https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html)
    private FileLayoutInfo getArtifactFromPath(final RepoPath repoPath) {
        final FileLayoutInfo fileLayoutInfo = repositories.getLayoutInfo(repoPath);

        try (final ResourceStreamHandle resourceStream = repositories.getContent(repoPath)) {
            InputStream inputStream = null;
            FileOutputStream fileOutputStream = null;
            try {
                inputStream = resourceStream.getInputStream();
                fileOutputStream = new FileOutputStream(new File(blackDuckArtifactoryConfig.getBlackDuckDirectory(), repoPath.getName()));
                IOUtils.copy(inputStream, fileOutputStream);
            } catch (final IOException e) {
                logger.error(String.format("There was an error getting %s", repoPath.getName()), e);
            } finally {
                ResourceUtil.closeQuietly(inputStream);
                ResourceUtil.closeQuietly(fileOutputStream);
            }
        }

        return fileLayoutInfo;
    }

    private ProjectVersionView scanArtifact(final RepoPath repoPath, final String fileName, final FileLayoutInfo fileLayoutInfo) throws IntegrationException, InterruptedException, IOException {
        final ProjectRequestBuilder projectRequestBuilder = new ProjectRequestBuilder();
        final HubScanConfigBuilder hubScanConfigBuilder = new HubScanConfigBuilder();
        final int scanMemory = Integer.parseInt(blackDuckArtifactoryConfig.getProperty(ScanPluginProperty.MEMORY));
        final boolean dryRun = Boolean.parseBoolean(blackDuckArtifactoryConfig.getProperty(ScanPluginProperty.DRY_RUN));
        final boolean useRepoPathAsCodeLocationName = Boolean.parseBoolean(blackDuckArtifactoryConfig.getProperty(ScanPluginProperty.REPO_PATH_CODELOCATION));

        hubScanConfigBuilder.setScanMemory(scanMemory);
        hubScanConfigBuilder.setDryRun(dryRun);
        hubScanConfigBuilder.setToolsDir(scanArtifactoryConfig.getCliDirectory());
        hubScanConfigBuilder.setWorkingDirectory(blackDuckArtifactoryConfig.getBlackDuckDirectory());
        hubScanConfigBuilder.disableScanTargetPathExistenceCheck();

        String project = FileLayoutUtil.getProjectNameFromFileLayoutInfo(fileLayoutInfo);
        String version = FileLayoutUtil.getProjectVersionNameFromFileLayoutInfo(fileLayoutInfo);
        if (StringUtils.isBlank(project) || StringUtils.isBlank(version)) {
            final String filenameWithoutExtension = FilenameUtils.getBaseName(fileName);
            final ProjectNameVersionGuesser guesser = new ProjectNameVersionGuesser();
            final ProjectNameVersionGuess guess = guesser.guessNameAndVersion(filenameWithoutExtension);
            project = guess.getProjectName();
            version = guess.getVersionName();
        }
        final File scanFile = new File(blackDuckArtifactoryConfig.getBlackDuckDirectory(), fileName);
        final String scanTargetPath = scanFile.getCanonicalPath();
        projectRequestBuilder.setProjectName(project);
        projectRequestBuilder.setVersionName(version);
        hubScanConfigBuilder.addScanTargetPath(scanTargetPath);

        if (useRepoPathAsCodeLocationName) {
            String hostName;
            try {
                hostName = InetAddress.getLocalHost().getHostName();
            } catch (final UnknownHostException e) {
                hostName = "UNKNOWN_HOST";
            }
            hubScanConfigBuilder.addTargetToCodeLocationName(scanTargetPath, String.format("%s#%s", hostName, repoPath.toPath()));
        }

        final HubScanConfig hubScanConfig = hubScanConfigBuilder.build();
        logger.info(String.format("Performing scan on '%s'", scanTargetPath));
        final ScanServiceOutput scanServiceOutput = blackDuckConnectionService.performScan(hubScanConfig, projectRequestBuilder);

        blackDuckConnectionService.phoneHome();

        return scanServiceOutput.getProjectVersionWrapper().getProjectVersionView();
    }

    public void scanArtifactPaths(final Set<RepoPath> repoPaths) {
        logger.info(String.format("Found %d repoPaths to scan", repoPaths.size()));
        final List<RepoPath> shouldScanRepoPaths = new ArrayList<>();
        for (final RepoPath repoPath : repoPaths) {
            logger.debug(String.format("Verifying if repoPath should be scanned: %s", repoPath));
            if (repositoryIdentificationService.shouldRepoPathBeScannedNow(repoPath)) {
                logger.info(String.format("Adding repoPath to scan list: %s", repoPath));
                shouldScanRepoPaths.add(repoPath);
            }
        }

        for (final RepoPath repoPath : shouldScanRepoPaths) {
            try {
                final String timeString = scanArtifactoryConfig.getDateTimeManager().getStringFromDate(new Date());
                artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_TIME, timeString);
                final FileLayoutInfo fileLayoutInfo = getArtifactFromPath(repoPath);
                final ProjectVersionView projectVersionView = scanArtifact(repoPath, repoPath.getName(), fileLayoutInfo);
                writeScanProperties(repoPath, projectVersionView);
            } catch (final Exception e) {
                logger.error(String.format("Please investigate the scan logs for details - the Black Duck Scan did not complete successfully on %s", repoPath.getName()), e);
                artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_RESULT, "FAILURE");
            } finally {
                deletePathArtifact(repoPath.getName());
            }
        }
    }

    private void writeScanProperties(final RepoPath repoPath, final ProjectVersionView projectVersionView) {
        logger.info(String.format("%s was successfully scanned by the BlackDuck CLI.", repoPath.getName()));
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_RESULT, "SUCCESS");

        if (projectVersionView != null) {
            try {
                final String projectVersionUrl = blackDuckConnectionService.getProjectVersionUrlFromView(projectVersionView);
                if (StringUtils.isNotEmpty(projectVersionUrl)) {
                    artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_URL, projectVersionUrl);
                    logger.info(String.format("Added %s to %s", projectVersionUrl, repoPath.getName()));
                }
                final String projectVersionUIUrl = blackDuckConnectionService.getProjectVersionUIUrlFromView(projectVersionView);
                if (StringUtils.isNotEmpty(projectVersionUIUrl)) {
                    artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_UI_URL, projectVersionUIUrl);
                    logger.info(String.format("Added %s to %s", projectVersionUIUrl, repoPath.getName()));
                }
            } catch (final Exception e) {
                logger.error("Exception getting code location url", e);
            }
        } else {
            logger.warn("No scan summaries were available for a successful scan. This is expected if this was a dry run, but otherwise there should be summaries.");
        }
    }

    private void deletePathArtifact(final String fileName) {
        try {
            final boolean deleteOk = new File(blackDuckArtifactoryConfig.getBlackDuckDirectory(), fileName).delete();
            logger.info(String.format("Successfully deleted temporary %s: %s", fileName, Boolean.toString(deleteOk)));
        } catch (final Exception e) {
            logger.error(String.format("Exception deleting %s", fileName), e);
        }
    }
}
