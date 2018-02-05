package com.blackducksoftware.integration.hub.artifactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder;
import com.blackducksoftware.integration.hub.global.HubServerConfig;

public class BlackDuckArtifactoryConfig {
    private File homeDirectory;
    private File etcDirectory;
    private File pluginsDirectory;
    private File pluginsLibDirectory;
    private File blackDuckDirectory;
    private File versionFile;
    private Properties properties;
    private HubServerConfig hubServerConfig;

    public void loadProperties(final String propertiesFilePath) throws FileNotFoundException, IOException {
        loadProperties(new File(propertiesFilePath));
    }

    public void loadProperties(final File propertiesFile) throws FileNotFoundException, IOException {
        properties.load(new FileInputStream(propertiesFile));

        final HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder();
        hubServerConfigBuilder.setFromProperties(properties);
        hubServerConfig = hubServerConfigBuilder.build();
    }

    public Properties getProperties() {
        return properties;
    }

    public HubServerConfig getHubServerConfig() {
        return hubServerConfig;
    }

    public void setBlackDuckDirectory(final String blackDuckDirectoryPath) {
        this.blackDuckDirectory = new File(blackDuckDirectoryPath);
    }

    public File getBlackDuckDirectory() {
        return blackDuckDirectory;
    }

    public void setPluginsDirectory(final String pluginsDirectoryPath) {
        this.pluginsDirectory = new File(pluginsDirectoryPath);
        this.pluginsLibDirectory = new File(pluginsDirectory, "lib");
        this.versionFile = new File(pluginsLibDirectory, "version.txt");
    }

    public File getPluginsDirectory() {
        return pluginsDirectory;
    }

    public File getPluginsLibDirectory() {
        return pluginsLibDirectory;
    }

    public void setEtcDirectory(final String etcDirectoryPath) {
        this.etcDirectory = new File(etcDirectoryPath);
    }

    public File getEtcDirectory() {
        return etcDirectory;
    }

    public void setHomeDirectory(final String homeDirectoryPath) {
        this.homeDirectory = new File(homeDirectoryPath);
    }

    public File getHomeDirectory() {
        return homeDirectory;
    }

    public File getVersionFile() {
        return versionFile;
    }

}
