package com.blackducksoftware.integration.hub.artifactory.scan;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPath;

import com.blackducksoftware.integration.hub.artifactory.HubConnectionService;

public class StatusCheckService {
    private final ScanPluginManager scanPluginManager;
    private final HubConnectionService hubConnectionService;
    private final RepositoryIdentificationService repositoryIdentificationService;

    public StatusCheckService(final ScanPluginManager scanPluginManager, final HubConnectionService hubConnectionService,
    final RepositoryIdentificationService repositoryIdentificationService) {
        this.scanPluginManager = scanPluginManager;
        this.hubConnectionService = hubConnectionService;
        this.repositoryIdentificationService = repositoryIdentificationService;
    }

    public String getStatusMessage() {
        final StringBuilder statusMessageBuilder = new StringBuilder();

        String connectMessage = "OK";
        try {
            if (hubConnectionService == null) {
                connectMessage = "Could not create the connection to the Hub - you will have to check the artifactory logs.";
            }
        } catch (final Exception e) {
            connectMessage = e.getMessage();
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
        if (StringUtils.isNotBlank(scanPluginManager.getArtifactCutoffDate())) {
            try {
                scanPluginManager.getDateTimeManager().getTimeFromString(scanPluginManager.getArtifactCutoffDate());
                cutoffMessage = "The date cutoff is specified correctly.";
            } catch (final Exception e) {
                cutoffMessage = String.format("The pattern: %s does not match the date string: %s: %s", scanPluginManager.getDateTimeManager().getDateTimePattern(), scanPluginManager.getArtifactCutoffDate(), e.getMessage());
            }
        }

        statusMessageBuilder.append(String.format("canConnectToHub: %s%n", connectMessage));
        statusMessageBuilder.append(String.format("artifactsFound: %s%n", artifactsFoundMessage));
        statusMessageBuilder.append(String.format("dateCutoffStatus: %s%n", cutoffMessage));

        return statusMessageBuilder.toString();
    }
}
