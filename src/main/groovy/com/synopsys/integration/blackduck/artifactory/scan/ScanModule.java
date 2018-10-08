package com.synopsys.integration.blackduck.artifactory.scan;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.artifactory.repo.RepoPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.BlackDuckConnectionService;
import com.synopsys.integration.blackduck.artifactory.ModuleType;
import com.synopsys.integration.blackduck.artifactory.TriggerType;
import com.synopsys.integration.blackduck.artifactory.inspect.UpdateStatus;
import com.synopsys.integration.blackduck.summary.Result;

public class ScanModule {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ScanModuleConfig scanModuleConfig;

    private final RepositoryIdentificationService repositoryIdentificationService;
    private final ArtifactScanService artifactScanService;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final BlackDuckConnectionService blackDuckConnectionService;

    public ScanModule(final ScanModuleConfig scanModuleConfig, final RepositoryIdentificationService repositoryIdentificationService, final ArtifactScanService artifactScanService,
        final ArtifactoryPropertyService artifactoryPropertyService, final BlackDuckConnectionService blackDuckConnectionService) {
        this.scanModuleConfig = scanModuleConfig;
        this.repositoryIdentificationService = repositoryIdentificationService;
        this.artifactScanService = artifactScanService;
        this.artifactoryPropertyService = artifactoryPropertyService;

        this.blackDuckConnectionService = blackDuckConnectionService;
    }

    public ScanModuleConfig getScanModuleConfig() {
        return scanModuleConfig;
    }

    public void triggerScan(final TriggerType triggerType) throws IOException {
        logStart("blackDuckScan", triggerType);

        final Set<RepoPath> repoPaths = repositoryIdentificationService.searchForRepoPaths();
        artifactScanService.scanArtifactPaths(repoPaths);

        logEnd("blackDuckScan", triggerType);
    }

    public void addPolicyStatus(final TriggerType triggerType) throws IOException {
        logStart("blackDuckAddPolicyStatus", triggerType);

        final Set<RepoPath> repoPaths = repositoryIdentificationService.searchForRepoPaths();
        blackDuckConnectionService.populatePolicyStatuses(repoPaths, ModuleType.SCANNER);

        logEnd("blackDuckAddPolicyStatus", triggerType);
    }

    public void deleteScanProperties(final TriggerType triggerType) {
        logStart("blackDuckDeleteScanProperties", triggerType);

        repositoryIdentificationService.getRepoKeysToScan()
            .forEach(artifactoryPropertyService::deleteAllBlackDuckPropertiesFromRepo);

        logEnd("blackDuckDeleteScanProperties", triggerType);
    }

    public void deleteScanPropertiesFromFailures(final TriggerType triggerType) {
        logStart("blackDuckDeleteScanPropertiesFromFailures", triggerType);

        repositoryIdentificationService.getRepoKeysToScan().stream()
            .map(repoKey -> artifactoryPropertyService.getAllItemsInRepoWithProperties(repoKey, BlackDuckArtifactoryProperty.SCAN_RESULT))
            .forEach(repoPaths -> repoPaths.stream()
                                      .filter(repoPath -> artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_RESULT).equals(Optional.of(Result.FAILURE.toString())))
                                      .forEach(artifactoryPropertyService::deleteAllBlackDuckPropertiesFromRepoPath)
            );

        logEnd("blackDuckDeleteScanPropertiesFromFailures", triggerType);
    }

    public void deleteScanPropertiesFromOutOfDate(final TriggerType triggerType) {
        logStart("blackDuckDeleteScanPropertiesFromOutOfDate", triggerType);

        repositoryIdentificationService.getRepoKeysToScan().stream()
            .map(repoKey -> artifactoryPropertyService.getAllItemsInRepoWithProperties(repoKey, BlackDuckArtifactoryProperty.UPDATE_STATUS))
            .forEach(repoPaths -> repoPaths.stream()
                                      .filter(repoPath -> artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.UPDATE_STATUS).equals(Optional.of(UpdateStatus.OUT_OF_DATE.toString())))
                                      .forEach(artifactoryPropertyService::deleteAllBlackDuckPropertiesFromRepoPath)
            );

        logEnd("blackDuckDeleteScanPropertiesFromOutOfDate", triggerType);
    }

    public void updateDeprecatedProperties(final TriggerType triggerType) {
        logStart("blackDuckUpdateDeprecatedProperties", triggerType);

        repositoryIdentificationService.getRepoKeysToScan()
            .forEach(artifactoryPropertyService::updateAllBlackDuckPropertiesFrom);

        logEnd("blackDuckUpdateDeprecatedProperties", triggerType);
    }

    private void logStart(final String functionName, final TriggerType triggerType) {
        logger.info(String.format("Starting %s %s...", functionName, triggerType.getLogName()));
    }

    private void logEnd(final String functionName, final TriggerType triggerType) {
        logger.info(String.format("...completed %s %s.", functionName, triggerType.getLogName()));
    }
}
