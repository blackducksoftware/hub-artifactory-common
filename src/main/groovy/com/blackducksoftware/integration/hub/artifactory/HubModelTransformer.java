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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.api.generated.enumeration.NotificationType;
import com.blackducksoftware.integration.hub.api.generated.view.ComponentVersionView;
import com.blackducksoftware.integration.hub.api.generated.view.OriginView;
import com.blackducksoftware.integration.hub.api.generated.view.ProjectVersionView;
import com.blackducksoftware.integration.hub.api.generated.view.VersionBomComponentView;
import com.blackducksoftware.integration.hub.api.response.AffectedProjectVersion;
import com.blackducksoftware.integration.hub.api.response.VulnerabilityNotificationContent;
import com.blackducksoftware.integration.hub.api.view.ReducedNotificationView;
import com.blackducksoftware.integration.hub.api.view.VulnerabilityNotificationView;
import com.blackducksoftware.integration.hub.artifactory.model.ProjectVersionComponentVersionModel;
import com.blackducksoftware.integration.hub.artifactory.model.VulnerabilityNotificationModel;
import com.blackducksoftware.integration.hub.service.HubService;
import com.blackducksoftware.integration.log.IntLogger;

public class HubModelTransformer {
    private final IntLogger intLogger;
    private final HubService hubService;

    public HubModelTransformer(final IntLogger intLogger, final HubService hubService) {
        this.intLogger = intLogger;
        this.hubService = hubService;
    }

    public List<ProjectVersionComponentVersionModel> getProjectVersionComponentVersionModels(final ProjectVersionView projectVersionView, final List<VersionBomComponentView> versionBomComponentRevisedViews) {
        final List<ProjectVersionComponentVersionModel> projectVersionComponentVersionModels = versionBomComponentRevisedViews.stream().map(versionBomComponent -> {
            final String componentVersionLink = versionBomComponent.componentVersion;
            try {
                final ComponentVersionView componentVersionView = hubService.getResponse(componentVersionLink, ComponentVersionView.class);
                final List<OriginView> originViews = hubService.getAllResponses(componentVersionView, ComponentVersionView.ORIGINS_LINK_RESPONSE);
                return new ProjectVersionComponentVersionModel(projectVersionView, versionBomComponent, componentVersionView, originViews);
            } catch (final IntegrationException e) {
                intLogger.error(String.format("Count not create ProjectVersionComponentVersionModel: %s", e.getMessage()), e);
            }
            return new ProjectVersionComponentVersionModel();
        }).collect(Collectors.toList());

        return projectVersionComponentVersionModels;
    }

    public ProjectVersionComponentVersionModel getProjectVersionComponentVersionModel(final ProjectVersionView projectVersionView, final ComponentVersionView componentVersionView) {
        try {
            final String versionBomComponentRevisedViewLink = getProjectVersionComponentLink(projectVersionView, componentVersionView);
            final VersionBomComponentView versionBomComponentView = hubService.getResponse(versionBomComponentRevisedViewLink, VersionBomComponentView.class);
            final List<OriginView> originViews = hubService.getAllResponses(componentVersionView, ComponentVersionView.ORIGINS_LINK_RESPONSE);
            return new ProjectVersionComponentVersionModel(projectVersionView, versionBomComponentView, componentVersionView, originViews);
        } catch (final IntegrationException e) {
            intLogger.error(String.format("Count not create ProjectVersionComponentVersionModel: %s", e.getMessage()), e);
        }

        return new ProjectVersionComponentVersionModel();
    }

    public List<VulnerabilityNotificationModel> getVulnerabilityNotificationModels(final List<ReducedNotificationView> notificationViews, final List<ProjectVersionView> projectVersionViews) {
        final Map<String, ProjectVersionView> projectVersionLinksToLookFor = projectVersionViews.stream().collect(Collectors.toMap(it -> it.meta.href, it -> it));

        final List<VulnerabilityNotificationContent> vulnerabilityNotificationContentList = notificationViews
                .stream()
                .filter(view -> NotificationType.VULNERABILITY == view.type)
                .map(notificationView -> {
                    final VulnerabilityNotificationView vulnerabilityNotificationView = (VulnerabilityNotificationView) notificationView;
                    return vulnerabilityNotificationView.content;
                })
                .filter(content -> {
                    return content.affectedProjectVersions.stream().filter(affectedProjectVersion -> projectVersionLinksToLookFor.containsKey(affectedProjectVersion.projectVersion)).findAny().isPresent();
                })
                .collect(Collectors.toList());

        final List<VulnerabilityNotificationModel> vulnerabilityNotificationModels = new ArrayList<>();
        for (final VulnerabilityNotificationContent vulnerabilityNotificationContent : vulnerabilityNotificationContentList) {
            for (final AffectedProjectVersion affectedProjectVersion : vulnerabilityNotificationContent.affectedProjectVersions) {
                if (projectVersionLinksToLookFor.containsKey(affectedProjectVersion.projectVersion)) {
                    try {
                        final ProjectVersionView projectVersionView = hubService.getResponse(affectedProjectVersion.projectVersion, ProjectVersionView.class);
                        final ComponentVersionView componentVersionView = hubService.getResponse(vulnerabilityNotificationContent.componentVersionLink, ComponentVersionView.class);
                        final ProjectVersionComponentVersionModel projectVersionComponentVersionModel = getProjectVersionComponentVersionModel(projectVersionView, componentVersionView);
                        final VulnerabilityNotificationModel vulnerabiltyNotificationModel = new VulnerabilityNotificationModel(vulnerabilityNotificationContent, projectVersionComponentVersionModel);
                        vulnerabilityNotificationModels.add(vulnerabiltyNotificationModel);
                    } catch (final IntegrationException e) {
                        intLogger.error(String.format("Count not create the VulnerabiltyNotificationModel: ", e.getMessage()), e);
                    }
                }
            }
        }

        return vulnerabilityNotificationModels;
    }

    // not a good practice, but right now, I do not know a better way, short of searching the entire BOM, to match up a BOM component with a component/version
    // ejk - 2018-01-15
    public String getProjectVersionComponentLink(final ProjectVersionView projectVersionView, final ComponentVersionView componentVersionView) {
        final String componentVersionLink = componentVersionView.meta.href;
        final String projectVersionLink = projectVersionView.meta.href;
        final String apiComponentsLinkPrefix = "/api/components/";
        final int apiComponentsStart = componentVersionLink.indexOf(apiComponentsLinkPrefix) + apiComponentsLinkPrefix.length();
        return projectVersionLink + "/components/" + componentVersionLink.substring(apiComponentsStart);
    }

}
