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
        this.properties = properties;
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
}
