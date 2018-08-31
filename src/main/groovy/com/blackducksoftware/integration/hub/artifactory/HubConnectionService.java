package com.blackducksoftware.integration.hub.artifactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.exception.EncryptionException;
import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.api.generated.component.ProjectRequest;
import com.blackducksoftware.integration.hub.api.generated.view.ProjectVersionView;
import com.blackducksoftware.integration.hub.api.generated.view.VersionBomPolicyStatusView;
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalId;
import com.blackducksoftware.integration.hub.cli.summary.ScanServiceOutput;
import com.blackducksoftware.integration.hub.configuration.HubScanConfig;
import com.blackducksoftware.integration.hub.configuration.HubServerConfig;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.rest.BlackduckRestConnection;
import com.blackducksoftware.integration.hub.service.CodeLocationService;
import com.blackducksoftware.integration.hub.service.HubService;
import com.blackducksoftware.integration.hub.service.HubServicesFactory;
import com.blackducksoftware.integration.hub.service.PhoneHomeService;
import com.blackducksoftware.integration.hub.service.ProjectService;
import com.blackducksoftware.integration.hub.service.SignatureScannerService;
import com.blackducksoftware.integration.hub.service.model.PolicyStatusDescription;
import com.blackducksoftware.integration.hub.service.model.ProjectRequestBuilder;
import com.blackducksoftware.integration.log.IntLogger;
import com.blackducksoftware.integration.log.Slf4jIntLogger;
import com.blackducksoftware.integration.phonehome.PhoneHomeRequestBody;

public class HubConnectionService {
    private final Logger logger = LoggerFactory.getLogger(HubConnectionService.class);

    private final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final DateTimeManager dateTimeManager;

    public HubConnectionService(final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig, final ArtifactoryPropertyService artifactoryPropertyService,
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

        final PhoneHomeService phoneHomeService = hubServicesFactory.createPhoneHomeService();
        final PhoneHomeRequestBody.Builder phoneHomeRequestBodyBuilder = phoneHomeService.createInitialPhoneHomeRequestBodyBuilder("hub-artifactory", pluginVersion);
        phoneHomeRequestBodyBuilder.addToMetaData("artifactory.version", thirdPartyVersion);
        phoneHomeRequestBodyBuilder.addToMetaData("hub.artifactory.plugin", pluginName);
        phoneHomeService.phoneHome(phoneHomeRequestBodyBuilder);
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

    public ScanServiceOutput performScan(final HubScanConfig hubScanConfig, final ProjectRequestBuilder projectRequestBuilder) throws InterruptedException, IntegrationException {
        final HubServicesFactory hubServicesFactory = createHubServicesFactory();
        final SignatureScannerService signatureScannerService = hubServicesFactory.createSignatureScannerService();

        final HubServerConfig hubServerConfig = blackDuckArtifactoryConfig.getHubServerConfig();
        final ProjectRequest projectRequest = projectRequestBuilder.build();

        return signatureScannerService.executeScans(hubServerConfig, hubScanConfig, projectRequest);
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

        if (StringUtils.isNotBlank(blackDuckArtifactoryConfig.getProperty(BlackDuckHubProperty.API_TOKEN))) {
            restConnection = hubServerConfig.createApiTokenRestConnection(intlogger);
        } else {
            restConnection = hubServerConfig.createCredentialsRestConnection(intlogger);
        }

        return new HubServicesFactory(HubServicesFactory.createDefaultGson(), HubServicesFactory.createDefaultJsonParser(), restConnection, intlogger);
    }

    public void populatePolicyStatuses(final Set<RepoPath> repoPaths) {
        boolean problemRetrievingPolicyStatus = false;
        for (final RepoPath repoPath : repoPaths) {
            try {
                String projectVersionUrl = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_URL);
                if (StringUtils.isNotBlank(projectVersionUrl)) {
                    projectVersionUrl = updateUrlPropertyToCurrentHubServer(projectVersionUrl);
                    artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.PROJECT_VERSION_URL, projectVersionUrl);
                    try {
                        final VersionBomPolicyStatusView versionBomPolicyStatusView = getPolicyStatusOfProjectVersion(projectVersionUrl);
                        logger.info("policy status json: " + versionBomPolicyStatusView.json);
                        final PolicyStatusDescription policyStatusDescription = new PolicyStatusDescription(versionBomPolicyStatusView);
                        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS, policyStatusDescription.getPolicyStatusMessage());
                        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS, versionBomPolicyStatusView.overallStatus.toString());
                        logger.info(String.format("Added policy status to %s", repoPath.getName()));
                        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.UPDATE_STATUS, "UP TO DATE");
                        artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.LAST_UPDATE, dateTimeManager.getStringFromDate(new Date()));
                        phoneHome();
                    } catch (final HubIntegrationException ignored) {
                        problemRetrievingPolicyStatus = true;
                        final String policyStatus = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS);
                        final String overallPolicyStatus = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.OVERALL_POLICY_STATUS);
                        if (StringUtils.isNotBlank(policyStatus) || StringUtils.isNotBlank(overallPolicyStatus)) {
                            artifactoryPropertyService.setProperty(repoPath, BlackDuckArtifactoryProperty.UPDATE_STATUS, "OUT OF DATE");
                        }
                    }
                }
            } catch (final Exception e) {
                logger.error(String.format("There was a problem trying to access repository %s: ", repoPath.getName()), e);
                problemRetrievingPolicyStatus = true;
            }
        }
        if (problemRetrievingPolicyStatus) {
            logger.warn("There was a problem retrieving policy status for one or more repos. This is expected if you do not have policy management.");
        }
    }

    /**
     * If the hub server being used changes, the existing properties in artifactory could be inaccurate so we will update them when they differ from the hub url established in the properties file.
     */
    // TODO: This does not update the backduck.uiUrl property
    private String updateUrlPropertyToCurrentHubServer(final String urlProperty) throws MalformedURLException {
        final String hubUrl = blackDuckArtifactoryConfig.getProperty(BlackDuckHubProperty.URL);
        if (urlProperty.startsWith(hubUrl)) {
            return urlProperty;
        }

        // Get the old hub url from the existing property
        final URL urlFromProperty = new URL(urlProperty);
        // TODO: Test with '/' at the end of hubUrl
        final URL updatedPropertyUrl = new URL(hubUrl + urlFromProperty.getPath());

        return updatedPropertyUrl.toString();
    }
}
