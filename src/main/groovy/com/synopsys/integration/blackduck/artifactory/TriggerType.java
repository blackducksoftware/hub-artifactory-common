package com.synopsys.integration.blackduck.artifactory;

public enum TriggerType {
    CRON_JOB("cron job"),
    REST_REQUEST("REST request"),
    STARTUP("startup"),
    STORAGE_AFTER_CREATE("storage afterCreate");

    private final String logName;

    TriggerType(final String logName) {
        this.logName = logName;
    }

    public String getLogName() {
        return logName;
    }
}
