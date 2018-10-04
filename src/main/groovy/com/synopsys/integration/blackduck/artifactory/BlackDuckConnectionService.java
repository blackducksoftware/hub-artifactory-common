package com.synopsys.integration.blackduck.artifactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomPolicyStatusView;
import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.rest.BlackduckRestConnection;
import com.synopsys.integration.blackduck.service.CodeLocationService;
import com.synopsys.integration.blackduck.service.HubService;
import com.synopsys.integration.blackduck.service.HubServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.model.PolicyStatusDescription;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.EncryptionException;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.model.externalid.ExternalId;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.util.NameVersion;

public class BlackDuckConnectionService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(BlackDuckConnectionService.class));

    private final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final DateTimeManager dateTimeManager;

    private final HubServicesFactory hubServicesFactory;

    public BlackDuckConnectionService(final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig, final ArtifactoryPropertyService artifactoryPropertyService,
        final DateTimeManager dateTimeManager) throws EncryptionException {
        this.blackDuckArtifactoryConfig = blackDuckArtifactoryConfig;
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.dateTimeManager = dateTimeManager;

        // Create hub services factory
        final HubServerConfig hubServerConfig = blackDuckArtifactoryConfig.getHubServerConfig();
        final BlackduckRestConnection restConnection;

        if (StringUtils.isNotBlank(blackDuckArtifactoryConfig.getProperty(BlackDuckProperty.API_TOKEN))) {
            restConnection = hubServerConfig.createApiTokenRestConnection(logger);
        } else {
            restConnection = hubServerConfig.createCredentialsRestConnection(logger);
        }

        this.hubServicesFactory = new HubServicesFactory(HubServicesFactory.createDefaultGson(), HubServicesFactory.createDefaultJsonParser(), restConnection, logger);
    }

    public void phoneHome() {
        try {
            String pluginVersion = null;
            final File versionFile = blackDuckArtifactoryConfig.getVersionFile();
            if (versionFile != null) {
                pluginVersion = FileUtils.readFileToString(versionFile, StandardCharsets.UTF_8);
            }

            phoneHome(pluginVersion, blackDuckArtifactoryConfig.getThirdPartyVersion(), blackDuckArtifactoryConfig.getPluginType().getName());
        } catch (final Exception ignored) {
        }
    }

    private void phoneHome(String pluginVersion, String thirdPartyVersion, final String pluginName) throws EncryptionException {
        if (pluginVersion == null) {
            pluginVersion = "UNKNOWN_VERSION";
        }

        if (thirdPartyVersion == null) {
            thirdPartyVersion = "UNKNOWN_VERSION";
        }

        //        final PhoneHomeRequestBody.Builder phoneHomeRequestBodyBuilder = new PhoneHomeRequestBody.Builder();
        //        phoneHomeRequestBodyBuilder.addToMetaData("artifactory.version", thirdPartyVersion);
        //        phoneHomeRequestBodyBuilder.addToMetaData("blackduck.artifactory.plugin", pluginName);
        //        final PhoneHomeCallable phoneHomeCallable = hubServicesFactory.createBlackDuckPhoneHomeCallable(blackDuckArtifactoryConfig.getHubServerConfig().getHubUrl(), "blackduck-artifactory", pluginVersion, phoneHomeRequestBodyBuilder);
        //        final PhoneHomeService phoneHomeService = hubServicesFactory.createPhoneHomeService(Executors.newSingleThreadExecutor());
        //        phoneHomeService.phoneHome(phoneHomeCallable);
        // TODO: Re-enable and verify phone home works after upgrade to hub-common:37.*
    }

    public void importBomFile(final File bdioFile) throws IntegrationException {
        final CodeLocationService codeLocationService = hubServicesFactory.createCodeLocationService();
        codeLocationService.importBomFile(bdioFile);
    }

    public void addComponentToProjectVersion(final ExternalId componentExternalId, final String projectName, final String projectVersionName) throws IntegrationException {
        final ProjectService projectService = hubServicesFactory.createProjectService();
        projectService.addComponentToProjectVersion(componentExternalId, projectName, projectVersionName);
    }

    // TODO: Check if project exists before attempting to get the policy
    private VersionBomPolicyStatusView getVersionBomPolicyStatus(final String projectName, final String projectVersion) throws IntegrationException {
        final ProjectService projectService = hubServicesFactory.createProjectService();
        final ProjectVersionWrapper projectVersionWrapper = projectService.getProjectVersion(projectName, projectVersion);

        return projectService.getPolicyStatusForVersion(projectVersionWrapper.getProjectVersionView());
    }

    private String getProjectVersionUIUrlFromView(final ProjectVersionView projectVersionView) throws EncryptionException {
        final HubService hubService = hubServicesFactory.createHubService();
        return hubService.getFirstLinkSafely(projectVersionView, "components");
    }

    public void populatePolicyStatuses(final Set<RepoPath> repoPaths) {
        final HubService hubService = hubServicesFactory.createHubService();
        final ProjectService projectService = hubServicesFactory.createProjectService();
        boolean problemRetrievingPolicyStatus = false;

        logger.info(String.format("Attempting to update policy status of %d repoPaths", repoPaths.size()));
        for (final RepoPath repoPath : repoPaths) {
            final Optional<NameVersion> nameVersion = getProjectNameVersion(hubService, repoPath);

            if (nameVersion.isPresent()) {
                updateProjectUIUrl(nameVersion.get().getName(), nameVersion.get().getVersion(), projectService, repoPath);
                problemRetrievingPolicyStatus = !setPolicyStatusProperties(repoPath, nameVersion.get().getName(), nameVersion.get().getVersion());
            } else {
                logger.debug(
                    String.format("Properties %s and %s were not found on %s. Cannot update policy",
                        BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME.getName(),
                        BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME.getName(),
                        repoPath.getPath()
                    )
                );
            }
        }

        if (problemRetrievingPolicyStatus) {
            logger.warn("There was a problem retrieving policy status for one or more repos. This is expected if you do not have policy management.");
        }
    }

    private boolean setPolicyStatusProperties(final RepoPath repoPath, final String projectName, final String projectVersionName) {
        boolean success = false;

        try {
            final VersionBomPolicyStatusView versionBomPolicyStatusView = getVersionBomPolicyStatus(projectName, projectVersionName);
            logger.debug(String.format("Policy status json for %s is: %s", repoPath.toPath(), versionBomPolicyStatusView.json));
            final PolicyStatusDescription policyStatusDescription = new PolicyStatusDescription(versionBomPolicyStatusView);
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS, policyStatusDescription.getPolicyStatusMessage());
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS, versionBomPolicyStatusView.overallStatus.toString());
            logger.info(String.format("Updated policy status of %s: %s", repoPath.getName(), repoPath.toPath()));
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.UPDATE_STATUS, "UP TO DATE");
            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.LAST_UPDATE, dateTimeManager.getStringFromDate(new Date()));
            phoneHome();
            success = true;
        } catch (final IntegrationException e) {
            logger.debug(String.format("An error occurred while attempting to update policy status on %s", repoPath.getPath()), e);
            final Optional<String> policyStatus = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS);
            final Optional<String> overallPolicyStatus = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS);
            if (policyStatus.isPresent() || overallPolicyStatus.isPresent()) {
                artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.UPDATE_STATUS, "OUT OF DATE");
            }
        }

        return success;
    }

    private void updateProjectUIUrl(final String projectName, final String projectVersionName, final ProjectService projectService, final RepoPath repoPath) {
        try {
            final ProjectVersionWrapper projectVersionWrapper = projectService.getProjectVersion(projectName, projectVersionName);
            final String projectVersionUIUrl = getProjectVersionUIUrlFromView(projectVersionWrapper.getProjectVersionView());

            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_UI_URL, projectVersionUIUrl);
        } catch (final IntegrationException e) {
            logger.debug(String.format("Failed to update property %s on %s", BlackDuckArtifactoryProperty.PROJECT_VERSION_UI_URL.getName(), repoPath.toPath()), e);
        }
    }

    // TODO: Replace instances of this with ArtifactoryPropertyService::getProjectNameVersion once BlackDuckArtifactoryProperty.PROJECT_VERSION_URL has been removed
    private Optional<NameVersion> getProjectNameVersion(final HubService hubService, final RepoPath repoPath) {
        final Optional<String> apiUrl = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_URL);
        Optional<NameVersion> nameVersion = artifactoryPropertyService.getProjectNameVersion(repoPath);

        if (!nameVersion.isPresent() && apiUrl.isPresent()) {
            try {
                final ProjectVersionView projectVersionView = hubService.getResponse(apiUrl.get(), ProjectVersionView.class);
                final ProjectView projectView = hubService.getResponse(projectVersionView, ProjectVersionView.PROJECT_LINK_RESPONSE);
                final NameVersion projectNameVersion = new NameVersion(projectView.name, projectVersionView.versionName);

                // TODO: Move to a DeprecationService class
                artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_NAME, projectNameVersion.getName());
                artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.BLACKDUCK_PROJECT_VERSION_NAME, projectNameVersion.getVersion());

                nameVersion = Optional.of(projectNameVersion);
            } catch (final IntegrationException e) {
                logger.error(String.format("Failed to get project name and version from url: %s", apiUrl.get()));
                logger.debug(e.getMessage(), e);
            }
        }

        return nameVersion;
    }

    public HubServicesFactory getHubServicesFactory() {
        return hubServicesFactory;
    }
}
