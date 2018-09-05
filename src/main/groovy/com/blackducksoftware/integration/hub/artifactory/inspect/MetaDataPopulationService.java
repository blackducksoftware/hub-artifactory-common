package com.blackducksoftware.integration.hub.artifactory.inspect;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.hub.artifactory.ArtifactoryPropertyService;
import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryProperty;
import com.blackducksoftware.integration.hub.artifactory.inspect.metadata.ArtifactMetaData;
import com.blackducksoftware.integration.hub.artifactory.inspect.metadata.ArtifactMetaDataService;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class MetaDataPopulationService {
    private final Logger logger = LoggerFactory.getLogger(MetaDataPopulationService.class);

    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final CacheInspectorService cacheInspectorService;
    private final ArtifactMetaDataService artifactMetaDataService;

    public MetaDataPopulationService(final ArtifactoryPropertyService artifactoryPropertyService, final CacheInspectorService cacheInspectorService, final ArtifactMetaDataService artifactMetaDataService) {
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.artifactMetaDataService = artifactMetaDataService;
        this.cacheInspectorService = cacheInspectorService;
    }

    public void populateMetadata(final String repoKey) {
        final RepoPath repoKeyPath = RepoPathFactory.create(repoKey);
        final Optional<InspectionStatus> inspectionStatus = cacheInspectorService.getInspectionStatus(repoKeyPath);

        if (inspectionStatus.isPresent() && inspectionStatus.get().equals(InspectionStatus.PENDING)) {
            try {
                final String projectName = artifactoryPropertyService.getRepoProjectName(repoKey);
                final String projectVersionName = artifactoryPropertyService.getRepoProjectVersionName(repoKey);

                final List<ArtifactMetaData> artifactMetaDataList = artifactMetaDataService.getArtifactMetadataOfRepository(repoKey, projectName, projectVersionName);
                populateBlackDuckMetadataFromIdMetadata(repoKey, artifactMetaDataList);

                cacheInspectorService.setInspectionStatus(repoKeyPath, InspectionStatus.SUCCESS);
            } catch (final Exception e) {
                logger.error(String.format("The blackDuckCacheInspector encountered an exception while populating artifact metadata in repository %s", repoKey), e);
                cacheInspectorService.setInspectionStatus(repoKeyPath, InspectionStatus.FAILURE);
            }
        }
    }

    public void populateBlackDuckMetadataFromIdMetadata(final String repoKey, final List<ArtifactMetaData> artifactMetaDataList) {
        for (final ArtifactMetaData artifactMetaData : artifactMetaDataList) {
            if (StringUtils.isNotBlank(artifactMetaData.originId) && StringUtils.isNotBlank(artifactMetaData.forge)) {
                final SetMultimap<String, String> setMultimap = HashMultimap.create();
                setMultimap.put(BlackDuckArtifactoryProperty.BLACKDUCK_ORIGIN_ID.getName(), artifactMetaData.originId);
                setMultimap.put(BlackDuckArtifactoryProperty.BLACKDUCK_FORGE.getName(), artifactMetaData.forge);
                final List<RepoPath> artifactsWithOriginId = artifactoryPropertyService.getAllItemsInRepoWithPropertiesAndValues(setMultimap, repoKey);
                for (final RepoPath repoPath : artifactsWithOriginId) {
                    artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.HIGH_VULNERABILITIES, Integer.toString(artifactMetaData.highSeverityCount));
                    artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.MEDIUM_VULNERABILITIES, Integer.toString(artifactMetaData.mediumSeverityCount));
                    artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.LOW_VULNERABILITIES, Integer.toString(artifactMetaData.lowSeverityCount));
                    artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS, artifactMetaData.policyStatus.toString());
                    artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.COMPONENT_VERSION_URL, artifactMetaData.componentVersionLink);
                }
            }
        }
    }

}
