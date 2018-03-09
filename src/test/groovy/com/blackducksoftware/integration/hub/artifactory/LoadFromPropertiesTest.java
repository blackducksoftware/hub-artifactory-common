package com.blackducksoftware.integration.hub.artifactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.junit.Test;

import com.blackducksoftware.integration.hub.configuration.HubServerConfigBuilder;

public class LoadFromPropertiesTest {
    @Test
    public void loadFromProperties() throws FileNotFoundException, IOException {
        final Properties properties = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream("src/test/resources/blackDuckCacheInspector.properties")) {
            properties.load(fileInputStream);
        }

        final HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder();
        hubServerConfigBuilder.setFromProperties(properties);
    }
}
