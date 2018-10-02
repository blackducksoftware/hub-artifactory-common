package com.synopsys.integration.blackduck.artifactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPath;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
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

            phoneHome(pluginVersion, blackDuckArtifactoryConfig.getThirdPartyVersion(), blackDuckArtifactoryConfig.getPluginName());
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
        boolean problemRetrievingPolicyStatus = false;

        for (final RepoPath repoPath : repoPaths) {
            final Optional<NameVersion> nameVersion = artifactoryPropertyService.getProjectNameVersion(repoPath);
            if (nameVersion.isPresent()) {
                updateProjectUIUrl(repoPath);
                try {
                    final VersionBomPolicyStatusView versionBomPolicyStatusView = getVersionBomPolicyStatus(nameVersion.get().getName(), nameVersion.get().getVersion());
                    logger.info("policy status json: " + versionBomPolicyStatusView.json);
                    final PolicyStatusDescription policyStatusDescription = new PolicyStatusDescription(versionBomPolicyStatusView);
                    artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS, policyStatusDescription.getPolicyStatusMessage());
                    artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS, versionBomPolicyStatusView.overallStatus.toString());
                    logger.info(String.format("Updated policy status of %s", repoPath.getName()));
                    artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.UPDATE_STATUS, "UP TO DATE");
                    artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.LAST_UPDATE, dateTimeManager.getStringFromDate(new Date()));
                    phoneHome();
                } catch (final IntegrationException e) {
                    logger.debug(String.format("An error occurred while attempting to update policy status on %s", repoPath.getPath()), e);
                    problemRetrievingPolicyStatus = true;
                    final Optional<String> policyStatus = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS);
                    final Optional<String> overallPolicyStatus = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS);
                    if (policyStatus.isPresent() || overallPolicyStatus.isPresent()) {
                        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.UPDATE_STATUS, "OUT OF DATE");
                    }
                }
            }
        }

        if (problemRetrievingPolicyStatus) {
            logger.warn("There was a problem retrieving policy status for one or more repos. This is expected if you do not have policy management.");
        }
    }

    private void updateProjectUIUrl(final RepoPath repoPath) {
        final Optional<String> projectUIUrlProperty = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_UI_URL);
        final Optional<NameVersion> projectNameVersion = artifactoryPropertyService.getProjectNameVersion(repoPath);

        if (projectUIUrlProperty.isPresent()) {
            updateUIUrlPropertyToCurrentHubServer(repoPath);
        } else if (projectNameVersion.isPresent()) {
            try {
                final ProjectService projectService = hubServicesFactory.createProjectService();
                final ProjectVersionWrapper projectVersionWrapper = projectService.getProjectVersion(projectNameVersion.get().getName(), projectNameVersion.get().getVersion());
                final String projectVersionUIUrl = getProjectVersionUIUrlFromView(projectVersionWrapper.getProjectVersionView());

                artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_UI_URL, projectVersionUIUrl);
            } catch (final IntegrationException ignore) {

            }
        }
    }

    /**
     * If the hub server being used changes, the existing properties in artifactory could be inaccurate so we will update them when they differ from the hub url established in the properties file.
     */
    // TODO: Test with '/' at the end of hubUrl
    private void updateUIUrlPropertyToCurrentHubServer(final RepoPath repoPath) {
        final BlackDuckArtifactoryProperty urlProperty = BlackDuckArtifactoryProperty.PROJECT_VERSION_UI_URL;
        try {
            final Optional<String> currentUrl = artifactoryPropertyService.getProperty(repoPath, urlProperty);
            final String hubUrl = blackDuckArtifactoryConfig.getProperty(BlackDuckProperty.URL);

            if (currentUrl.map(url -> url.startsWith(hubUrl)).isPresent()) {
                return;
            }

            final URL urlFromProperty = new URL(currentUrl.get());
            final URL updatedPropertyUrl = new URL(hubUrl + urlFromProperty.getPath());

            logger.info(String.format("Updating property %s with a new URL", urlProperty.getName()));
            artifactoryPropertyService.setProperty(repoPath, urlProperty, updatedPropertyUrl.toString());
        } catch (final MalformedURLException e) {
            logger.info(String.format("Failed to update property %s on repo path %s", urlProperty.getName(), repoPath.getPath()));
            logger.debug(e.getMessage(), e);
        }
    }

    public HubServicesFactory getHubServicesFactory() {
        return hubServicesFactory;
    }
}
