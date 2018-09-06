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
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.hub.configuration.HubServerConfig;
import com.blackducksoftware.integration.hub.configuration.HubServerConfigBuilder;

public class BlackDuckArtifactoryConfig {
    private final Logger logger = LoggerFactory.getLogger(BlackDuckArtifactoryConfig.class);

    private File homeDirectory;
    private File etcDirectory;
    private File pluginsDirectory;
    private File pluginsLibDirectory;
    private File blackDuckDirectory;
    private File versionFile;
    private String thirdPartyVersion;
    private String pluginName;
    private Properties properties;

    private HubServerConfig hubServerConfig;

    public void loadProperties(final String propertiesFilePathOverride) throws IOException {
        final File propertiesFile;
        if (StringUtils.isNotBlank(propertiesFilePathOverride)) {
            propertiesFile = new File(propertiesFilePathOverride);
        } else {
            propertiesFile = new File(getPluginsLibDirectory(), getDefaultPropertiesFileName());
        }

        try {
            loadProperties(propertiesFile);
        } catch (final Exception e) {
            logger.error(String.format("A Black Duck plugin encountered an unexpected error when trying to load its properties file at %s", propertiesFile.getAbsolutePath()), e);
            throw (e);
        }
    }

    public void loadProperties(final File propertiesFile) throws IOException {
        properties = new Properties();
        try (final FileInputStream fileInputStream = new FileInputStream(propertiesFile)) {
            properties.load(fileInputStream);
        }
    }

    public void setupHubServerConfig() {
        final HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder();
        hubServerConfigBuilder.setFromProperties(properties);
        hubServerConfig = hubServerConfigBuilder.build();
    }

    public List<String> getRepositoryKeysFromProperties(final ConfigurationProperty repositoryKeyListProperty, final ConfigurationProperty repositoryKeyCsvProperty) throws IOException {
        final List<String> repositoryKeys;

        final String repositoryKeyListString = getProperty(repositoryKeyListProperty);
        final String repositoryKeyCsvPath = getProperty(repositoryKeyCsvProperty);
        final File repositoryKeyCsvFile = new File(repositoryKeyCsvPath);

        if (repositoryKeyCsvFile.isFile()) {
            repositoryKeys = Files.readAllLines(repositoryKeyCsvFile.toPath()).stream()
                             .map(line -> line.split(","))
                             .flatMap(Arrays::stream)
                             .filter(StringUtils::isNotBlank)
                             .collect(Collectors.toList());
        } else {
            repositoryKeys = Arrays.asList(repositoryKeyListString.split(","));
        }

        return repositoryKeys;
    }

    public Properties getProperties() {
        return properties;
    }

    public String getProperty(final ConfigurationProperty property) {
        return properties.getProperty(property.getKey());
    }

    public Object setProperty(final ConfigurationProperty property, final String value) {
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

    public String getThirdPartyVersion() { return thirdPartyVersion; }

    public void setThirdPartyVersion(final String thirdPartyVersion) { this.thirdPartyVersion = thirdPartyVersion; }

    public String getPluginName() { return pluginName; }

    public void setPluginName(final String pluginName) { this.pluginName = pluginName; }

    public String getDefaultPropertiesFileName() { return String.format("%s.properties", pluginName); }
}
