package com.synopsys.integration.blackduck.artifactory;

import com.synopsys.integration.log.IntLogger;

public class LogUtil {
    public static void start(final IntLogger logger, final String functionName, final TriggerType triggerType) {
        if (triggerType.equals(TriggerType.STARTUP)) {
            logger.info(String.format("Starting %s for %s...", functionName, triggerType.getLogName()));
        } else {
            logger.info(String.format("Starting %s from %s...", functionName, triggerType.getLogName()));
        }
    }

    public static void finish(final IntLogger logger, final String functionName, final TriggerType triggerType) {
        logger.info(String.format("...completed %s %s.", functionName, triggerType.getLogName()));
    }
}
