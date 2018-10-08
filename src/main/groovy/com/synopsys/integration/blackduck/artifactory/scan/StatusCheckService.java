package com.synopsys.integration.blackduck.artifactory.scan;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPath;

import com.synopsys.integration.blackduck.artifactory.BlackDuckConnectionService;
import com.synopsys.integration.blackduck.artifactory.DateTimeManager;

public class StatusCheckService {
    private final ScanModuleConfig scanModuleConfig;
    private final BlackDuckConnectionService blackDuckConnectionService;
    private final RepositoryIdentificationService repositoryIdentificationService;
    private final DateTimeManager dateTimeManager;

    public StatusCheckService(final ScanModuleConfig scanModuleConfig, final BlackDuckConnectionService blackDuckConnectionService,
        final RepositoryIdentificationService repositoryIdentificationService, final DateTimeManager dateTimeManager) {
        this.scanModuleConfig = scanModuleConfig;
        this.blackDuckConnectionService = blackDuckConnectionService;
        this.repositoryIdentificationService = repositoryIdentificationService;
        this.dateTimeManager = dateTimeManager;
    }

    public String getStatusMessage() {
        final StringBuilder statusMessageBuilder = new StringBuilder();

        String connectMessage = "OK";
        if (blackDuckConnectionService == null) {
            connectMessage = "Could not create the connection to BlackDuck - you will have to check the artifactory logs.";
        }

        Set<RepoPath> repoPaths = null;
        String artifactsFoundMessage = "UNKNOWN - you will have to check the artifactory logs.";
        try {
            repoPaths = repositoryIdentificationService.searchForRepoPaths();
        } catch (final IOException e) {
            artifactsFoundMessage = e.getMessage();
        }
        if (repoPaths != null) {
            artifactsFoundMessage = String.valueOf(repoPaths.size());
        }

        String cutoffMessage = "The date cutoff is not specified so all artifacts that are found will be scanned.";
        if (StringUtils.isNotBlank(scanModuleConfig.getArtifactCutoffDate())) {
            try {
                dateTimeManager.getTimeFromString(scanModuleConfig.getArtifactCutoffDate());
                cutoffMessage = "The date cutoff is specified correctly.";
            } catch (final Exception e) {
                cutoffMessage = String.format("The pattern: %s does not match the date string: %s: %s", dateTimeManager.getDateTimePattern(), scanModuleConfig.getArtifactCutoffDate(), e.getMessage());
            }
        }

        statusMessageBuilder.append(String.format("canConnectToHub: %s%n", connectMessage));
        statusMessageBuilder.append(String.format("artifactsFound: %s%n", artifactsFoundMessage));
        statusMessageBuilder.append(String.format("dateCutoffStatus: %s%n", cutoffMessage));

        return statusMessageBuilder.toString();
    }
}
