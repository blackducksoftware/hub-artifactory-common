package com.blackducksoftware.integration.hub.artifactory.inspect;

import com.blackducksoftware.integration.hub.artifactory.ConfigurationProperty;

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
        this.key = "hub.artifactory.inspect." + key;
    }

    @Override
    public String getKey() {
        return key;
    }

}
