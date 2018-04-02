package com.blackducksoftware.integration.hub.artifactory.scan;

import com.blackducksoftware.integration.hub.artifactory.ConfigurationProperty;

public enum ScanPluginProperty implements ConfigurationProperty {
    REPOS("repos"),
    REPOS_CSV_PATH("repos.csv.path"),
    NAME_PATTERNS("name.patterns"),
    BINARIES_DIRECTORY_PATH("binaries.directory.path"),
    MEMORY("memory"),
    DRY_RUN("dry.run"),
    DATE_TIME_PATTERN("date.time.pattern"),
    CUTOFF_DATE("cutoff.date"),
    CRON_LOG_VERBOSE("cron.log.verbose"),
    SCAN_CRON("cron"),
    ADD_POLICY_STATUS_CRON("add.policy.status.cron");

    private final String key;

    private ScanPluginProperty(final String key) {
        this.key = "hub.artifactory.scan." + key;
    }

    @Override
    public String getKey() {
        return key;
    }

}
