package com.synopsys.integration.blackduck.artifactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.artifactory.repo.Repositories;
import org.artifactory.search.Searches;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.analytics.AnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.analytics.AnalyticsModule;
import com.synopsys.integration.blackduck.artifactory.inspect.ArtifactIdentificationService;
import com.synopsys.integration.blackduck.artifactory.inspect.ArtifactoryExternalIdFactory;
import com.synopsys.integration.blackduck.artifactory.inspect.CacheInspectorService;
import com.synopsys.integration.blackduck.artifactory.inspect.InspectionModule;
import com.synopsys.integration.blackduck.artifactory.inspect.InspectionModuleConfig;
import com.synopsys.integration.blackduck.artifactory.inspect.MetaDataPopulationService;
import com.synopsys.integration.blackduck.artifactory.inspect.MetaDataUpdateService;
import com.synopsys.integration.blackduck.artifactory.inspect.PackageTypePatternManager;
import com.synopsys.integration.blackduck.artifactory.inspect.metadata.ArtifactMetaDataService;
import com.synopsys.integration.blackduck.artifactory.policy.PolicyModule;
import com.synopsys.integration.blackduck.artifactory.policy.PolicyModuleConfig;
import com.synopsys.integration.blackduck.artifactory.scan.ArtifactScanService;
import com.synopsys.integration.blackduck.artifactory.scan.RepositoryIdentificationService;
import com.synopsys.integration.blackduck.artifactory.scan.ScanModule;
import com.synopsys.integration.blackduck.artifactory.scan.ScanModuleConfig;
import com.synopsys.integration.blackduck.artifactory.scan.ScanModuleProperty;
import com.synopsys.integration.blackduck.artifactory.scan.StatusCheckService;
import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.configuration.HubServerConfigBuilder;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.hub.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class PluginService {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));
    private final PluginConfig pluginConfig;
    private final Repositories repositories;
    private final Searches searches;

    private BlackDuckPropertyManager blackDuckPropertyManager;
    private HubServerConfig hubServerConfig;
    private File blackDuckDirectory;
    private DateTimeManager dateTimeManager;
    private ArtifactoryPropertyService artifactoryPropertyService;
    private BlackDuckConnectionService blackDuckConnectionService;

    public PluginService(final PluginConfig pluginConfig, final Repositories repositories, final Searches searches) {
        this.pluginConfig = pluginConfig;
        this.repositories = repositories;
        this.searches = searches;
    }

    public ModuleManager initializePlugin() throws IOException, IntegrationException {
        logger.info("initializing blackDuckPlugin...");

        final File propertiesFile = getPropertiesFile();
        final Properties unprocessedProperties = loadPropertiesFromFile(propertiesFile);
        blackDuckPropertyManager = new BlackDuckPropertyManager(unprocessedProperties);

        final HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder();
        hubServerConfigBuilder.setFromProperties(blackDuckPropertyManager.properties);
        hubServerConfig = hubServerConfigBuilder.build();

        this.blackDuckDirectory = setUpBlackDuckDirectory();

        dateTimeManager = new DateTimeManager(blackDuckPropertyManager.getProperty(BlackDuckProperty.DATE_TIME_PATTERN));
        artifactoryPropertyService = new ArtifactoryPropertyService(repositories, searches, dateTimeManager);
        blackDuckConnectionService = new BlackDuckConnectionService(pluginConfig, artifactoryPropertyService, dateTimeManager, hubServerConfig);

        final ScanModule scanModule = createScanModule();
        final InspectionModule inspectionModule = createInspectionModule();
        final PolicyModule policyModule = createPolicyModule();
        final AnalyticsModule analyticsModule = createAnalyticsModule();
        final ModuleManager moduleManager = new ModuleManager(scanModule, inspectionModule, policyModule, analyticsModule);

        analyticsModule.registerModules(scanModule, inspectionModule, policyModule);

        logger.info("...blackDuckPlugin initialized.");
        return moduleManager;
    }

    public void reloadBlackDuckDirectory(final TriggerType triggerType) throws IOException, IntegrationException {
        LogUtil.start(logger, "blackDuckReloadDirectory", triggerType);

        FileUtils.deleteDirectory(determineBlackDuckDirectory());
        this.blackDuckDirectory = setUpBlackDuckDirectory();

        LogUtil.finish(logger, "blackDuckReloadDirectory", triggerType);
    }

    private ScanModule createScanModule() {
        final ScanModuleConfig scanModuleConfig = new ScanModuleConfig(blackDuckPropertyManager);
        scanModuleConfig.setUpCliDuckDirectory(blackDuckDirectory);
        final RepositoryIdentificationService repositoryIdentificationService = new RepositoryIdentificationService(blackDuckPropertyManager, dateTimeManager, repositories, searches);
        final ArtifactScanService artifactScanService = new ArtifactScanService(scanModuleConfig, hubServerConfig, blackDuckDirectory, blackDuckPropertyManager, repositoryIdentificationService,
            blackDuckConnectionService, artifactoryPropertyService, repositories, dateTimeManager);
        final StatusCheckService statusCheckService = new StatusCheckService(scanModuleConfig, blackDuckConnectionService, repositoryIdentificationService, dateTimeManager);
        final AnalyticsCollector analyticsCollector = new AnalyticsCollector(ScanModule.class);
        final ScanModule scanModule = new ScanModule(scanModuleConfig, repositoryIdentificationService, artifactScanService, artifactoryPropertyService, blackDuckConnectionService, statusCheckService, analyticsCollector);

        logger.info(String.format("Module [%s] created", scanModule.getClass().getSimpleName()));
        return scanModule;
    }

    private InspectionModule createInspectionModule() throws IOException {
        final CacheInspectorService cacheInspectorService = new CacheInspectorService(blackDuckPropertyManager, repositories, artifactoryPropertyService);
        final List<String> repoKeys = cacheInspectorService.getRepositoriesToInspect();
        final InspectionModuleConfig inspectionModuleConfig = new InspectionModuleConfig(blackDuckPropertyManager, repoKeys);
        final PackageTypePatternManager packageTypePatternManager = new PackageTypePatternManager();
        packageTypePatternManager.loadPatterns(blackDuckPropertyManager);
        final ExternalIdFactory externalIdFactory = new ExternalIdFactory();
        final ArtifactoryExternalIdFactory artifactoryExternalIdFactory = new ArtifactoryExternalIdFactory(externalIdFactory);
        final ArtifactIdentificationService artifactIdentificationService = new ArtifactIdentificationService(repositories, searches, packageTypePatternManager,
            artifactoryExternalIdFactory, artifactoryPropertyService, cacheInspectorService, blackDuckConnectionService);
        final ArtifactMetaDataService artifactMetaDataService = new ArtifactMetaDataService(blackDuckConnectionService);
        final MetaDataPopulationService metaDataPopulationService = new MetaDataPopulationService(artifactoryPropertyService, cacheInspectorService, artifactMetaDataService);
        final MetaDataUpdateService metaDataUpdateService = new MetaDataUpdateService(artifactoryPropertyService, cacheInspectorService, artifactMetaDataService, metaDataPopulationService);
        final AnalyticsCollector analyticsCollector = new AnalyticsCollector(InspectionModule.class);
        final InspectionModule inspectionModule = new InspectionModule(inspectionModuleConfig, artifactIdentificationService, metaDataPopulationService, metaDataUpdateService, artifactoryPropertyService, repositories, analyticsCollector);

        logger.info(String.format("Module [%s] created", inspectionModule.getClass().getSimpleName()));
        return inspectionModule;
    }

    private PolicyModule createPolicyModule() {
        final PolicyModuleConfig policyModuleConfig = new PolicyModuleConfig(blackDuckPropertyManager);
        final AnalyticsCollector analyticsCollector = new AnalyticsCollector(PolicyModule.class);
        final PolicyModule policyModule = new PolicyModule(policyModuleConfig, artifactoryPropertyService, analyticsCollector);

        logger.info(String.format("Module [%s] created", policyModule.getClass().getSimpleName()));
        return policyModule;
    }

    private AnalyticsModule createAnalyticsModule() {
        final AnalyticsCollector analyticsCollector = new AnalyticsCollector(AnalyticsModule.class);
        final AnalyticsModule analyticsModule = new AnalyticsModule(blackDuckConnectionService, analyticsCollector);

        logger.info(String.format("Module [%s] created", analyticsModule.getClass().getSimpleName()));
        return analyticsModule;
    }

    private File setUpBlackDuckDirectory() throws IOException, IntegrationException {
        try {
            final File blackDuckDirectory = determineBlackDuckDirectory();

            if (!blackDuckDirectory.exists() && !blackDuckDirectory.mkdir()) {
                throw new IntegrationException(String.format("Failed to create the BlackDuck directory: %s", blackDuckDirectory.getCanonicalPath()));
            }

            return blackDuckDirectory;
        } catch (final IOException | IntegrationException e) {
            logger.error(String.format("Exception while setting up the Black Duck directory %s", blackDuckDirectory), e);
            throw e;
        }
    }

    private File determineBlackDuckDirectory() {
        final File blackDuckDirectory;
        final String scanBinariesDirectory = blackDuckPropertyManager.getProperty(ScanModuleProperty.BINARIES_DIRECTORY_PATH);
        if (StringUtils.isNotEmpty(scanBinariesDirectory)) {
            blackDuckDirectory = new File(pluginConfig.getHomeDirectory(), scanBinariesDirectory);
        } else {
            blackDuckDirectory = new File(pluginConfig.getEtcDirectory(), "blackducksoftware");
        }

        return blackDuckDirectory;
    }

    private File getPropertiesFile() {
        final String propertiesFilePathOverride = pluginConfig.getPropertiesFilePathOverride();
        final File propertiesFile;

        if (StringUtils.isNotBlank(propertiesFilePathOverride)) {
            propertiesFile = new File(propertiesFilePathOverride);
        } else {
            propertiesFile = new File(pluginConfig.getPluginsLibDirectory(), "blackDuckPlugin.properties");
        }

        return propertiesFile;
    }

    private Properties loadPropertiesFromFile(final File propertiesFile) throws IOException {
        final Properties properties = new Properties();
        try (final FileInputStream fileInputStream = new FileInputStream(propertiesFile)) {
            properties.load(fileInputStream);
        }

        return properties;
    }
}
