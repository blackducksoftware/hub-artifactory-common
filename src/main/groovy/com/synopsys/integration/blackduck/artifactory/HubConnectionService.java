package com.synopsys.integration.blackduck.artifactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.RepoPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.api.generated.component.ProjectRequest;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomPolicyStatusView;
import com.synopsys.integration.blackduck.cli.summary.ScanServiceOutput;
import com.synopsys.integration.blackduck.configuration.HubScanConfig;
import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.exception.HubIntegrationException;
import com.synopsys.integration.blackduck.rest.BlackduckRestConnection;
import com.synopsys.integration.blackduck.service.CodeLocationService;
import com.synopsys.integration.blackduck.service.HubService;
import com.synopsys.integration.blackduck.service.HubServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.SignatureScannerService;
import com.synopsys.integration.blackduck.service.model.PolicyStatusDescription;
import com.synopsys.integration.blackduck.service.model.ProjectRequestBuilder;
import com.synopsys.integration.exception.EncryptionException;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.model.externalid.ExternalId;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.phonehome.PhoneHomeCallable;
import com.synopsys.integration.phonehome.PhoneHomeRequestBody;
import com.synopsys.integration.phonehome.PhoneHomeService;

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

        final PhoneHomeRequestBody.Builder phoneHomeRequestBodyBuilder = new PhoneHomeRequestBody.Builder();
        phoneHomeRequestBodyBuilder.addToMetaData("artifactory.version", thirdPartyVersion);
        phoneHomeRequestBodyBuilder.addToMetaData("hub.artifactory.plugin", pluginName);
        final PhoneHomeCallable phoneHomeCallable = hubServicesFactory.createBlackDuckPhoneHomeCallable(blackDuckArtifactoryConfig.getHubServerConfig().getHubUrl(), "hub-artifactory", pluginVersion, phoneHomeRequestBodyBuilder);
        final PhoneHomeService phoneHomeService = hubServicesFactory.createPhoneHomeService(Executors.newSingleThreadExecutor());
        phoneHomeService.phoneHome(phoneHomeCallable);
        // TODO: Verify phone home works
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
    // TODO: This does not update the blackduck.uiUrl property
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
