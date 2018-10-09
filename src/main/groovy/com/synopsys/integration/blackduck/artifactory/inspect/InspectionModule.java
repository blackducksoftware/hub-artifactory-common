package com.synopsys.integration.blackduck.artifactory.inspect;

import java.util.Optional;
import java.util.Set;

import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.Repositories;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.LogUtil;
import com.synopsys.integration.blackduck.artifactory.TriggerType;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class InspectionModule {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final InspectionModuleConfig inspectionModuleConfig;
    private final ArtifactIdentificationService artifactIdentificationService;
    private final MetaDataPopulationService metaDataPopulationService;
    private final MetaDataUpdateService metaDataUpdateService;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final Repositories repositories;

    public InspectionModule(final InspectionModuleConfig inspectionModuleConfig, final ArtifactIdentificationService artifactIdentificationService,
        final MetaDataPopulationService metaDataPopulationService, final MetaDataUpdateService metaDataUpdateService, final ArtifactoryPropertyService artifactoryPropertyService, final Repositories repositories) {
        this.inspectionModuleConfig = inspectionModuleConfig;
        this.artifactIdentificationService = artifactIdentificationService;
        this.metaDataPopulationService = metaDataPopulationService;
        this.metaDataUpdateService = metaDataUpdateService;
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.repositories = repositories;
    }

    public InspectionModuleConfig getInspectionModuleConfig() {
        return inspectionModuleConfig;
    }

    public void identifyArtifacts(final TriggerType triggerType) {
        LogUtil.start(logger, "blackDuckIdentifyArtifacts", triggerType);

        inspectionModuleConfig.getRepoKeys()
            .forEach(artifactIdentificationService::identifyArtifacts);

        LogUtil.finish(logger, "blackDuckIdentifyArtifacts", triggerType);
    }

    public void populateMetadata(final TriggerType triggerType) {
        LogUtil.start(logger, "blackDuckPopulateMetadata", triggerType);

        inspectionModuleConfig.getRepoKeys()
            .forEach(metaDataPopulationService::populateMetadata);

        LogUtil.finish(logger, "blackDuckPopulateMetadata", triggerType);
    }

    public void updateMetadata(final TriggerType triggerType) {
        LogUtil.start(logger, "blackDuckUpdateMetadata", triggerType);

        inspectionModuleConfig.getRepoKeys()
            .forEach(metaDataUpdateService::updateMetadata);

        LogUtil.finish(logger, "blackDuckUpdateMetadata", triggerType);
    }

    public void deleteInspectionProperties(final TriggerType triggerType) {
        LogUtil.start(logger, "blackDuckDeleteInspectionProperties", triggerType);

        inspectionModuleConfig.getRepoKeys()
            .forEach(artifactoryPropertyService::deleteAllBlackDuckPropertiesFromRepo);

        LogUtil.finish(logger, "blackDuckDeleteInspectionProperties", triggerType);
    }

    public void updateDeprecatedProperties(final TriggerType triggerType) {
        LogUtil.start(logger, "updateDeprecatedProperties", triggerType);

        inspectionModuleConfig.getRepoKeys()
            .forEach(artifactoryPropertyService::updateAllBlackDuckPropertiesFromRepoKey);

        LogUtil.finish(logger, "updateDeprecatedProperties", triggerType);
    }

    public void inspectItem(final ItemInfo itemInfo, final TriggerType triggerType) {
        LogUtil.start(logger, "inspectItem", triggerType);

        final String repoKey = itemInfo.getRepoKey();
        final RepoPath repoPath = itemInfo.getRepoPath();

        try {
            final String packageType = repositories.getRepositoryConfiguration(repoKey).getPackageType();

            if (inspectionModuleConfig.getRepoKeys().contains(repoKey)) {
                final Optional<Set<RepoPath>> identifiableArtifacts = artifactIdentificationService.getIdentifiableArtifacts(repoKey);

                if (identifiableArtifacts.isPresent() && identifiableArtifacts.get().contains(repoPath)) {
                    final Optional<ArtifactIdentificationService.IdentifiedArtifact> optionalIdentifiedArtifact = artifactIdentificationService.identifyArtifact(repoPath, packageType);
                    optionalIdentifiedArtifact.ifPresent(artifactIdentificationService::populateIdMetadataOnIdentifiedArtifact);
                }
            }
        } catch (final Exception e) {
            logger.error(String.format("Failed to inspect item added to storage: %s", repoPath.toPath()));
            logger.debug(e.getMessage(), e);
        }

        LogUtil.finish(logger, "inspectItem", triggerType);
    }
}
