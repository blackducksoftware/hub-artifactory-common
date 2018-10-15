package com.synopsys.integration.blackduck.artifactory.scan;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.BlackDuckConnectionService;
import com.synopsys.integration.blackduck.artifactory.LogUtil;
import com.synopsys.integration.blackduck.artifactory.TriggerType;
import com.synopsys.integration.blackduck.artifactory.analytics.AnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.analytics.Analyzable;
import com.synopsys.integration.blackduck.artifactory.analytics.FunctionAnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.inspect.UpdateStatus;
import com.synopsys.integration.blackduck.summary.Result;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class ScanModule implements Analyzable {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final ScanModuleConfig scanModuleConfig;

    private final RepositoryIdentificationService repositoryIdentificationService;
    private final ArtifactScanService artifactScanService;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final BlackDuckConnectionService blackDuckConnectionService;
    private final StatusCheckService statusCheckService;
    private final FunctionAnalyticsCollector functionAnalyticsCollector;

    public ScanModule(final ScanModuleConfig scanModuleConfig, final RepositoryIdentificationService repositoryIdentificationService, final ArtifactScanService artifactScanService,
        final ArtifactoryPropertyService artifactoryPropertyService, final BlackDuckConnectionService blackDuckConnectionService, final StatusCheckService statusCheckService, final FunctionAnalyticsCollector functionAnalyticsCollector) {
        this.scanModuleConfig = scanModuleConfig;
        this.repositoryIdentificationService = repositoryIdentificationService;
        this.artifactScanService = artifactScanService;
        this.artifactoryPropertyService = artifactoryPropertyService;

        this.blackDuckConnectionService = blackDuckConnectionService;
        this.statusCheckService = statusCheckService;
        this.functionAnalyticsCollector = functionAnalyticsCollector;
    }

    public ScanModuleConfig getScanModuleConfig() {
        return scanModuleConfig;
    }

    public void triggerScan(final TriggerType triggerType) throws IOException {
        LogUtil.start(logger, "blackDuckScan", triggerType);

        final Set<RepoPath> repoPaths = repositoryIdentificationService.searchForRepoPaths();
        artifactScanService.scanArtifactPaths(repoPaths);

        functionAnalyticsCollector.logFunction("blackDuckScan", triggerType);
        LogUtil.finish(logger, "blackDuckScan", triggerType);
    }

    public void addPolicyStatus(final TriggerType triggerType) throws IOException {
        LogUtil.start(logger, "blackDuckAddPolicyStatus", triggerType);

        final Set<RepoPath> repoPaths = repositoryIdentificationService.searchForRepoPaths();
        blackDuckConnectionService.populatePolicyStatuses(repoPaths);

        functionAnalyticsCollector.logFunction("blackDuckAddPolicyStatus", triggerType);
        LogUtil.finish(logger, "blackDuckAddPolicyStatus", triggerType);
    }

    public void deleteScanProperties(final TriggerType triggerType) {
        LogUtil.start(logger, "blackDuckDeleteScanProperties", triggerType);

        repositoryIdentificationService.getRepoKeysToScan()
            .forEach(artifactoryPropertyService::deleteAllBlackDuckPropertiesFromRepo);

        functionAnalyticsCollector.logFunction("blackDuckDeleteScanProperties", triggerType);
        LogUtil.finish(logger, "blackDuckDeleteScanProperties", triggerType);
    }

    public void deleteScanPropertiesFromFailures(final TriggerType triggerType) {
        LogUtil.start(logger, "blackDuckDeleteScanPropertiesFromFailures", triggerType);

        repositoryIdentificationService.getRepoKeysToScan().stream()
            .map(repoKey -> artifactoryPropertyService.getAllItemsInRepoWithProperties(repoKey, BlackDuckArtifactoryProperty.SCAN_RESULT))
            .forEach(repoPaths -> repoPaths.stream()
                                      .filter(repoPath -> artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_RESULT).equals(Optional.of(Result.FAILURE.toString())))
                                      .forEach(artifactoryPropertyService::deleteAllBlackDuckPropertiesFromRepoPath)
            );

        functionAnalyticsCollector.logFunction("blackDuckDeleteScanPropertiesFromFailures", triggerType);
        LogUtil.finish(logger, "blackDuckDeleteScanPropertiesFromFailures", triggerType);
    }

    public void deleteScanPropertiesFromOutOfDate(final TriggerType triggerType) {
        LogUtil.start(logger, "blackDuckDeleteScanPropertiesFromOutOfDate", triggerType);

        repositoryIdentificationService.getRepoKeysToScan().stream()
            .map(repoKey -> artifactoryPropertyService.getAllItemsInRepoWithProperties(repoKey, BlackDuckArtifactoryProperty.UPDATE_STATUS))
            .forEach(repoPaths -> repoPaths.stream()
                                      .filter(repoPath -> artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.UPDATE_STATUS).equals(Optional.of(UpdateStatus.OUT_OF_DATE.toString())))
                                      .forEach(artifactoryPropertyService::deleteAllBlackDuckPropertiesFromRepoPath)
            );

        functionAnalyticsCollector.logFunction("blackDuckDeleteScanPropertiesFromOutOfDate", triggerType);
        LogUtil.finish(logger, "blackDuckDeleteScanPropertiesFromOutOfDate", triggerType);
    }

    public void updateDeprecatedProperties(final TriggerType triggerType) {
        LogUtil.start(logger, "blackDuckUpdateDeprecatedProperties", triggerType);

        repositoryIdentificationService.getRepoKeysToScan()
            .forEach(artifactoryPropertyService::updateAllBlackDuckPropertiesFromRepoKey);

        functionAnalyticsCollector.logFunction("blackDuckUpdateDeprecatedProperties", triggerType);
        LogUtil.finish(logger, "blackDuckUpdateDeprecatedProperties", triggerType);
    }

    public String getStatusCheckMessage(final TriggerType triggerType) {
        LogUtil.start(logger, "blackDuckTestConfig", triggerType);

        final String message = statusCheckService.getStatusMessage();

        functionAnalyticsCollector.logFunction("blackDuckTestConfig", triggerType);
        LogUtil.finish(logger, "blackDuckTestConfig", triggerType);
        return message;
    }

    @Override
    public List<AnalyticsCollector> getAnalyticsCollectors() {
        return Arrays.asList(functionAnalyticsCollector);
    }

    public RepositoryIdentificationService getRepositoryIdentificationService() {
        return repositoryIdentificationService;
    }
}
