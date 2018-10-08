package com.synopsys.integration.blackduck.artifactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.Repositories;
import org.artifactory.search.Searches;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.util.NameVersion;

public class ArtifactoryPropertyService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    private final Repositories repositories;
    private final Searches searches;
    private final DateTimeManager dateTimeManager;

    public ArtifactoryPropertyService(final Repositories repositories, final Searches searches, final DateTimeManager dateTimeManager) {
        this.repositories = repositories;
        this.searches = searches;
        this.dateTimeManager = dateTimeManager;
    }

    public Optional<String> getProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property) {
        String propertyValue = StringUtils.stripToNull(repositories.getProperty(repoPath, property.getName()));

        // TODO: Remove after re-branding
        // If the property isn't found, see if it can be found by its deprecated name
        if (propertyValue == null) {
            propertyValue = StringUtils.stripToNull(repositories.getProperty(repoPath, property.getOldName()));

            if (propertyValue != null) {
                if (property.getName() == null) {
                    logger.warn(String.format("The property %s is deprecated! This should be removed from: %s", property.getOldName(), repoPath.toPath()));
                } else {
                    logger.warn(String.format("Property %s is deprecated! Please use %s: %s", property.getOldName(), property.getName(), repoPath.toPath()));
                }
                logger.warn(
                    "You can hit this endpoint to update all the BlackDuck properties with the following command: curl -X POST -u admin:password \"http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckUpdateDeprecatedProperties\""
                );
            }
        }

        return Optional.ofNullable(propertyValue);
    }

    public Optional<Date> getDateFromProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property) {
        final Optional<String> dateTimeAsString = getProperty(repoPath, property);

        return dateTimeAsString.map(dateTimeManager::getDateFromString);
    }

    public void setProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property, final String value) {
        repositories.setProperty(repoPath, property.getName(), value);
        logger.info(String.format("Set property %s to %s on %s", property.getName(), value, repoPath.toPath()));
    }

    public void setPropertyToDate(final RepoPath repoPath, final BlackDuckArtifactoryProperty property, final Date date) {
        final String dateTimeAsString = dateTimeManager.getStringFromDate(date);
        setProperty(repoPath, property, dateTimeAsString);
    }

    public void deleteProperty(final RepoPath repoPath, final String propertyName) {
        repositories.deleteProperty(repoPath, propertyName);
        logger.info(String.format("Removed property %s from %s", propertyName, repoPath.toPath()));
    }

    public void deleteProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property) {
        deleteProperty(repoPath, property.getName());
    }

    public void deleteDeprecatedProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property) {
        deleteProperty(repoPath, property.getOldName());
    }

    public void deleteAllBlackDuckPropertiesFromRepo(final String repoKey) {
        Arrays.stream(BlackDuckArtifactoryProperty.values())
            .forEach(artifactoryProperty -> getAllItemsInRepoWithAnyProperties(repoKey, artifactoryProperty)
                                                .forEach(this::deleteAllBlackDuckPropertiesFromRepoPath)
            );
    }

    public void deleteAllBlackDuckPropertiesFromRepoPath(final RepoPath repoPath) {
        Arrays.stream(BlackDuckArtifactoryProperty.values())
            .filter(property -> property.getName() != null)
            .forEach(property -> deleteProperty(repoPath, property));
    }

    public List<RepoPath> getAllItemsInRepoWithProperties(final String repoKey, final BlackDuckArtifactoryProperty... properties) {
        final SetMultimap<String, String> setMultimap = HashMultimap.create();
        Arrays.stream(properties)
            .filter(property -> property.getName() != null)
            .forEach(property -> setMultimap.put(property.getName(), "*"));

        return getAllItemsInRepoWithPropertiesAndValues(setMultimap, repoKey);
    }

    public List<RepoPath> getAllItemsInRepoWithAnyProperties(final String repoKey, final BlackDuckArtifactoryProperty... properties) {
        final List<RepoPath> repoPaths = new ArrayList<>();

        Arrays.stream(properties)
            .map(property -> getAllItemsInRepoWithProperties(repoKey, property))
            .forEach(repoPaths::addAll);

        return repoPaths;
    }

    public List<RepoPath> getAllItemsInRepoWithPropertiesAndValues(final SetMultimap<String, String> setMultimap, final String repoKey) {
        return searches.itemsByProperties(setMultimap, repoKey);
    }

    public Optional<NameVersion> getProjectNameVersion(final RepoPath repoPath) {
        final Optional<String> projectName = getProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME);
        final Optional<String> projectVersionName = getProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME);
        NameVersion nameVersion = null;

        if (projectName.isPresent() && projectVersionName.isPresent()) {
            nameVersion = new NameVersion(projectName.get(), projectVersionName.get());
        }

        return Optional.ofNullable(nameVersion);
    }

    public List<RepoPath> getAllItemsInRepoWithDeprecatedProperties(final String repoKey, final BlackDuckArtifactoryProperty... properties) {
        final SetMultimap<String, String> setMultimap = HashMultimap.create();
        Arrays.stream(properties)
            .filter(property -> StringUtils.isNotBlank(property.getOldName()))
            .forEach(property -> setMultimap.put(property.getOldName(), "*"));

        return getAllItemsInRepoWithPropertiesAndValues(setMultimap, repoKey);
    }

    public void updateDeprecatedPropertyName(final RepoPath repoPath, final BlackDuckArtifactoryProperty artifactoryProperty) {
        final String deprecatedName = artifactoryProperty.getOldName();

        if (StringUtils.isBlank(deprecatedName) || !repositories.hasProperty(repoPath, deprecatedName)) {
            return; // Nothing to update
        }

        if (artifactoryProperty.getName() == null) {
            deleteDeprecatedProperty(repoPath, artifactoryProperty);
        } else {
            final String propertyValue = repositories.getProperty(repoPath, deprecatedName);
            deleteDeprecatedProperty(repoPath, artifactoryProperty);
            setProperty(repoPath, artifactoryProperty, propertyValue);
            logger.info(String.format("Renamed property %s to %s on %s", artifactoryProperty.getOldName(), artifactoryProperty.getName(), repoPath.toPath()));
        }
    }

    public void updateAllBlackDuckPropertiesFrom(final String repoKey) {
        for (final BlackDuckArtifactoryProperty property : BlackDuckArtifactoryProperty.values()) {
            final List<RepoPath> repoPathsWithProperty = getAllItemsInRepoWithDeprecatedProperties(repoKey, property);
            repoPathsWithProperty.forEach(repoPath -> updateDeprecatedPropertyName(repoPath, property));
        }
    }
}
