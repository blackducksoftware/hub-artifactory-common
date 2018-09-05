package com.blackducksoftware.integration.hub.artifactory

import com.blackducksoftware.integration.hub.artifactory.inspect.ArtifactoryExternalIdFactory
import com.blackducksoftware.integration.hub.artifactory.inspect.SupportedPackageType
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalIdFactory
import groovy.transform.CompileStatic
import org.artifactory.fs.FileLayoutInfo
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

@CompileStatic
class ArtifactoryExternalIdFactoryTest {
    Logger testLogger = LoggerFactory.getLogger(ArtifactoryExternalIdFactoryTest.class)

    @Test
    void createNugetExternalId() {
        final Map propertiesMap = [
        'nuget.id'     : 'component',
        'nuget.version': 'version'
        ]

        testNameVersionExternalIdCreation(SupportedPackageType.NUGET.getArtifactoryName(), propertiesMap)
    }

    @Test
    void createNpmExternalId() {
        final Map propertiesMap = [
        'npm.name'   : 'component',
        'npm.version': 'version'
        ]

        testNameVersionExternalIdCreation(SupportedPackageType.NPM.getArtifactoryName(), propertiesMap)
    }

    @Test
    void createPypiExternalId() {
        final Map propertiesMap = [
        'pypi.name'   : 'component',
        'pypi.version': 'version'
        ]

        testNameVersionExternalIdCreation(SupportedPackageType.PYPI.getArtifactoryName(), propertiesMap)
    }

    @Test
    void createRubygemsExternalId() {
        final Map propertiesMap = [
        'gem.name'   : 'component',
        'gem.version': 'version'
        ]

        testNameVersionExternalIdCreation(SupportedPackageType.GEMS.getArtifactoryName(), propertiesMap)
    }

    @Test
    void createMavenExternalId() {
        testMavenDependencyCreation(SupportedPackageType.MAVEN.getArtifactoryName())
    }

    @Test
    void createGradleExternalId() {
        testMavenDependencyCreation(SupportedPackageType.GRADLE.getArtifactoryName())
    }

    private void testNameVersionExternalIdCreation(String packageType, Map propertiesMap) {
        final ArtifactoryExternalIdFactory artifactoryExternalIdFactory = new ArtifactoryExternalIdFactory(new ExternalIdFactory())
        final String module = 'component'
        final String baseRevision = 'version'
        final def fileLayoutInfo = ['getModule': { module }, 'getBaseRevision': { baseRevision }] as FileLayoutInfo
        final def missingFileLayoutInfo = ['getModule': { null }, 'getBaseRevision': { null }] as FileLayoutInfo

        final def properties = ['getFirst': { String key -> propertiesMap.get(key) }] as org.artifactory.md.Properties
        final def missingProperties = ['getFirst': { String key -> [:].get(key) }] as org.artifactory.md.Properties

        def externalId = artifactoryExternalIdFactory.createExternalId(packageType, fileLayoutInfo, properties)
        assertTrue(externalId.isPresent())

        externalId = artifactoryExternalIdFactory.createExternalId(packageType, missingFileLayoutInfo, properties)
        assertTrue(externalId.isPresent())

        externalId = artifactoryExternalIdFactory.createExternalId(packageType, fileLayoutInfo, missingProperties)
        assertTrue(externalId.isPresent())

        externalId = artifactoryExternalIdFactory.createExternalId(packageType, missingFileLayoutInfo, missingProperties)
        assertFalse(externalId.isPresent())
    }

    private void testMavenDependencyCreation(String packageType) {
        final ArtifactoryExternalIdFactory artifactoryExternalIdFactory = new ArtifactoryExternalIdFactory(new ExternalIdFactory())
        final String organization = 'group'
        final String module = 'component'
        final String baseRevision = 'version'
        final def fileLayoutInfo = ['getModule': { module }, 'getBaseRevision': { baseRevision }, 'getOrganization': { organization }] as FileLayoutInfo
        final def missingFileLayoutInfo = ['getModule': { null }, 'getBaseRevision': { null }, 'getOrganization': { null }] as FileLayoutInfo

        def externalId = artifactoryExternalIdFactory.createExternalId(packageType, fileLayoutInfo, null)
        assertTrue(externalId.isPresent())

        externalId = artifactoryExternalIdFactory.createExternalId(packageType, missingFileLayoutInfo, null)
        assertFalse(externalId.isPresent())
    }
}

