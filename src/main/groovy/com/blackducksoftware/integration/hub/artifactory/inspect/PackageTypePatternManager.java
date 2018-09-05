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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.blackducksoftware.integration.hub.artifactory.BlackDuckArtifactoryConfig;

public class PackageTypePatternManager {
    private final Map<SupportedPackageType, String> patternMap;

    public PackageTypePatternManager() {
        patternMap = new HashMap<>();
    }

    public void loadPatterns(final BlackDuckArtifactoryConfig blackDuckArtifactoryConfig) {
        patternMap.put(SupportedPackageType.GEMS, blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.PATTERNS_RUBYGEMS));
        patternMap.put(SupportedPackageType.MAVEN, blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.PATTERNS_MAVEN));
        patternMap.put(SupportedPackageType.GRADLE, blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.PATTERNS_GRADLE));
        patternMap.put(SupportedPackageType.PYPI, blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.PATTERNS_PYPI));
        patternMap.put(SupportedPackageType.NUGET, blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.PATTERNS_NUGET));
        patternMap.put(SupportedPackageType.NPM, blackDuckArtifactoryConfig.getProperty(InspectPluginProperty.PATTERNS_NPM));
    }

    public Optional<String> getPattern(final String packageType) {
        Optional<String> pattern = Optional.empty();

        final Optional<SupportedPackageType> possiblySupportedPackageType = SupportedPackageType.getAsSupportedPackageType(packageType);
        if (possiblySupportedPackageType.isPresent()) {
            pattern = getPattern(possiblySupportedPackageType.get());
        }

        return pattern;
    }

    public Optional<String> getPattern(final SupportedPackageType packageType) {
        return Optional.ofNullable(patternMap.get(packageType));
    }

}
