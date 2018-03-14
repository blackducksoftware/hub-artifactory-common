/**
 * hub-artifactory-common
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.hub.artifactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.blackducksoftware.integration.hub.configuration.HubServerConfig;
import com.blackducksoftware.integration.hub.configuration.HubServerConfigBuilder;

public class BlackDuckArtifactoryConfig {
    private File homeDirectory;
    private File etcDirectory;
    private File pluginsDirectory;
    private File pluginsLibDirectory;
    private File blackDuckDirectory;
    private File versionFile;
    private Properties properties;
    private HubServerConfig hubServerConfig;

    public void loadProperties(final String propertiesFilePath) throws IOException {
        loadProperties(new File(propertiesFilePath));
    }

    public void loadProperties(final File propertiesFile) throws IOException {
        properties = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream(propertiesFile)) {
            properties.load(fileInputStream);
        }

        final HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder();
        hubServerConfigBuilder.setFromProperties(properties);
        hubServerConfig = hubServerConfigBuilder.build();
    }

    public Properties getProperties() {
        return properties;
    }

    public String getProperty(final PluginProperty property) {
        return properties.getProperty(property.getKey());
    }

    public Object setProperty(final PluginProperty property, final String value) {
        return properties.setProperty(property.getKey(), value);
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
