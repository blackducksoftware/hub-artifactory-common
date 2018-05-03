package com.blackducksoftware.integration.hub.artifactory

import static org.junit.Assert.*

import org.artifactory.fs.FileLayoutInfo
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.blackducksoftware.integration.hub.artifactory.inspect.DependencyFactory
import com.blackducksoftware.integration.hub.artifactory.inspect.SupportedPackageType

public class DependencyFactoryTest {
    Logger testLogger = LoggerFactory.getLogger(DependencyFactoryTest.class);

    @Test
    public void createNugetDependency() {
        final Map propertiesMap = [
            'nuget.id':'component',
            'nuget.version':'version'
        ];

        testNameVersionDependencyCreation(SupportedPackageType.nuget.name(), propertiesMap);
    }

    @Test
    public void createNpmDependency() {
        final Map propertiesMap = [
            'npm.name':'component',
            'npm.version':'version'
        ];

        testNameVersionDependencyCreation(SupportedPackageType.npm.name(), propertiesMap);
    }

    @Test
    public void createPypiDependency() {
        final Map propertiesMap = [
            'pypi.name':'component',
            'pypi.version':'version'
        ];

        testNameVersionDependencyCreation(SupportedPackageType.pypi.name(), propertiesMap);
    }

    @Test
    public void createRubygemsDependency() {
        final Map propertiesMap = [
            'gem.name':'component',
            'gem.version':'version'
        ];

        testNameVersionDependencyCreation(SupportedPackageType.gems.name(), propertiesMap);
    }

    @Test
    public void createMavenDependency() {
        testMavenDependencyCreation(SupportedPackageType.maven.name());
    }

    @Test
    public void createGradleDependency() {
        testMavenDependencyCreation(SupportedPackageType.gradle.name());
    }

    private void testNameVersionDependencyCreation(String packageType, Map propertiesMap) {
        final DependencyFactory dependencyFactory = new DependencyFactory();
        final String module = 'component';
        final String baseRevision = 'version';
        final def fileLayoutInfo = ['getModule':{module}, 'getBaseRevision':{baseRevision}] as FileLayoutInfo;
        final def missingFileLayoutInfo = ['getModule':{null}, 'getBaseRevision':{null}] as FileLayoutInfo;

        final def properties = ['getFirst':{String key -> propertiesMap.get(key)}] as org.artifactory.md.Properties;
        final def missingProperties = ['getFirst':{String key -> [:].get(key)}] as org.artifactory.md.Properties;

        def dependency = dependencyFactory.createDependency(testLogger, packageType, fileLayoutInfo, properties);
        assertTrue(dependency.isPresent());

        dependency = dependencyFactory.createDependency(testLogger, packageType, missingFileLayoutInfo, properties);
        assertTrue(dependency.isPresent());

        dependency = dependencyFactory.createDependency(testLogger, packageType, fileLayoutInfo, missingProperties);
        assertTrue(dependency.isPresent());

        dependency = dependencyFactory.createDependency(testLogger, packageType, missingFileLayoutInfo, missingProperties);
        assertFalse(dependency.isPresent());
    }

    private void testMavenDependencyCreation(String packageType) {
        final DependencyFactory dependencyFactory = new DependencyFactory();
        final String organization = 'group';
        final String module = 'component';
        final String baseRevision = 'version';
        final def fileLayoutInfo = ['getModule':{module}, 'getBaseRevision':{baseRevision}, 'getOrganization':{organization}] as FileLayoutInfo;
        final def missingFileLayoutInfo = ['getModule':{null}, 'getBaseRevision':{null}, 'getOrganization':{null}] as FileLayoutInfo;

        def dependency = dependencyFactory.createDependency(testLogger, packageType, fileLayoutInfo, null);
        assertTrue(dependency.isPresent());

        dependency = dependencyFactory.createDependency(testLogger, packageType, missingFileLayoutInfo, null);
        assertFalse(dependency.isPresent());
    }
}

