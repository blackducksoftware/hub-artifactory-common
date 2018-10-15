package com.synopsys.integration.blackduck.artifactory.inspect;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.Repositories;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.LogUtil;
import com.synopsys.integration.blackduck.artifactory.TriggerType;
import com.synopsys.integration.blackduck.artifactory.analytics.AnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.analytics.Analyzable;
import com.synopsys.integration.blackduck.artifactory.analytics.FeatureAnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.analytics.SimpleAnalyticsCollector;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class InspectionModule implements Analyzable {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final InspectionModuleConfig inspectionModuleConfig;
    private final ArtifactIdentificationService artifactIdentificationService;
    private final MetaDataPopulationService metaDataPopulationService;
    private final MetaDataUpdateService metaDataUpdateService;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final Repositories repositories;
    private final FeatureAnalyticsCollector featureAnalyticsCollector;
    private final SimpleAnalyticsCollector simpleAnalyticsCollector;

    public InspectionModule(final InspectionModuleConfig inspectionModuleConfig, final ArtifactIdentificationService artifactIdentificationService, final MetaDataPopulationService metaDataPopulationService,
        final MetaDataUpdateService metaDataUpdateService, final ArtifactoryPropertyService artifactoryPropertyService, final Repositories repositories, final FeatureAnalyticsCollector featureAnalyticsCollector,
        final SimpleAnalyticsCollector simpleAnalyticsCollector) {
        this.inspectionModuleConfig = inspectionModuleConfig;
        this.artifactIdentificationService = artifactIdentificationService;
        this.metaDataPopulationService = metaDataPopulationService;
        this.metaDataUpdateService = metaDataUpdateService;
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.repositories = repositories;
        this.featureAnalyticsCollector = featureAnalyticsCollector;
        this.simpleAnalyticsCollector = simpleAnalyticsCollector;
    }

    public InspectionModuleConfig getInspectionModuleConfig() {
        return inspectionModuleConfig;
    }

    public void identifyArtifacts(final TriggerType triggerType) {
        LogUtil.start(logger, "blackDuckIdentifyArtifacts", triggerType);

        inspectionModuleConfig.getRepoKeys()
            .forEach(artifactIdentificationService::identifyArtifacts);

        featureAnalyticsCollector.logFeatureHit("blackDuckIdentifyArtifacts", triggerType);
        LogUtil.finish(logger, "blackDuckIdentifyArtifacts", triggerType);
    }

    public void populateMetadata(final TriggerType triggerType) {
        LogUtil.start(logger, "blackDuckPopulateMetadata", triggerType);

        inspectionModuleConfig.getRepoKeys()
            .forEach(metaDataPopulationService::populateMetadata);

        featureAnalyticsCollector.logFeatureHit("blackDuckPopulateMetadata", triggerType);
        LogUtil.finish(logger, "blackDuckPopulateMetadata", triggerType);
    }

    public void updateMetadata(final TriggerType triggerType) {
        LogUtil.start(logger, "blackDuckUpdateMetadata", triggerType);

        inspectionModuleConfig.getRepoKeys()
            .forEach(metaDataUpdateService::updateMetadata);

        featureAnalyticsCollector.logFeatureHit("blackDuckUpdateMetadata", triggerType);
        LogUtil.finish(logger, "blackDuckUpdateMetadata", triggerType);
    }

    public void deleteInspectionProperties(final TriggerType triggerType) {
        LogUtil.start(logger, "blackDuckDeleteInspectionProperties", triggerType);

        inspectionModuleConfig.getRepoKeys()
            .forEach(artifactoryPropertyService::deleteAllBlackDuckPropertiesFromRepo);

        featureAnalyticsCollector.logFeatureHit("blackDuckDeleteInspectionProperties", triggerType);
        LogUtil.finish(logger, "blackDuckDeleteInspectionProperties", triggerType);
    }

    public void updateDeprecatedProperties(final TriggerType triggerType) {
        LogUtil.start(logger, "updateDeprecatedScanProperties", triggerType);

        inspectionModuleConfig.getRepoKeys()
            .forEach(artifactoryPropertyService::updateAllBlackDuckPropertiesFromRepoKey);

        featureAnalyticsCollector.logFeatureHit("updateDeprecatedScanProperties", triggerType);
        LogUtil.finish(logger, "updateDeprecatedScanProperties", triggerType);
    }

    public void handleAfterCreateEvent(final ItemInfo itemInfo, final TriggerType triggerType) {
        LogUtil.start(logger, "handleAfterCreateEvent", triggerType);

        final String repoKey = itemInfo.getRepoKey();
        final RepoPath repoPath = itemInfo.getRepoPath();

        boolean successfulInspection;
        try {
            final String packageType = repositories.getRepositoryConfiguration(repoKey).getPackageType();

            if (inspectionModuleConfig.getRepoKeys().contains(repoKey)) {
                final Optional<Set<RepoPath>> identifiableArtifacts = artifactIdentificationService.getIdentifiableArtifacts(repoKey);

                if (identifiableArtifacts.isPresent() && identifiableArtifacts.get().contains(repoPath)) {
                    final Optional<ArtifactIdentificationService.IdentifiedArtifact> optionalIdentifiedArtifact = artifactIdentificationService.identifyArtifact(repoPath, packageType);
                    optionalIdentifiedArtifact.ifPresent(artifactIdentificationService::populateIdMetadataOnIdentifiedArtifact);
                }
            }
            successfulInspection = true;
        } catch (final Exception e) {
            logger.error(String.format("Failed to inspect item added to storage: %s", repoPath.toPath()));
            logger.debug(e.getMessage(), e);
            successfulInspection = false;
        }

        featureAnalyticsCollector.logFeatureHit("handleAfterCreateEvent", successfulInspection);
        LogUtil.finish(logger, "handleAfterCreateEvent", triggerType);
    }

    @Override
    public List<AnalyticsCollector> getAnalyticsCollectors() {
        return Arrays.asList(featureAnalyticsCollector, simpleAnalyticsCollector);
    }
}
