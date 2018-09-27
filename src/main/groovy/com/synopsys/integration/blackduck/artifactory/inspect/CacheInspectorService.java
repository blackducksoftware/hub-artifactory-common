package com.synopsys.integration.blackduck.artifactory.inspect;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.repo.Repositories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryConfig;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.util.HostNameHelper;

public class CacheInspectorService {
    private final Logger logger = LoggerFactory.getLogger(CacheInspectorService.class);

    private final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig;
    private final Repositories repositories;
    private final ArtifactoryPropertyService artifactoryPropertyService;

    public CacheInspectorService(final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig, final Repositories repositories, final ArtifactoryPropertyService artifactoryPropertyService) {
        this.repositories = repositories;
        this.blackDuckArtifactoryConfig = blackDuckArtifactoryConfig;
        this.artifactoryPropertyService = artifactoryPropertyService;
    }

    public void setInspectionStatus(final RepoPath repoPath, final InspectionStatus status) {
        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS, status.name());
        artifactoryPropertyService.setPropertyToDate(repoPath, BlackDuckArtifactoryProperty.LAST_INSPECTION, new Date());
    }

    public Optional<InspectionStatus> getInspectionStatus(final RepoPath repoPath) {
        final Optional<String> inspectionStatusString = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.INSPECTION_STATUS);
        InspectionStatus status = null;

        if (inspectionStatusString.isPresent()) {
            try {
                status = InspectionStatus.valueOf(inspectionStatusString.get());
            } catch (final IllegalArgumentException ignore) {

            }
        }

        return Optional.ofNullable(status);
    }

    public String getRepoProjectName(final String repoKey) {
        final RepoPath repoPath = RepoPathFactory.create(repoKey);
        final Optional<String> projectNameProperty = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME);

        return projectNameProperty.orElse(repoKey);
    }

    public String getRepoProjectVersionName(final String repoKey) {
        final RepoPath repoPath = RepoPathFactory.create(repoKey);
        final Optional<String> projectVersionNameProperty = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME);

        return projectVersionNameProperty.orElse(HostNameHelper.getMyHostName("UNKNOWN_HOST"));
    }

    public List<String> getRepositoriesToInspect() throws IOException {
        final List<String> repoKeys = blackDuckArtifactoryConfig.getRepositoryKeysFromProperties(InspectPluginProperty.REPOS, InspectPluginProperty.REPOS_CSV_PATH);
        return repoKeys.stream().filter(this::isValidRepository).collect(Collectors.toList());
    }

    private boolean isValidRepository(final String repoKey) {
        final boolean isValid;

        final RepoPath repoKeyPath = RepoPathFactory.create(repoKey);
        if (repositories.exists(repoKeyPath) && repositories.getRepositoryConfiguration(repoKey) != null) {
            isValid = true;
        } else {
            logger.warn(String.format("Black Duck Cache Inspector will ignore configured repository \'%s\': Repository was not found or is not a valid repository.", repoKey));
            isValid = false;
        }

        return isValid;
    }

}
