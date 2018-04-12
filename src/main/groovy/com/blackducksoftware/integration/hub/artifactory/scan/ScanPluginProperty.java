package com.blackducksoftware.integration.hub.artifactory.scan;

import com.blackducksoftware.integration.hub.artifactory.ConfigurationProperty;

public enum ScanPluginProperty implements ConfigurationProperty {
    ADD_POLICY_STATUS_CRON("add.policy.status.cron"),
    BINARIES_DIRECTORY_PATH("binaries.directory.path"),
    CUTOFF_DATE("cutoff.date"),
    CRON_LOG_VERBOSE("cron.log.verbose"),
    DATE_TIME_PATTERN("date.time.pattern"),
    DRY_RUN("dry.run"),
    NAME_PATTERNS("name.patterns"),
    MEMORY("memory"),
    REPO_PATH_CODELOCATION("repo.path.codelocation"),
    REPOS("repos"),
    REPOS_CSV_PATH("repos.csv.path"),
    SCAN_CRON("cron");

    private final String key;

    private ScanPluginProperty(final String key) {
        this.key = "hub.artifactory.scan." + key;
    }

    @Override
    public String getKey() {
        return key;
    }

}
