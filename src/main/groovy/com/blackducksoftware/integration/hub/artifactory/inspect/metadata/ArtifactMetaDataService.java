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
package com.blackducksoftware.integration.hub.artifactory.inspect.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.api.generated.view.ComponentVersionView;
import com.blackducksoftware.integration.hub.api.generated.view.NotificationView;
import com.blackducksoftware.integration.hub.api.generated.view.ProjectVersionView;
import com.blackducksoftware.integration.hub.api.generated.view.VersionBomComponentView;
import com.blackducksoftware.integration.hub.api.generated.view.VulnerabilityV2View;
import com.blackducksoftware.integration.hub.artifactory.HubConnectionService;
import com.blackducksoftware.integration.hub.notification.CommonNotificationView;
import com.blackducksoftware.integration.hub.notification.NotificationDetailResults;
import com.blackducksoftware.integration.hub.notification.content.detail.NotificationContentDetailFactory;
import com.blackducksoftware.integration.hub.service.CommonNotificationService;
import com.blackducksoftware.integration.hub.service.HubService;
import com.blackducksoftware.integration.hub.service.HubServicesFactory;
import com.blackducksoftware.integration.hub.service.NotificationService;
import com.blackducksoftware.integration.hub.service.ProjectService;
import com.blackducksoftware.integration.log.IntLogger;
import com.blackducksoftware.integration.log.Slf4jIntLogger;

public class ArtifactMetaDataService {
    private final Logger logger = LoggerFactory.getLogger(ArtifactMetaDataService.class);

    private final IntLogger intLogger;
    private final HubConnectionService hubConnectionService;

    public ArtifactMetaDataService(final HubConnectionService hubConnectionService) {
        this.intLogger = new Slf4jIntLogger(logger);
        this.hubConnectionService = hubConnectionService;
    }

    public List<ArtifactMetaData> getArtifactMetadataOfRepository(final String repoKey, final String projectName, final String projectVersionName) throws IntegrationException {
        final HubServicesFactory hubServicesFactory = hubConnectionService.createHubServicesFactory();
        final HubService hubService = hubServicesFactory.createHubService();
        final ProjectService projectDataService = hubServicesFactory.createProjectService();
        final CompositeComponentManager compositeComponentManager = new CompositeComponentManager(intLogger, hubService);
        final Map<String, ArtifactMetaData> idToArtifactMetaData = new HashMap<>();

        final ProjectVersionView projectVersionView = projectDataService.getProjectVersion(projectName, projectVersionName).getProjectVersionView();
        final List<VersionBomComponentView> versionBomComponentViews = hubService.getAllResponses(projectVersionView, ProjectVersionView.COMPONENTS_LINK_RESPONSE);
        final List<CompositeComponentModel> projectVersionComponentVersionModels = compositeComponentManager.parseBom(projectVersionView, versionBomComponentViews);

        for (final CompositeComponentModel projectVersionComponentVersionModel : projectVersionComponentVersionModels) {
            populateMetaDataMap(repoKey, idToArtifactMetaData, hubService, projectVersionComponentVersionModel);
        }

        return new ArrayList<>(idToArtifactMetaData.values());
    }

    public ArtifactMetaDataFromNotifications getArtifactMetadataFromNotifications(final String repoKey, final String projectName, final String projectVersionName, final Date startDate, final Date endDate) throws IntegrationException {
        final HubServicesFactory hubServicesFactory = hubConnectionService.createHubServicesFactory();
        final NotificationService notificationService = hubServicesFactory.createNotificationService();
        final CommonNotificationService commonNotificationService = hubServicesFactory.createCommonNotificationService(new NotificationContentDetailFactory(HubServicesFactory.createDefaultGson(), HubServicesFactory.createDefaultJsonParser()), false);
        final ProjectService projectDataService = hubServicesFactory.createProjectService();
        final HubService hubService = hubServicesFactory.createHubService();
        final CompositeComponentManager compositeComponentManager = new CompositeComponentManager(intLogger, hubService);
        final Map<String, ArtifactMetaData> idToArtifactMetaData = new HashMap<>();

        final List<NotificationView> notificationViews = notificationService.getAllNotifications(startDate, endDate);
        final List<CommonNotificationView> commonNotificationViews = commonNotificationService.getCommonNotifications(notificationViews);
        final NotificationDetailResults notificationDetailResults = commonNotificationService.getNotificationDetailResults(commonNotificationViews);
        final ProjectVersionView projectVersionView = projectDataService.getProjectVersion(projectName, projectVersionName).getProjectVersionView();
        final List<ProjectVersionView> projectVersionViews = Arrays.asList(projectVersionView);
        final List<CompositeComponentModel> projectVersionComponentVersionModels = compositeComponentManager.parseNotifications(notificationDetailResults, projectVersionViews);

        for (final CompositeComponentModel projectVersionComponentVersionModel : projectVersionComponentVersionModels) {
            populateMetaDataMap(repoKey, idToArtifactMetaData, hubService, projectVersionComponentVersionModel);
        }

        return new ArtifactMetaDataFromNotifications(notificationDetailResults.getLatestNotificationCreatedAtDate(), new ArrayList<>(idToArtifactMetaData.values()));
    }

    private void populateMetaDataMap(final String repoKey, final Map<String, ArtifactMetaData> idToArtifactMetaData, final HubService hubService, final CompositeComponentModel compositeComponentModel) {
        compositeComponentModel.originViews.forEach(originView -> {
            final String forge = originView.originName;
            final String originId = originView.originId;
            if (!idToArtifactMetaData.containsKey(key(forge, originId))) {
                final ArtifactMetaData artifactMetaData = new ArtifactMetaData();
                artifactMetaData.repoKey = repoKey;
                artifactMetaData.forge = forge;
                artifactMetaData.originId = originId;
                artifactMetaData.componentVersionLink = compositeComponentModel.componentVersionView._meta.href;
                artifactMetaData.policyStatus = compositeComponentModel.versionBomComponentView.policyStatus;

                populateVulnerabilityCounts(artifactMetaData, compositeComponentModel.componentVersionView, hubService);

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
                intLogger.error(String.format("Can't populate vulnerability counts for %s: %s", componentVersionView._meta.href, e.getMessage()));
            }
        }
    }

    private String key(final String forge, final String originId) {
        return forge + ":" + originId;
    }
}