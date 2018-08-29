package com.blackducksoftware.integration.hub.artifactory.scan;

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

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.api.generated.view.ProjectVersionView;
import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryConfig;
import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryProperty;
import com.blackducksoftware.integration.hub.artifactory.DateTimeManager;
import com.blackducksoftware.integration.hub.artifactory.HubConnectionService;
import com.blackducksoftware.integration.hub.cli.summary.ScanServiceOutput;
import com.blackducksoftware.integration.hub.configuration.HubScanConfig;
import com.blackducksoftware.integration.hub.configuration.HubScanConfigBuilder;
import com.blackducksoftware.integration.hub.service.model.ProjectNameVersionGuess;
import com.blackducksoftware.integration.hub.service.model.ProjectNameVersionGuesser;
import com.blackducksoftware.integration.hub.service.model.ProjectRequestBuilder;
import com.blackducksoftware.integration.util.ResourceUtil;

public class ArtifactScanService {
    private final Logger logger = LoggerFactory.getLogger(ArtifactScanService.class);

    private final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig;
    private final RepositoryIdentifactionService repositoryIdentifactionService;
    private final ScanFileService scanFileService;
    private final ScanPhoneHomeService scanPhoneHomeService;
    private final Repositories repositories;
    private final DateTimeManager dateTimeManager;

    public ArtifactScanService(final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig, final RepositoryIdentifactionService repositoryIdentifactionService,
    final ScanFileService scanFileService, final ScanPhoneHomeService scanPhoneHomeService, final Repositories repositories, final DateTimeManager dateTimeManager) {
        this.blackDuckArtifactoryConfig = blackDuckArtifactoryConfig;
        this.repositoryIdentifactionService = repositoryIdentifactionService;
        this.scanFileService = scanFileService;
        this.scanPhoneHomeService = scanPhoneHomeService;
        this.repositories = repositories;
        this.dateTimeManager = dateTimeManager;
    }

    private FileLayoutInfo getArtifactFromPath(final RepoPath repoPath) {
        final ResourceStreamHandle resourceStream = repositories.getContent(repoPath);
        final FileLayoutInfo fileLayoutInfo = repositories.getLayoutInfo(repoPath);
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            inputStream = resourceStream.getInputStream();
            fileOutputStream = new FileOutputStream(new File(blackDuckArtifactoryConfig.getBlackDuckDirectory(), repoPath.getName()));
            IOUtils.copy(inputStream, fileOutputStream);
        } catch (final Exception e) {
            logger.error(String.format("There was an error getting %s", repoPath.getName()), e);
        } finally {
            ResourceUtil.closeQuietly(inputStream);
            ResourceUtil.closeQuietly(fileOutputStream);
            resourceStream.close();
        }
        return fileLayoutInfo;
    }

    private ProjectVersionView scanArtifact(final RepoPath repoPath, final String fileName, final FileLayoutInfo fileLayoutInfo) throws IntegrationException, InterruptedException, IOException {
        final ProjectRequestBuilder projectRequestBuilder = new ProjectRequestBuilder();
        final HubScanConfigBuilder hubScanConfigBuilder = new HubScanConfigBuilder();
        final HubConnectionService hubConnectionService = new HubConnectionService(blackDuckArtifactoryConfig);
        final int scanMemory = Integer.parseInt(blackDuckArtifactoryConfig.getProperty(ScanPluginProperty.MEMORY));
        final boolean dryRun = Boolean.parseBoolean(blackDuckArtifactoryConfig.getProperty(ScanPluginProperty.DRY_RUN));
        final boolean useRepoPathAsCodeLocationName = Boolean.parseBoolean(blackDuckArtifactoryConfig.getProperty(ScanPluginProperty.REPO_PATH_CODELOCATION));

        hubScanConfigBuilder.setScanMemory(scanMemory);
        hubScanConfigBuilder.setDryRun(dryRun);
        hubScanConfigBuilder.setToolsDir(scanFileService.getCliDirectory());
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
        logger.warn(String.format("Performing scan on '%s'", scanTargetPath));
        final ScanServiceOutput scanServiceOutput = hubConnectionService.performScan(hubScanConfig, projectRequestBuilder);

        scanPhoneHomeService.phoneHome();

        return scanServiceOutput.getProjectVersionWrapper().getProjectVersionView();
    }

    public void scanArtifactPaths(final Set<RepoPath> repoPaths) {
        logger.warn(String.format("Found %d repoPaths to scan", repoPaths.size()));
        final List<RepoPath> shouldScanRepoPaths = new ArrayList<>();
        for (final RepoPath repoPath : repoPaths) {
            logger.warn(String.format("Verifying if repoPath should be scanned: %s", repoPath));
            if (repositoryIdentifactionService.shouldRepoPathBeScannedNow(repoPath)) {
                logger.warn(String.format("Adding repoPath to scan list: %s", repoPath));
                shouldScanRepoPaths.add(repoPath);
            }
        }

        for (final RepoPath repoPath : shouldScanRepoPaths) {
            try {
                final String timeString = dateTimeManager.getStringFromDate(new Date());
                repositories.setProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_TIME.getName(), timeString);
                final FileLayoutInfo fileLayoutInfo = getArtifactFromPath(repoPath);
                final ProjectVersionView projectVersionView = scanArtifact(repoPath, repoPath.getName(), fileLayoutInfo);
                scanFileService.writeScanProperties(repoPath, projectVersionView);
            } catch (final Exception e) {
                logger.error(String.format("Please investigate the scan logs for details - the Black Duck Scan did not complete successfully on %s", repoPath.getName()), e);
                repositories.setProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_RESULT.getName(), "FAILURE");
            } finally {
                scanFileService.deletePathArtifact(repoPath.getName());
            }
        }
    }
}
