package com.blackducksoftware.integration.hub.artifactory.scan;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.RepoPathFactory;
import org.artifactory.repo.Repositories;
import org.artifactory.repo.RepositoryConfiguration;
import org.artifactory.search.Searches;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryConfig;
import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryProperty;
import com.blackducksoftware.integration.hub.artifactory.DateTimeManager;

public class RepositoryIdentificationService {
    private final Logger logger = LoggerFactory.getLogger(RepositoryIdentificationService.class);

    private final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig;
    private final DateTimeManager dateTimeManager;
    private final Repositories repositories;
    private final Searches searches;

    private final List<String> repoKeysToScan = new ArrayList<>();

    public RepositoryIdentificationService(final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig, final ScanArtifactoryConfig scanArtifactoryConfig, final Repositories repositories, final Searches searches) {
        this.blackDuckArtifactoryConfig = blackDuckArtifactoryConfig;
        this.searches = searches;
        this.dateTimeManager = scanArtifactoryConfig.getDateTimeManager();
        this.repositories = repositories;

        loadRepositoriesToScan();
    }

    private void loadRepositoriesToScan() {
        try {
            repoKeysToScan.addAll(blackDuckArtifactoryConfig.getRepositoryKeysFromProperties(ScanPluginProperty.REPOS, ScanPluginProperty.REPOS_CSV_PATH));
        } catch (final IOException e) {
            logger.error(String.format("Exception while attempting to extract repositories from '%s'", blackDuckArtifactoryConfig.getProperty(ScanPluginProperty.REPOS_CSV_PATH)));
        }

        final List<String> invalidRepoKeys = new ArrayList<>();
        for (final String repoKey : repoKeysToScan) {
            final RepoPath repoPath = RepoPathFactory.create(repoKey);
            final RepositoryConfiguration repositoryConfiguration = repositories.getRepositoryConfiguration(repoKey);
            if (!repositories.exists(repoPath) || repositoryConfiguration == null) {
                invalidRepoKeys.add(repoKey);
                logger.warn(String.format("Black Duck Scan For Hub will not scan artifacts in configured repository '%s': Repository was not found or is not a valid repository.", repoKey));
            }
        }

        repoKeysToScan.removeAll(invalidRepoKeys);
    }

    public Set<RepoPath> searchForRepoPaths() throws IOException {
        final List<String> patternsToScan = Arrays.asList(blackDuckArtifactoryConfig.getProperty(ScanPluginProperty.NAME_PATTERNS).split(","));
        final List<String> repoKeysToScan = blackDuckArtifactoryConfig.getRepositoryKeysFromProperties(ScanPluginProperty.REPOS, ScanPluginProperty.REPOS_CSV_PATH);
        final List<RepoPath> repoPaths = new ArrayList<>();

        for (final String pattern : patternsToScan) {
            repoPaths.addAll(searches.artifactsByName(pattern, repoKeysToScan.toArray(new String[repoKeysToScan.size()])));
        }
        logger.debug(String.format("patternsToScan: %d", patternsToScan.size()));
        logger.debug(String.format("repoKeysToScan: %d", repoKeysToScan.size()));
        logger.debug(String.format("repoPaths: %d", repoPaths.size()));
        return new HashSet<>(repoPaths);
    }

    /**
     * If artifact's last modified time is newer than the scan time, or we have no record of the scan time, we should scan now, unless, if the cutoff date is set, only scan if the modified date is greater than or equal to the cutoff.
     */
    boolean shouldRepoPathBeScannedNow(final RepoPath repoPath) {
        final ItemInfo itemInfo = repositories.getItemInfo(repoPath);
        final long lastModifiedTime = itemInfo.getLastModified();
        final String artifactCutoffDate = blackDuckArtifactoryConfig.getProperty(ScanPluginProperty.CUTOFF_DATE);

        boolean shouldCutoffPreventScanning = false;
        if (StringUtils.isNotBlank(artifactCutoffDate)) {
            try {
                final long cutoffTime = dateTimeManager.getTimeFromString(artifactCutoffDate);
                shouldCutoffPreventScanning = lastModifiedTime < cutoffTime;
            } catch (final Exception e) {
                logger.error(String.format("The pattern: %s does not match the date string: %s", dateTimeManager.getDateTimePattern(), artifactCutoffDate), e);
                shouldCutoffPreventScanning = false;
            }
        }

        if (shouldCutoffPreventScanning) {
            logger.warn(String.format("%s was not scanned because the cutoff was set and the artifact is too old", itemInfo.getName()));
            return false;
        }

        final String blackDuckScanTimeProperty = repositories.getProperty(repoPath, BlackDuckArtifactoryProperty.SCAN_TIME.getName());
        if (StringUtils.isBlank(blackDuckScanTimeProperty)) {
            return true;
        }

        try {
            final long blackDuckScanTime = dateTimeManager.getTimeFromString(blackDuckScanTimeProperty);
            return lastModifiedTime >= blackDuckScanTime;
        } catch (final Exception e) {
            //if the date format changes, the old format won't parse, so just cleanup the property by returning true and re-scanning
            logger.error("Exception parsing the scan date (most likely the format changed)", e);
        }

        return true;
    }
}