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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomPolicyStatusView;
import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.exception.HubIntegrationException;
import com.synopsys.integration.blackduck.rest.BlackduckRestConnection;
import com.synopsys.integration.blackduck.service.CodeLocationService;
import com.synopsys.integration.blackduck.service.ComponentService;
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
    private final Logger logger = LoggerFactory.getLogger(BlackDuckConnectionService.class);

    private final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final DateTimeManager dateTimeManager;

    public BlackDuckConnectionService(final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig, final ArtifactoryPropertyService artifactoryPropertyService,
        final DateTimeManager dateTimeManager) {
        this.blackDuckArtifactoryConfig = blackDuckArtifactoryConfig;
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.dateTimeManager = dateTimeManager;
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

    public void phoneHome(String pluginVersion, String thirdPartyVersion, final String pluginName) throws EncryptionException {
        final HubServicesFactory hubServicesFactory = createHubServicesFactory();
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
        final HubServicesFactory hubServicesFactory = createHubServicesFactory();
        final CodeLocationService codeLocationService = hubServicesFactory.createCodeLocationService();
        codeLocationService.importBomFile(bdioFile);
    }

    public void addComponentToProjectVersion(final ExternalId componentExternalId, final String projectName, final String projectVersionName) throws IntegrationException {
        final HubServicesFactory hubServicesFactory = createHubServicesFactory();
        final ProjectService projectService = hubServicesFactory.createProjectService();
        projectService.addComponentToProjectVersion(componentExternalId, projectName, projectVersionName);
    }

    public VersionBomPolicyStatusView getPolicyStatusOfProjectVersion(final String projectVersionUrl) throws IntegrationException {
        final HubServicesFactory hubServicesFactory = createHubServicesFactory();
        final HubService hubService = hubServicesFactory.createHubService();
        final ProjectVersionView projectVersionView = hubService.getResponse(projectVersionUrl, ProjectVersionView.class);
        final String policyStatusUrl = hubService.getFirstLink(projectVersionView, ProjectVersionView.POLICY_STATUS_LINK);
        logger.info("Looking up policy status: " + policyStatusUrl);
        return hubService.getResponse(policyStatusUrl, VersionBomPolicyStatusView.class);
    }

    public String getProjectVersionUrlFromView(final ProjectVersionView projectVersionView) throws HubIntegrationException, EncryptionException {
        final HubServicesFactory hubServicesFactory = createHubServicesFactory();
        final HubService hubService = hubServicesFactory.createHubService();
        return hubService.getHref(projectVersionView);
    }

    public String getProjectVersionUIUrlFromView(final ProjectVersionView projectVersionView) throws EncryptionException {
        final HubServicesFactory hubServicesFactory = createHubServicesFactory();
        final HubService hubService = hubServicesFactory.createHubService();
        return hubService.getFirstLinkSafely(projectVersionView, "components");
    }

    public HubServicesFactory createHubServicesFactory() throws EncryptionException {
        final HubServerConfig hubServerConfig = blackDuckArtifactoryConfig.getHubServerConfig();
        final BlackduckRestConnection restConnection;
        final IntLogger intlogger = new Slf4jIntLogger(logger);

        if (StringUtils.isNotBlank(blackDuckArtifactoryConfig.getProperty(BlackDuckProperty.API_TOKEN))) {
            restConnection = hubServerConfig.createApiTokenRestConnection(intlogger);
        } else {
            restConnection = hubServerConfig.createCredentialsRestConnection(intlogger);
        }

        return new HubServicesFactory(HubServicesFactory.createDefaultGson(), HubServicesFactory.createDefaultJsonParser(), restConnection, intlogger);
    }

    private VersionBomPolicyStatusView getVersionBomPolicyStatus(final NameVersion nameVersion) throws IntegrationException {
        final IntLogger slf4jLogger = new Slf4jIntLogger(logger);
        final HubServicesFactory hubServicesFactory = createHubServicesFactory();
        final HubService hubService = hubServicesFactory.createHubService();
        final ComponentService componentService = new ComponentService(hubService, slf4jLogger);
        final ProjectService projectService = new ProjectService(hubService, slf4jLogger, componentService);
        logger.info(String.format("Name [%s] Version [%s]", nameVersion.getName(), nameVersion.getVersion()));
        final ProjectVersionWrapper projectVersionWrapper = projectService.getProjectVersion(nameVersion.getName(), nameVersion.getVersion());
        logger.info(String.format("ProjectVersionWrapper is null: %s", String.valueOf(projectVersionWrapper == null)));
        final VersionBomPolicyStatusView versionBomPolicyStatusView = projectService.getPolicyStatusForProjectAndVersion(nameVersion.getName(), nameVersion.getVersion());

        return versionBomPolicyStatusView;
    }

    public void populatePolicyStatuses(final Set<RepoPath> repoPaths) {
        boolean problemRetrievingPolicyStatus = false;

        for (final RepoPath repoPath : repoPaths) {
            final Optional<NameVersion> nameVersion = artifactoryPropertyService.getProjectNameVersion(repoPath);
            if (nameVersion.isPresent()) {
                updateUrlPropertyToCurrentHubServer(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_UI_URL);
                try {
                    final VersionBomPolicyStatusView versionBomPolicyStatusView = getVersionBomPolicyStatus(nameVersion.get());
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
                    final String policyStatus = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS);
                    final String overallPolicyStatus = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS);
                    if (StringUtils.isNotBlank(policyStatus) || StringUtils.isNotBlank(overallPolicyStatus)) {
                        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.UPDATE_STATUS, "OUT OF DATE");
                    }
                }
            }
        }

        if (problemRetrievingPolicyStatus) {
            logger.warn("There was a problem retrieving policy status for one or more repos. This is expected if you do not have policy management.");
        }
    }

    /**
     * If the hub server being used changes, the existing properties in artifactory could be inaccurate so we will update them when they differ from the hub url established in the properties file.
     */
    // TODO: Test with '/' at the end of hubUrl
    private void updateUrlPropertyToCurrentHubServer(final RepoPath repoPath, final BlackDuckArtifactoryProperty urlProperty) {
        try {
            final String currentUrl = artifactoryPropertyService.getProperty(repoPath, urlProperty);
            final String hubUrl = blackDuckArtifactoryConfig.getProperty(BlackDuckProperty.URL);
            if (StringUtils.isBlank(currentUrl) || currentUrl.startsWith(hubUrl)) {
                return;
            }

            final URL urlFromProperty = new URL(currentUrl);
            final URL updatedPropertyUrl = new URL(hubUrl + urlFromProperty.getPath());

            logger.info(String.format("Updating property %s with a new URL", urlProperty.getName()));
            artifactoryPropertyService.setProperty(repoPath, urlProperty, updatedPropertyUrl.toString());
        } catch (final MalformedURLException e) {
            logger.info(String.format("Failed to update property %s on repo path %s", urlProperty.getName(), repoPath.getPath()));
            logger.debug(e.getMessage(), e);
        }
    }
}
