package com.synopsys.integration.blackduck.artifactory.inspect;

import java.util.Date;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.inspect.metadata.ArtifactMetaDataFromNotifications;
import com.synopsys.integration.blackduck.artifactory.inspect.metadata.ArtifactMetaDataService;
import com.synopsys.integration.exception.IntegrationException;

public class MetaDataUpdateService {
    private final Logger logger = LoggerFactory.getLogger(MetaDataUpdateService.class);

    public static String UP_TO_DATE = "UP TO DATE";
    public static String OUT_OF_DATE = "OUT OF DATE";

    private final ArtifactMetaDataService artifactMetaDataService;
    private final MetaDataPopulationService metadataPopulationService;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final CacheInspectorService cacheInspectorService;

    public MetaDataUpdateService(final ArtifactoryPropertyService artifactoryPropertyService, final CacheInspectorService cacheInspectorService, final ArtifactMetaDataService artifactMetaDataService,
    final MetaDataPopulationService metadataPopulationService) {
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.cacheInspectorService = cacheInspectorService;
        this.artifactMetaDataService = artifactMetaDataService;
        this.metadataPopulationService = metadataPopulationService;
    }

    public void updateMetadata(final String repoKey) {
        final RepoPath repoKeyPath = RepoPathFactory.create(repoKey);
        final Optional<InspectionStatus> inspectionStatus = cacheInspectorService.getInspectionStatus(repoKeyPath);

        if (inspectionStatus.isPresent() && inspectionStatus.get().equals(InspectionStatus.SUCCESS)) {
            try {
                final Date now = new Date();
                final Date dateToCheck;
                try {
                    if (StringUtils.isNotBlank(artifactoryPropertyService.getProperty(repoKeyPath, BlackDuckArtifactoryProperty.LAST_UPDATE))) {
                        dateToCheck = artifactoryPropertyService.getDateFromProperty(repoKeyPath, BlackDuckArtifactoryProperty.LAST_UPDATE);
                    } else {
                        dateToCheck = artifactoryPropertyService.getDateFromProperty(repoKeyPath, BlackDuckArtifactoryProperty.LAST_INSPECTION);
                    }
                } catch (final NullPointerException npe) {
                    throw new IntegrationException(String.format(
                    "Could not find timestamp property on %s. Black Duck artifactory metadata is likely malformed and requires re-inspection. Run the blackDuckDeleteInspectorProperties rest endpoint to re-inspect all configured repositories or delete the malformed properties manually.",
                    repoKeyPath.toPath()), npe);
                }
                final String projectName = artifactoryPropertyService.getRepoProjectName(repoKey);
                final String projectVersionName = artifactoryPropertyService.getRepoProjectVersionName(repoKey);

                final Date lastNotificationDate = updateFromHubProjectNotifications(repoKey, projectName, projectVersionName, dateToCheck, now);
                artifactoryPropertyService.setProperty(repoKeyPath, BlackDuckArtifactoryProperty.UPDATE_STATUS, UP_TO_DATE);
                artifactoryPropertyService.setPropertyToDate(repoKeyPath, BlackDuckArtifactoryProperty.LAST_UPDATE, lastNotificationDate);
            } catch (final Exception e) {
                logger.error(String.format("The blackDuckCacheInspector encountered an exception while updating artifact metadata from Hub notifications in repository %s:", repoKey), e);
                artifactoryPropertyService.setProperty(repoKeyPath, BlackDuckArtifactoryProperty.UPDATE_STATUS, OUT_OF_DATE);
            }
        }
    }

    private Date updateFromHubProjectNotifications(final String repoKey, final String projectName, final String projectVersionName, final Date startDate, final Date endDate) throws IntegrationException {
        final ArtifactMetaDataFromNotifications artifactMetaDataFromNotifications = artifactMetaDataService.getArtifactMetadataFromNotifications(repoKey, projectName, projectVersionName, startDate, endDate);
        metadataPopulationService.populateBlackDuckMetadataFromIdMetadata(repoKey, artifactMetaDataFromNotifications.getArtifactMetaData());

        final Optional<Date> lastNotificationDate = artifactMetaDataFromNotifications.getLastNotificationDate();
        final Date lastDateParsed;

        if (lastNotificationDate.isPresent()) {
            lastDateParsed = lastNotificationDate.get();
        } else {
            // We don't want to miss notifications, so if something goes wrong we will err on the side of caution.
            lastDateParsed = startDate;
        }

        return lastDateParsed;
    }

}
