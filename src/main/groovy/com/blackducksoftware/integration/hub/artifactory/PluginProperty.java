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

public enum PluginProperty {
    BLACKDUCK_HUB_URL("blackduck.hub.url"),
    BLACKDUCK_HUB_API_TOKEN("blackduck.hub.api.token"),
    BLACKDUCK_HUB_TIMEOUT("blackduck.hub.timeout"),
    BLACKDUCK_HUB_PROXY_HOST("blackduck.hub.proxy.host"),
    BLACKDUCK_HUB_PROXY_PORT("blackduck.hub.proxy.port"),
    BLACKDUCK_HUB_PROXY_USERNAME("blackduck.hub.proxy.username"),
    BLACKDUCK_HUB_PROXY_PASSWORD("blackduck.hub.proxy.password"),
    BLACKDUCK_HUB_TRUST_CERT("blackduck.hub.trust.cert"),

    HUB_ARTIFACTORY_INSPECT_REPOS("hub.artifactory.inspect.repos"),
    HUB_ARTIFACTORY_INSPECT_REPOS_CSV_PATH("hub.artifactory.inspect.repos.csv.path"),
    HUB_ARTIFACTORY_INSPECT_PATTERNS_RUBYGEMS("hub.artifactory.inspect.patterns.rubygems"),
    HUB_ARTIFACTORY_INSPECT_PATTERNS_MAVEN("hub.artifactory.inspect.patterns.maven"),
    HUB_ARTIFACTORY_INSPECT_PATTERNS_GRADLE("hub.artifactory.inspect.patterns.gradle"),
    HUB_ARTIFACTORY_INSPECT_PATTERNS_PYPI("hub.artifactory.inspect.patterns.pypi"),
    HUB_ARTIFACTORY_INSPECT_PATTERNS_NUGET("hub.artifactory.inspect.patterns.nuget"),
    HUB_ARTIFACTORY_INSPECT_PATTERNS_NPM("hub.artifactory.inspect.patterns.npm"),
    HUB_ARTIFACTORY_INSPECT_DATE_TIME_PATTERN("hub.artifactory.inspect.date.time.pattern"),
    HUB_ARTIFACTORY_INSPECT_CRON_LOG_VERBOSE("hub.artifactory.inspect.cron.log.verbose"),

    HUB_ARTIFACTORY_SCAN_REPOS("hub.artifactory.scan.repos"),
    HUB_ARTIFACTORY_SCAN_REPOS_CSV_PATH("hub.artifactory.scan.repos.csv.path"),
    HUB_ARTIFACTORY_SCAN_NAME_PATTERNS("hub.artifactory.scan.name.patterns"),
    HUB_ARTIFACTORY_SCAN_BINARIES_DIRECTORY_PATH("hub.artifactory.scan.binaries.directory.path"),
    HUB_ARTIFACTORY_SCAN_MEMORY("hub.artifactory.scan.memory"),
    HUB_ARTIFACTORY_SCAN_DRY_RUN("hub.artifactory.scan.dry.run"),
    HUB_ARTIFACTORY_SCAN_DATE_TIME_PATTERN("hub.artifactory.scan.date.time.pattern"),
    HUB_ARTIFACTORY_SCAN_CUTOFF_DATE("hub.artifactory.scan.cutoff.date"),
    HUB_ARTIFACTORY_SCAN_CRON_LOG_VERBOSE("hub.artifactory.scan.cron.log.verbose");

    private final String key;

    private PluginProperty(final String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

}
