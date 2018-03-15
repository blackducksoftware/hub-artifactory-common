package com.blackducksoftware.integration.hub.artifactory;

import java.util.Date;
import java.util.List;

public class ArtifactMetaDataFromNotifications {
    private final Date lastNotificationDate;
    private final List<ArtifactMetaData> artifactMetaData;

    public ArtifactMetaDataFromNotifications(final Date lastNotificationDate, final List<ArtifactMetaData> artifactMetaData) {
        this.lastNotificationDate = lastNotificationDate;
        this.artifactMetaData = artifactMetaData;
    }

    public Date getLastNotificationDate() {
        return lastNotificationDate;
    }

    public List<ArtifactMetaData> getArtifactMetaData() {
        return artifactMetaData;
    }

}
