package com.synopsys.integration.blackduck.artifactory;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.Repositories;
import org.artifactory.search.Searches;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.synopsys.integration.util.NameVersion;

public class ArtifactoryPropertyService {
    private final Repositories repositories;
    private final Searches searches;
    private final DateTimeManager dateTimeManager;

    public ArtifactoryPropertyService(final Repositories repositories, final Searches searches, final DateTimeManager dateTimeManager) {
        this.repositories = repositories;
        this.searches = searches;
        this.dateTimeManager = dateTimeManager;
    }

    public Optional<String> getProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property) {
        final String propertyValue = repositories.getProperty(repoPath, property.getName());
        Optional<String> propertyOptional = Optional.empty();

        if (StringUtils.isNotEmpty(propertyValue)) {
            propertyOptional = Optional.of(propertyValue);
        }

        return propertyOptional;
    }

    public Optional<Date> getDateFromProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property) {
        final Optional<String> dateTimeAsString = getProperty(repoPath, property);
        Optional<Date> date = Optional.empty();
        if (dateTimeAsString.isPresent()) {
            date = Optional.of(dateTimeManager.getDateFromString(dateTimeAsString.get()));
        }

        return date;
    }

    public void setProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property, final String value) {
        repositories.setProperty(repoPath, property.getName(), value);
    }

    public void setPropertyToDate(final RepoPath repoPath, final BlackDuckArtifactoryProperty property, final Date date) {
        final String dateTimeAsString = dateTimeManager.getStringFromDate(date);
        setProperty(repoPath, property, dateTimeAsString);
    }

    public void deleteProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property) {
        repositories.deleteProperty(repoPath, property.getName());
    }

    public void deleteAllBlackDuckPropertiesFrom(final String repoKey) {
        for (final BlackDuckArtifactoryProperty property : BlackDuckArtifactoryProperty.values()) {
            final List<RepoPath> repoPathsWithProperty = getAllItemsInRepoWithProperties(repoKey, property);
            repoPathsWithProperty.forEach(repoPath -> repositories.deleteProperty(repoPath, property.getName()));
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

}
