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
package com.synopsys.integration.blackduck.artifactory.scan;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.artifactory.BlackDuckPropertyManager;
import com.synopsys.integration.blackduck.artifactory.ModuleConfig;
import com.synopsys.integration.exception.IntegrationException;

public class ScanModuleConfig extends ModuleConfig {
    private final Logger logger = LoggerFactory.getLogger(ScanModuleConfig.class);

    private final String artifactCutoffDate;
    private final String blackDuckScanCron;
    private final String blackDuckAddPolicyStatusCron;

    private File cliDirectory;

    public ScanModuleConfig(final boolean enabled, final String artifactCutoffDate, final String blackDuckScanCron, final String blackDuckAddPolicyStatusCron) {
        super(ScanModule.class.getSimpleName(), enabled);
        this.artifactCutoffDate = artifactCutoffDate;
        this.blackDuckScanCron = blackDuckScanCron;
        this.blackDuckAddPolicyStatusCron = blackDuckAddPolicyStatusCron;
    }

    public static ScanModuleConfig createFromProperties(final BlackDuckPropertyManager blackDuckPropertyManager) {
        final boolean enabled = blackDuckPropertyManager.getBooleanProperty(ScanModuleProperty.ENABLED);
        final String artifactCutoffDate = blackDuckPropertyManager.getProperty(ScanModuleProperty.CUTOFF_DATE);
        final String blackDuckScanCron = blackDuckPropertyManager.getProperty(ScanModuleProperty.SCAN_CRON);
        final String blackDuckAddPolicyStatusCron = blackDuckPropertyManager.getProperty(ScanModuleProperty.ADD_POLICY_STATUS_CRON);

        return new ScanModuleConfig(enabled, artifactCutoffDate, blackDuckScanCron, blackDuckAddPolicyStatusCron);
    }

    // TODO: Move to ScanModule
    public void setUpCliDuckDirectory(final File blackDuckDirectory) {
        try {
            final File cliDirectory = new File(blackDuckDirectory, "cli");
            if (!cliDirectory.exists() && !cliDirectory.mkdir()) {
                throw new IntegrationException(String.format("Failed to create cliDirectory: %s", cliDirectory.getCanonicalPath()));
            }
            this.cliDirectory = cliDirectory;
        } catch (final IOException | IntegrationException e) {
            logger.error(String.format("Exception while setting up the Black Duck directory %s", cliDirectory), e);
        }
    }

    public File getCliDirectory() {
        return cliDirectory;
    }

    public String getArtifactCutoffDate() {
        return artifactCutoffDate;
    }

    public String getBlackDuckScanCron() {
        return blackDuckScanCron;
    }

    public String getBlackDuckAddPolicyStatusCron() {
        return blackDuckAddPolicyStatusCron;
    }
}
