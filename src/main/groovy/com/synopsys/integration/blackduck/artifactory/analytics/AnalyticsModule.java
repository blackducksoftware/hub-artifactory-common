package com.synopsys.integration.blackduck.artifactory.analytics;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.repo.Repositories;
import org.artifactory.repo.RepositoryConfiguration;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.LogUtil;
import com.synopsys.integration.blackduck.artifactory.TriggerType;
import com.synopsys.integration.blackduck.artifactory.inspect.InspectionModuleConfig;
import com.synopsys.integration.blackduck.artifactory.scan.RepositoryIdentificationService;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class AnalyticsModule implements Analyzable {
    public final static String UPDATE_ANALYTICS_CRON = "0 0 * ? * * *"; // Every hour
    public final static String SUBMIT_ANALYTICS_CRON = "0 0 0 ? * * *"; // Every day at 12 am

    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final AnalyticsService analyticsService;
    private final FunctionAnalyticsCollector functionAnalyticsCollector;
    private final SimpleAnalyticsCollector simpleAnalyticsCollector;
    private final RepositoryIdentificationService repositoryIdentificationService;
    private final InspectionModuleConfig inspectionModuleConfig;
    private final Repositories repositories;

    public AnalyticsModule(final AnalyticsService analyticsService, final FunctionAnalyticsCollector functionAnalyticsCollector, final SimpleAnalyticsCollector simpleAnalyticsCollector,
        final RepositoryIdentificationService repositoryIdentificationService, final InspectionModuleConfig inspectionModuleConfig, final Repositories repositories) {
        this.analyticsService = analyticsService;
        this.functionAnalyticsCollector = functionAnalyticsCollector;
        this.simpleAnalyticsCollector = simpleAnalyticsCollector;
        this.repositoryIdentificationService = repositoryIdentificationService;
        this.inspectionModuleConfig = inspectionModuleConfig;
        this.repositories = repositories;
    }

    @Override
    public List<AnalyticsCollector> getAnalyticsCollectors() {
        return Arrays.asList(functionAnalyticsCollector, simpleAnalyticsCollector);
    }

    /**
     * Submits a payload to phone home with data from all the collectors ({@link FunctionAnalyticsCollector})
     * This should be used infrequently such as once a day due to quota
     */
    public void submitAnalytics(final TriggerType triggerType) {
        LogUtil.start(logger, "submitAnalytics", triggerType);
        functionAnalyticsCollector.logFunction("submitAnalytics", triggerType);

        analyticsService.submitAnalytics();

        LogUtil.finish(logger, "submitAnalytics", triggerType);
    }

    public void updateAnalytics(final TriggerType triggerType) {
        LogUtil.start(logger, "updateAnalytics", triggerType);
        functionAnalyticsCollector.logFunction("updateAnalytics", triggerType);

        final List<String> scanRepositoryKeys = repositoryIdentificationService.getRepoKeysToScan();
        simpleAnalyticsCollector.putMetadata("scan.repo.count", scanRepositoryKeys.size());
        simpleAnalyticsCollector.putMetadata("scan.artifact.count", getArtifactCount(scanRepositoryKeys));

        final List<String> cacheRepositoryKeys = inspectionModuleConfig.getRepoKeys();
        simpleAnalyticsCollector.putMetadata("cache.repo.count", cacheRepositoryKeys.size());
        simpleAnalyticsCollector.putMetadata("cache.artifact.count", getArtifactCount(cacheRepositoryKeys));
        simpleAnalyticsCollector.putMetadata("cache.package.managers", StringUtils.join(getPackageManagers(cacheRepositoryKeys), "/"));

        LogUtil.finish(logger, "updateAnalytics", triggerType);
    }

    private List<String> getPackageManagers(final List<String> repoKeys) {
        return repoKeys.stream()
                   .map(repositories::getRepositoryConfiguration)
                   .map(RepositoryConfiguration::getPackageType)
                   .collect(Collectors.toList());
    }

    private Long getArtifactCount(final List<String> repoKeys) {
        return repoKeys.stream()
                   .map(RepoPathFactory::create)
                   .map(repositories::getArtifactsCount)
                   .mapToLong(Long::longValue)
                   .sum();
    }
}
