package com.synopsys.integration.blackduck.artifactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class BlackDuckPropertyManager {
    final Properties properties;

    public BlackDuckPropertyManager(final Properties properties) {
        this.properties = convertPropertiesToOldPrefix(BlackDuckProperty.values(), properties);
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

    public String getProperty(final ConfigurationProperty property) {
        String propertyValue = properties.getProperty(property.getKey());
        if (propertyValue == null) {
            propertyValue = properties.getProperty(property.getOldKey());
        }

        return propertyValue;
    }

    /**
     * Converts blackDuckProperties from the new prefix (blackduck.*) to the old prefix (blackduck.hub.*)
     * for use with the HubServerConfigBuilder in hub-common:8.3.1
     *
     * to exclude the term "hub"
     * @param configurationProperties the ConfigurationProperties values
     * @param properties              the blackDuckProperties
     * @return a copy of the blackDuckProperties with keys converted
     */
    // TODO: This should be removed once HubServerConfigBuilder in hub-common changes HUB_SERVER_CONFIG_ENVIRONMENT_VARIABLE_PREFIX and HUB_SERVER_CONFIG_PROPERTY_KEY_PREFIX
    private Properties convertPropertiesToOldPrefix(final ConfigurationProperty[] configurationProperties, final Properties properties) {
        final Properties newProperties = new Properties();
        newProperties.putAll(properties);

        for (final ConfigurationProperty configurationProperty : configurationProperties) {
            final String currentKey = configurationProperty.getKey();
            final String oldKey = configurationProperty.getOldKey();
            if (newProperties.containsKey(currentKey) && StringUtils.isNotBlank(oldKey)) {
                final Object propertyValue = newProperties.remove(currentKey);
                newProperties.put(oldKey, propertyValue);
            }
        }

        return newProperties;
    }
}
