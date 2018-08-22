package com.blackducksoftware.integration.hub.artifactory

import static org.junit.Assert.*

import org.artifactory.fs.FileLayoutInfo
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.blackducksoftware.integration.hub.artifactory.inspect.ArtifactoryExternalIdFactory
import com.blackducksoftware.integration.hub.artifactory.inspect.SupportedPackageType

public class ArtifactoryExternalIdFactoryTest {
    Logger testLogger = LoggerFactory.getLogger(ArtifactoryExternalIdFactoryTest.class);

    @Test
    public void createNugetExternalId() {
        final Map propertiesMap = [
            'nuget.id':'component',
            'nuget.version':'version'
        ];

        testNameVersionExternalIdCreation(SupportedPackageType.NUGET.getArtifactoryName(), propertiesMap);
    }

    @Test
    public void createNpmExternalId() {
        final Map propertiesMap = [
            'npm.name':'component',
            'npm.version':'version'
        ];

        testNameVersionExternalIdCreation(SupportedPackageType.NPM.getArtifactoryName(), propertiesMap);
    }

    @Test
    public void createPypiExternalId() {
        final Map propertiesMap = [
            'pypi.name':'component',
            'pypi.version':'version'
        ];

        testNameVersionExternalIdCreation(SupportedPackageType.PYPI.getArtifactoryName(), propertiesMap);
    }

    @Test
    public void createRubygemsExternalId() {
        final Map propertiesMap = [
            'gem.name':'component',
            'gem.version':'version'
        ];

        testNameVersionExternalIdCreation(SupportedPackageType.GEMS.getArtifactoryName(), propertiesMap);
    }

    @Test
    public void createMavenExternalId() {
        testMavenDependencyCreation(SupportedPackageType.MAVEN.getArtifactoryName());
    }

    @Test
    public void createGradleExternalId() {
        testMavenDependencyCreation(SupportedPackageType.GRADLE.getArtifactoryName());
    }

    private void testNameVersionExternalIdCreation(String packageType, Map propertiesMap) {
        final ArtifactoryExternalIdFactory artifactoryExternalIdFactory = new ArtifactoryExternalIdFactory();
        final String module = 'component';
        final String baseRevision = 'version';
        final def fileLayoutInfo = ['getModule':{module}, 'getBaseRevision':{baseRevision}] as FileLayoutInfo;
        final def missingFileLayoutInfo = ['getModule':{null}, 'getBaseRevision':{null}] as FileLayoutInfo;

        final def properties = ['getFirst':{String key -> propertiesMap.get(key)}] as org.artifactory.md.Properties;
        final def missingProperties = ['getFirst':{String key -> [:].get(key)}] as org.artifactory.md.Properties;

        def externalId = artifactoryExternalIdFactory.createExternalId(testLogger, packageType, fileLayoutInfo, properties);
        assertTrue(externalId.isPresent());

        externalId = artifactoryExternalIdFactory.createExternalId(testLogger, packageType, missingFileLayoutInfo, properties);
        assertTrue(externalId.isPresent());

        externalId = artifactoryExternalIdFactory.createExternalId(testLogger, packageType, fileLayoutInfo, missingProperties);
        assertTrue(externalId.isPresent());

        externalId = artifactoryExternalIdFactory.createExternalId(testLogger, packageType, missingFileLayoutInfo, missingProperties);
        assertFalse(externalId.isPresent());
    }

    private void testMavenDependencyCreation(String packageType) {
        final ArtifactoryExternalIdFactory artifactoryExternalIdFactory = new ArtifactoryExternalIdFactory();
        final String organization = 'group';
        final String module = 'component';
        final String baseRevision = 'version';
        final def fileLayoutInfo = ['getModule':{module}, 'getBaseRevision':{baseRevision}, 'getOrganization':{organization}] as FileLayoutInfo;
        final def missingFileLayoutInfo = ['getModule':{null}, 'getBaseRevision':{null}, 'getOrganization':{null}] as FileLayoutInfo;

        def externalId = artifactoryExternalIdFactory.createExternalId(testLogger, packageType, fileLayoutInfo, null);
        assertTrue(externalId.isPresent());

        externalId = artifactoryExternalIdFactory.createExternalId(testLogger, packageType, missingFileLayoutInfo, null);
        assertFalse(externalId.isPresent());
    }
}

