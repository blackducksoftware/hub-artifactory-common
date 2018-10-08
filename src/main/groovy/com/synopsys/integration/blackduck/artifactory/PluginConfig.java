package com.synopsys.integration.blackduck.artifactory;

import java.io.File;

public class PluginConfig {
    private final File homeDirectory;
    private final File etcDirectory;
    private final File pluginsDirectory;
    private final File pluginsLibDirectory;
    private final File versionFile;
    private final String thirdPartyVersion;
    private final String propertiesFilePathOverride;

    public PluginConfig(final File homeDirectory, final File etcDirectory, final File pluginsDirectory, final String thirdPartyVersion, final String propertiesFilePathOverride) {
        this.homeDirectory = homeDirectory;
        this.etcDirectory = etcDirectory;
        this.pluginsDirectory = pluginsDirectory;
        this.pluginsLibDirectory = new File(this.pluginsDirectory, "lib");
        this.versionFile = new File(this.pluginsLibDirectory, "version.txt");
        this.thirdPartyVersion = thirdPartyVersion;
        this.propertiesFilePathOverride = propertiesFilePathOverride;
    }

    public File getHomeDirectory() {
        return homeDirectory;
    }

    public File getEtcDirectory() {
        return etcDirectory;
    }

    public File getPluginsDirectory() {
        return pluginsDirectory;
    }

    public File getPluginsLibDirectory() {
        return pluginsLibDirectory;
    }

    public File getVersionFile() {
        return versionFile;
    }

    public String getThirdPartyVersion() {
        return thirdPartyVersion;
    }

    public String getPropertiesFilePathOverride() {
        return propertiesFilePathOverride;
    }
}
