package com.synopsys.integration.blackduck.artifactory;

public enum TriggerType {
    CRON_JOB("cron job"),
    REST_REQUEST("REST request");

    private String logName;

    TriggerType(final String logName) {
        this.logName = logName;
    }

    public String getLogName() {
        return logName;
    }
}
