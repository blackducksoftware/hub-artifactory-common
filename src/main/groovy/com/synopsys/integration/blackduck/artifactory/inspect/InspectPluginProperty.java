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
package com.synopsys.integration.blackduck.artifactory.inspect;

import com.synopsys.integration.blackduck.artifactory.ConfigurationProperty;

public enum InspectPluginProperty implements ConfigurationProperty {
    REPOS("repos"),
    REPOS_CSV_PATH("repos.csv.path"),
    PATTERNS_RUBYGEMS("patterns.rubygems"),
    PATTERNS_MAVEN("patterns.maven"),
    PATTERNS_GRADLE("patterns.gradle"),
    PATTERNS_PYPI("patterns.pypi"),
    PATTERNS_NUGET("patterns.nuget"),
    PATTERNS_NPM("patterns.npm"),
    DATE_TIME_PATTERN("date.time.pattern"),
    CRON_LOG_VERBOSE("cron.log.verbose"),
    IDENTIFY_ARTIFACTS_CRON("identify.artifacts.cron"),
    POPULATE_METADATA_CRON("populate.metadata.cron"),
    UPDATE_METADATA_CRON("update.metadata.cron"),
    ADD_PENDING_ARTIFACTS_CRON("add.pending.artifacts.cron");

    private final String key;

    private InspectPluginProperty(final String key) {
        this.key = "blackduck.artifactory.inspect." + key;
    }

    @Override
    public String getKey() {
        return key;
    }

}
