package com.blackducksoftware.integration.hub.artifactory;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
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
import com.blackducksoftware.integration.hub.service.model.ProjectRequestBuilder;
import com.blackducksoftware.integration.log.IntLogger;
import com.blackducksoftware.integration.log.Slf4jIntLogger;
import com.blackducksoftware.integration.phonehome.PhoneHomeRequestBody;

public class HubConnectionService {
    private final Logger logger = LoggerFactory.getLogger(HubConnectionService.class);

    private final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig;

    public HubConnectionService(final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig) {
        this.blackDuckArtifactoryConfig = blackDuckArtifactoryConfig;
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
        final SignatureScannerService signatureScannerService = hubServicesFactory.createSignatureScannerService(null);

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

}
