package com.blackducksoftware.integration.hub.artifactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.repo.Repositories;
import org.artifactory.search.Searches;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class ArtifactoryPropertyService {
    private final Repositories repositories;
    private final Searches searches;
    private final DateTimeManager dateTimeManager;

    public ArtifactoryPropertyService(final Repositories repositories, final Searches searches, final DateTimeManager dateTimeManager) {
        this.repositories = repositories;
        this.searches = searches;
        this.dateTimeManager = dateTimeManager;
    }

    public String getProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property) {
        return repositories.getProperty(repoPath, property.getName());
    }

    public Date getDateFromProperty(final RepoPath repoPath, final BlackDuckArtifactoryProperty property) {
        final String dateTimeAsString = getProperty(repoPath, property);
        return dateTimeManager.getDateFromString(dateTimeAsString);
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

    public String getRepoProjectName(final String repoKey) {
        String projectName;
        final RepoPath repoPath = RepoPathFactory.create(repoKey);
        final String projectNameProperty = getProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_NAME);
        if (StringUtils.isNotBlank(projectNameProperty)) {
            projectName = projectNameProperty;
        } else {
            projectName = repoKey;
        }
        return projectName;
    }

    public String getRepoProjectVersionName(final String repoKey) {
        String projectVersionName;
        final RepoPath repoPath = RepoPathFactory.create(repoKey);
        final String projectVersionNameProperty = getProperty(repoPath, BlackDuckArtifactoryProperty.HUB_PROJECT_VERSION_NAME);
        if (StringUtils.isNotBlank(projectVersionNameProperty)) {
            projectVersionName = projectVersionNameProperty;
        } else {
            try {
                projectVersionName = InetAddress.getLocalHost().getHostName();
            } catch (final UnknownHostException e) {
                projectVersionName = "UNKNOWN_HOST";
            }
        }
        return projectVersionName;
    }

}
