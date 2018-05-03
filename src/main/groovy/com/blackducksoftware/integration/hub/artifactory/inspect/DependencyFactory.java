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
package com.blackducksoftware.integration.hub.artifactory.inspect;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.artifactory.fs.FileLayoutInfo;
import org.slf4j.Logger;

import com.blackducksoftware.integration.hub.bdio.model.Forge;
import com.blackducksoftware.integration.hub.bdio.model.dependency.Dependency;
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalId;
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalIdFactory;

public class DependencyFactory {
    private final ExternalIdFactory externalIdFactory;

    public DependencyFactory() {
        externalIdFactory = new ExternalIdFactory();
    }

    public DependencyFactory(final ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
    }

    public Optional<Dependency> createDependency(final Logger log, final String packageType, final FileLayoutInfo fileLayoutInfo, final org.artifactory.md.Properties properties) {
        Optional<Dependency> optionalDependency = Optional.empty();
        try {
            if (SupportedPackageType.nuget.name().equals(packageType)) {
                optionalDependency = createNugetDependency(fileLayoutInfo, properties);
            } else if (SupportedPackageType.npm.name().equals(packageType)) {
                optionalDependency = createNpmDependency(fileLayoutInfo, properties);
            } else if (SupportedPackageType.pypi.name().equals(packageType)) {
                optionalDependency = createPyPiDependency(fileLayoutInfo, properties);
            } else if (SupportedPackageType.gems.name().equals(packageType)) {
                optionalDependency = createRubygemsDependency(fileLayoutInfo, properties);
            } else if (SupportedPackageType.maven.name().equals(packageType) || SupportedPackageType.gradle.name().equals(packageType)) {
                optionalDependency = createMavenDependency(fileLayoutInfo);
            }
        } catch (final Exception e) {
            log.error("Could not resolve dependency:", e);
        }

        return optionalDependency;
    }

    private Optional<Dependency> createMavenDependency(final FileLayoutInfo fileLayoutInfo) {
        final String group = fileLayoutInfo.getOrganization();
        final String name = fileLayoutInfo.getModule();
        final String version = fileLayoutInfo.getBaseRevision();
        Dependency dependency = null;
        if (StringUtils.isNotBlank(group) && StringUtils.isNotBlank(name) && StringUtils.isNotBlank(version)) {
            final ExternalId externalId = externalIdFactory.createMavenExternalId(group, name, version);
            dependency = new Dependency(name, version, externalId);
        }
        return Optional.ofNullable(dependency);
    }

    private Optional<Dependency> createNugetDependency(final FileLayoutInfo fileLayoutInfo, final org.artifactory.md.Properties properties) {
        Optional<Dependency> dependency = createNameVersionDependencyFromProperties(Forge.NUGET, properties, "nuget.id", "nuget.version");
        if (!dependency.isPresent()) {
            dependency = createNameVersionDependencyFromFileLayoutInfo(Forge.NUGET, fileLayoutInfo);
        }
        return dependency;
    }

    private Optional<Dependency> createNpmDependency(final FileLayoutInfo fileLayoutInfo, final org.artifactory.md.Properties properties) {
        Optional<Dependency> dependency = createNameVersionDependencyFromProperties(Forge.NPM, properties, "npm.name", "npm.version");
        if (!dependency.isPresent()) {
            dependency = createNameVersionDependencyFromFileLayoutInfo(Forge.NPM, fileLayoutInfo);
        }
        return dependency;
    }

    private Optional<Dependency> createPyPiDependency(final FileLayoutInfo fileLayoutInfo, final org.artifactory.md.Properties properties) {
        Optional<Dependency> dependency = createNameVersionDependencyFromProperties(Forge.PYPI, properties, "pypi.name", "pypi.version");
        if (!dependency.isPresent()) {
            dependency = createNameVersionDependencyFromFileLayoutInfo(Forge.PYPI, fileLayoutInfo);
        }
        return dependency;
    }

    private Optional<Dependency> createRubygemsDependency(final FileLayoutInfo fileLayoutInfo, final org.artifactory.md.Properties properties) {
        Optional<Dependency> dependency = createNameVersionDependencyFromProperties(Forge.RUBYGEMS, properties, "gem.name", "gem.version");
        if (!dependency.isPresent()) {
            dependency = createNameVersionDependencyFromFileLayoutInfo(Forge.RUBYGEMS, fileLayoutInfo);
        }
        return dependency;
    }

    private Optional<Dependency> createNameVersionDependencyFromProperties(final Forge forge, final org.artifactory.md.Properties properties, final String namePropertyName, final String versionPropertyName) {
        final String name = properties.getFirst(namePropertyName);
        final String version = properties.getFirst(versionPropertyName);
        return createNameVersionDependency(forge, name, version);
    }

    private Optional<Dependency> createNameVersionDependencyFromFileLayoutInfo(final Forge forge, final FileLayoutInfo fileLayoutInfo) {
        final String name = fileLayoutInfo.getModule();
        final String version = fileLayoutInfo.getBaseRevision();
        return createNameVersionDependency(forge, name, version);
    }

    private Optional<Dependency> createNameVersionDependency(final Forge forge, final String name, final String version) {
        Dependency dependency = null;
        if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(version)) {
            final ExternalId externalId = externalIdFactory.createNameVersionExternalId(forge, name, version);
            dependency = new Dependency(name, version, externalId);
        }
        return Optional.ofNullable(dependency);
    }

}
