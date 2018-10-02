package com.synopsys.integration.blackduck.artifactory;

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
        Optional<String> propertyValue = Optional.ofNullable(repositories.getProperty(repoPath, property.getName()))
                                             .map(StringUtils::stripToNull);

        // TODO: Replace this with the Optional.or in Java 9 or later
        // If the property isn't found, see if it can be found by its deprecated name
        if (!propertyValue.isPresent()) {
            propertyValue = Optional.ofNullable(repositories.getProperty(repoPath, property.getOldName()))
                                .map(StringUtils::stripToNull);
            propertyValue.ifPresent(ignored -> {
                logger.warn(String.format("Property %s is deprecated! Please use %s: %s", property.getOldName(), property.getName(), repoPath.getPath()));
                logger.warn(
                    "You can hit this endpoint to update all the properties with the following command: curl -X POST -u admin:password \"http://ARTIFACTORY_SERVER/artifactory/api/plugins/execute/blackDuckScanUpdateDeprecatedProperties\"");
            });
        }

        return propertyValue;
    }

    public Optional<Date> getDateFromProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property) {
        final Optional<String> dateTimeAsString = getProperty(repoPath, property);

        return dateTimeAsString.map(dateTimeManager::getDateFromString);
    }

    public void setProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property, final String value) {
        repositories.setProperty(repoPath, property.getName(), value);
    }

    public void setPropertyToDate(final RepoPath repoPath, final BlackDuckArtifactoryProperty property, final Date date) {
        final String dateTimeAsString = dateTimeManager.getStringFromDate(date);
        setProperty(repoPath, property, dateTimeAsString);
    }

    public void deleteProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property) {
        deleteProperty(repoPath, property, false);
    }

    public void deleteProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property, final boolean deleteOldName) {
        String propertyName = property.getName();
        if (deleteOldName && StringUtils.isNotBlank(property.getOldName())) {
            propertyName = property.getOldName();
        }

        repositories.deleteProperty(repoPath, propertyName);
    }

    public void deleteAllBlackDuckPropertiesFrom(final String repoKey) {
        for (final BlackDuckArtifactoryProperty property : BlackDuckArtifactoryProperty.values()) {
            final List<RepoPath> repoPathsWithProperty = getAllItemsInRepoWithProperties(repoKey, property);
            repoPathsWithProperty.forEach(repoPath -> deleteProperty(repoPath, property));
        }
    }

    public List<RepoPath> getAllItemsInRepoWithProperties(final String repoKey, final BlackDuckArtifactoryProperty... properties) {
        final SetMultimap<String, String> setMultimap = HashMultimap.create();
        Arrays.stream(properties).forEach(property -> setMultimap.put(property.getName(), "*"));

        return getAllItemsInRepoWithPropertiesAndValues(setMultimap, repoKey);
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

        if (StringUtils.isBlank(deprecatedName)) {
            return; // Nothing to update
        }

        if (repositories.hasProperty(repoPath, deprecatedName)) {
            final String propertyValue = repositories.getProperty(repoPath, artifactoryProperty.getOldName());
            deleteProperty(repoPath, artifactoryProperty, true);
            setProperty(repoPath, artifactoryProperty, propertyValue);
        }
    }

    public void updateAllBlackDuckPropertiesFrom(final String repoKey) {
        for (final BlackDuckArtifactoryProperty property : BlackDuckArtifactoryProperty.values()) {
            final List<RepoPath> repoPathsWithProperty = getAllItemsInRepoWithDeprecatedProperties(repoKey, property);
            repoPathsWithProperty.forEach(repoPath -> updateDeprecatedPropertyName(repoPath, property));
        }
    }
}
