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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.api.generated.view.ComponentVersionView;
import com.blackducksoftware.integration.hub.api.generated.view.NotificationView;
import com.blackducksoftware.integration.hub.api.generated.view.ProjectVersionView;
import com.blackducksoftware.integration.hub.api.generated.view.VersionBomComponentView;
import com.blackducksoftware.integration.hub.api.generated.view.VulnerabilityV2View;
import com.blackducksoftware.integration.hub.artifactory.model.ProjectVersionComponentVersionModel;
import com.blackducksoftware.integration.hub.artifactory.model.VulnerabilityNotificationModel;
import com.blackducksoftware.integration.hub.service.HubService;
import com.blackducksoftware.integration.hub.service.NotificationService;
import com.blackducksoftware.integration.log.IntLogger;

public class ArtifactMetaDataManager {
    private final IntLogger intLogger;

    public ArtifactMetaDataManager(final IntLogger intLogger) {
        this.intLogger = intLogger;
    }

    public List<ArtifactMetaData> getMetaData(final String repoKey, final HubService hubService, final ProjectVersionView projectVersionView) throws IntegrationException {
        final Map<String, ArtifactMetaData> idToArtifactMetaData = new HashMap<>();

        final HubModelTransformer hubModelTransformer = new HubModelTransformer(intLogger, hubService);
        final List<VersionBomComponentView> versionBomComponentViews = hubService.getAllResponses(projectVersionView, ProjectVersionView.COMPONENTS_LINK_RESPONSE);
        final List<ProjectVersionComponentVersionModel> projectVersionComponentVersionModels = hubModelTransformer.getProjectVersionComponentVersionModels(projectVersionView, versionBomComponentViews);
        for (final ProjectVersionComponentVersionModel projectVersionComponentVersionModel : projectVersionComponentVersionModels) {
            populateMetaDataMap(repoKey, idToArtifactMetaData, hubService, projectVersionComponentVersionModel);
        }

        return new ArrayList<>(idToArtifactMetaData.values());
    }

    public List<ArtifactMetaData> getMetaDataFromNotifications(final String repoKey, final HubService hubService, final NotificationService notificationService, final ProjectVersionView projectVersionView, final Date startDate, final Date endDate)
            throws IntegrationException {
        final Map<String, ArtifactMetaData> idToArtifactMetaData = new HashMap<>();

        final List<NotificationView> notificationViews = notificationService.getAllNotifications(startDate, endDate);
        final List<ProjectVersionView> projectVersionViews = Arrays.asList(new ProjectVersionView[] { projectVersionView });
        final HubModelTransformer hubModelTransformer = new HubModelTransformer(intLogger, hubService);
        final List<VulnerabilityNotificationModel> vulnerabilityNotificationModels = hubModelTransformer.getVulnerabilityNotificationModels(notificationViews, projectVersionViews);
        for (final VulnerabilityNotificationModel vulnerabilityNotificationModel : vulnerabilityNotificationModels) {
            populateMetaDataMap(repoKey, idToArtifactMetaData, hubService, vulnerabilityNotificationModel.projectVersionComponentVersionModel);
        }

        return new ArrayList<>(idToArtifactMetaData.values());
    }

    private void populateMetaDataMap(final String repoKey, final Map<String, ArtifactMetaData> idToArtifactMetaData, final HubService hubService, final ProjectVersionComponentVersionModel projectVersionComponentVersionModel) {
        projectVersionComponentVersionModel.originViews.forEach(originView -> {
            final String forge = originView.originName;
            final String originId = originView.originId;
            if (!idToArtifactMetaData.containsKey(key(forge, originId))) {
                final ArtifactMetaData artifactMetaData = new ArtifactMetaData();
                artifactMetaData.repoKey = repoKey;
                artifactMetaData.forge = forge;
                artifactMetaData.originId = originId;
                artifactMetaData.componentVersionLink = projectVersionComponentVersionModel.componentVersionView.meta.href;
                artifactMetaData.policyStatus = projectVersionComponentVersionModel.versionBomComponentRevisedView.policyStatus;

                populateVulnerabilityCounts(artifactMetaData, projectVersionComponentVersionModel.componentVersionView, hubService);

                idToArtifactMetaData.put(key(forge, originId), artifactMetaData);
            }
        });
    }

    private void populateVulnerabilityCounts(final ArtifactMetaData artifactMetaData, final ComponentVersionView componentVersionView, final HubService hubService) {
        final String vulnerabilitiesLink = hubService.getFirstLinkSafely(componentVersionView, ComponentVersionView.VULNERABILITIES_LINK);
        if (StringUtils.isNotBlank(vulnerabilitiesLink)) {
            try {
                final List<VulnerabilityV2View> componentVulnerabilities = hubService.getAllResponses(vulnerabilitiesLink, VulnerabilityV2View.class);
                componentVulnerabilities.forEach(vulnerability -> {
                    if ("HIGH".equals(vulnerability.severity)) {
                        artifactMetaData.highSeverityCount++;
                    } else if ("MEDIUM".equals(vulnerability.severity)) {
                        artifactMetaData.mediumSeverityCount++;
                    } else if ("LOW".equals(vulnerability.severity)) {
                        artifactMetaData.lowSeverityCount++;
                    }
                });
            } catch (final IntegrationException e) {
                intLogger.error(String.format("Can't populate vulnerability counts for %s: %s", componentVersionView.meta.href, e.getMessage()));
            }
        }
    }

    private String key(final String forge, final String originId) {
        return forge + ":" + originId;
    }
}
