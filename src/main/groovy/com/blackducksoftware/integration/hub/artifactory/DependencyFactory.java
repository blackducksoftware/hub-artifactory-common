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

import org.apache.commons.lang3.StringUtils;
import org.artifactory.fs.FileLayoutInfo;

import com.blackducksoftware.integration.hub.bdio.model.Forge;
import com.blackducksoftware.integration.hub.bdio.model.dependency.Dependency;
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalId;
import com.blackducksoftware.integration.hub.bdio.model.externalid.ExternalIdFactory;

public class DependencyFactory {
    public ExternalIdFactory externalIdFactory;

    public DependencyFactory() {
        externalIdFactory = new ExternalIdFactory();
    }

    public Dependency createMavenDependency(final FileLayoutInfo fileLayoutInfo) {
        final String name = fileLayoutInfo.getModule();
        final String version = fileLayoutInfo.getBaseRevision();
        final String group = fileLayoutInfo.getOrganization();
        final ExternalId externalId = externalIdFactory.createMavenExternalId(group, name, version);
        if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(version) && null != externalId) {
            return new Dependency(name, version, externalId);
        }
        return null;
    }

    public Dependency createNugetDependency(final FileLayoutInfo fileLayoutInfo, final org.artifactory.md.Properties properties) {
        Dependency dependency = createNameVersionDependencyFromProperties(Forge.NUGET, properties, "nuget.id", "nuget.version");
        if (null == dependency) {
            dependency = createNameVersionDependencyFromFileLayoutInfo(Forge.NUGET, fileLayoutInfo);
        }
        return dependency;
    }

    public Dependency createNpmDependency(final FileLayoutInfo fileLayoutInfo, final org.artifactory.md.Properties properties) {
        Dependency dependency = createNameVersionDependencyFromProperties(Forge.NPM, properties, "npm.name", "npm.version");
        if (null == dependency) {
            dependency = createNameVersionDependencyFromFileLayoutInfo(Forge.NPM, fileLayoutInfo);
        }
        return dependency;
    }

    public Dependency createPyPiDependency(final FileLayoutInfo fileLayoutInfo, final org.artifactory.md.Properties properties) {
        Dependency dependency = createNameVersionDependencyFromProperties(Forge.PYPI, properties, "pypi.name", "pypi.version");
        if (null == dependency) {
            dependency = createNameVersionDependencyFromFileLayoutInfo(Forge.PYPI, fileLayoutInfo);
        }
        return dependency;
    }

    public Dependency createRubygemsDependency(final FileLayoutInfo fileLayoutInfo, final org.artifactory.md.Properties properties) {
        Dependency dependency = createNameVersionDependencyFromProperties(Forge.RUBYGEMS, properties, "gem.name", "gem.version");
        if (null == dependency) {
            dependency = createNameVersionDependencyFromFileLayoutInfo(Forge.RUBYGEMS, fileLayoutInfo);
        }
        return dependency;
    }

    private Dependency createNameVersionDependencyFromProperties(final Forge forge, final org.artifactory.md.Properties properties, final String namePropertyName, final String versionPropertyName) {
        final String name = properties.getFirst(namePropertyName);
        final String version = properties.getFirst(versionPropertyName);
        if (StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(version)) {
            return createNameVersionDependency(forge, name, version);
        }
        return null;
    }

    private Dependency createNameVersionDependencyFromFileLayoutInfo(final Forge forge, final FileLayoutInfo fileLayoutInfo) {
        final String name = fileLayoutInfo.getModule();
        final String version = fileLayoutInfo.getBaseRevision();
        return createNameVersionDependency(forge, name, version);
    }

    private Dependency createNameVersionDependency(final Forge forge, final String name, final String version) {
        final ExternalId externalId = externalIdFactory.createNameVersionExternalId(forge, name, version);
        if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(version) && null != externalId) {
            return new Dependency(name, version, externalId);
        }
        return null;
    }

}
