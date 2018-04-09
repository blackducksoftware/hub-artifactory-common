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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.api.generated.enumeration.NotificationType;
import com.blackducksoftware.integration.hub.api.generated.view.ComponentVersionView;
import com.blackducksoftware.integration.hub.api.generated.view.OriginView;
import com.blackducksoftware.integration.hub.api.generated.view.ProjectVersionView;
import com.blackducksoftware.integration.hub.api.generated.view.VersionBomComponentView;
import com.blackducksoftware.integration.hub.api.response.PolicyOverrideNotificationContent;
import com.blackducksoftware.integration.hub.api.response.RuleViolationClearedNotificationContent;
import com.blackducksoftware.integration.hub.api.response.RuleViolationNotificationContent;
import com.blackducksoftware.integration.hub.api.response.VulnerabilityNotificationContent;
import com.blackducksoftware.integration.hub.api.view.PolicyOverrideNotificationView;
import com.blackducksoftware.integration.hub.api.view.ReducedNotificationView;
import com.blackducksoftware.integration.hub.api.view.RuleViolationClearedNotificationView;
import com.blackducksoftware.integration.hub.api.view.RuleViolationNotificationView;
import com.blackducksoftware.integration.hub.api.view.VulnerabilityNotificationView;
import com.blackducksoftware.integration.hub.artifactory.model.ProjectVersionComponentVersionModel;
import com.blackducksoftware.integration.hub.service.HubService;
import com.blackducksoftware.integration.log.IntLogger;

public class HubModelTransformer {
    private final IntLogger intLogger;
    private final HubService hubService;

    public HubModelTransformer(final IntLogger intLogger, final HubService hubService) {
        this.intLogger = intLogger;
        this.hubService = hubService;
    }

    public List<ProjectVersionComponentVersionModel> createProjectVersionComponentVersionModels(final ProjectVersionView projectVersionView, final List<VersionBomComponentView> versionBomComponentRevisedViews) {
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

    public ProjectVersionComponentVersionModel createProjectVersionComponentVersionModel(final String projectVersionLink, final String componentVersionLink) {
        try {
            final ProjectVersionView projectVersionView = hubService.getResponse(projectVersionLink, ProjectVersionView.class);
            final ComponentVersionView componentVersionView = hubService.getResponse(componentVersionLink, ComponentVersionView.class);
            final String versionBomComponentRevisedViewLink = getProjectVersionComponentLink(projectVersionView, componentVersionView);
            final VersionBomComponentView versionBomComponentView = hubService.getResponse(versionBomComponentRevisedViewLink, VersionBomComponentView.class);
            final List<OriginView> originViews = hubService.getAllResponses(componentVersionView, ComponentVersionView.ORIGINS_LINK_RESPONSE);
            return new ProjectVersionComponentVersionModel(projectVersionView, versionBomComponentView, componentVersionView, originViews);
        } catch (final IntegrationException e) {
            intLogger.error(String.format("Could not create the ProjectVersionComponentVersionModel: %s", e.getMessage()), e);
        }

        return new ProjectVersionComponentVersionModel();
    }

    public List<ProjectVersionComponentVersionModel> transformToProjectVersionComponentVersionModels(final ReducedNotificationView reducedNotificationView, final Map<String, ProjectVersionView> projectVersionLinksToLookFor) {
        final List<String> projectVersionLinks;
        final List<String> componentVersionLinks;

        if (NotificationType.VULNERABILITY == reducedNotificationView.type) {
            final VulnerabilityNotificationView vulnerabilityNotificationView = (VulnerabilityNotificationView) reducedNotificationView;
            final VulnerabilityNotificationContent vulnerabilityNotificationContent = vulnerabilityNotificationView.content;
            projectVersionLinks = vulnerabilityNotificationContent.affectedProjectVersions.stream()
                    .map(affectedProjectVersion -> affectedProjectVersion.projectVersion)
                    .collect(Collectors.toList());
            componentVersionLinks = Arrays.asList(vulnerabilityNotificationContent.componentVersionLink);
        } else if (NotificationType.RULE_VIOLATION == reducedNotificationView.type) {
            final RuleViolationNotificationView ruleViolationNotificationView = (RuleViolationNotificationView) reducedNotificationView;
            final RuleViolationNotificationContent ruleViolationNotificationContent = ruleViolationNotificationView.content;
            projectVersionLinks = Arrays.asList(ruleViolationNotificationContent.projectVersionLink);
            componentVersionLinks = ruleViolationNotificationContent.componentVersionStatuses.stream()
                    .map(componentVersionStatus -> componentVersionStatus.componentVersionLink)
                    .collect(Collectors.toList());
        } else if (NotificationType.RULE_VIOLATION_CLEARED == reducedNotificationView.type) {
            final RuleViolationClearedNotificationView ruleViolationClearedNotificationView = (RuleViolationClearedNotificationView) reducedNotificationView;
            final RuleViolationClearedNotificationContent ruleViolationClearedNotificationContent = ruleViolationClearedNotificationView.content;
            projectVersionLinks = Arrays.asList(ruleViolationClearedNotificationContent.projectVersionLink);
            componentVersionLinks = ruleViolationClearedNotificationContent.componentVersionStatuses.stream()
                    .map(componentVersionStatus -> componentVersionStatus.componentVersionLink)
                    .collect(Collectors.toList());
        } else if (NotificationType.POLICY_OVERRIDE == reducedNotificationView.type) {
            final PolicyOverrideNotificationView policyOverrideNotificationView = (PolicyOverrideNotificationView) reducedNotificationView;
            final PolicyOverrideNotificationContent policyOverrideNotificationContent = policyOverrideNotificationView.content;
            projectVersionLinks = Arrays.asList(policyOverrideNotificationContent.projectVersionLink);
            componentVersionLinks = Arrays.asList(policyOverrideNotificationContent.componentVersionLink);
        } else {
            projectVersionLinks = new ArrayList<>();
            componentVersionLinks = new ArrayList<>();
        }

        final List<ProjectVersionComponentVersionModel> projectVersionComponentVersionModels = projectVersionLinks.stream()
                .filter(projectVersionLink -> projectVersionLinksToLookFor.containsKey(projectVersionLink))
                .flatMap(projectVersionLink -> componentVersionLinks.stream()
                        .flatMap(componentVersionLink -> Stream.of(createProjectVersionComponentVersionModel(projectVersionLink, componentVersionLink))))
                .collect(Collectors.toList());

        return projectVersionComponentVersionModels;
    }

    public List<ProjectVersionComponentVersionModel> getProjectVersionComponentVersionModelsFromNotifications(final List<ReducedNotificationView> notificationViews, final List<ProjectVersionView> projectVersionViews) {
        final Map<String, ProjectVersionView> projectVersionLinksToLookFor = projectVersionViews.stream().collect(Collectors.toMap(it -> it._meta.href, it -> it));

        final ArrayList<ProjectVersionComponentVersionModel> projectVersionComponentVersionModels = notificationViews
                .stream()
                .filter(view -> NotificationType.VULNERABILITY == view.type
                        || NotificationType.RULE_VIOLATION == view.type
                        || NotificationType.RULE_VIOLATION_CLEARED == view.type
                        || NotificationType.POLICY_OVERRIDE == view.type)
                .map(notificationView -> transformToProjectVersionComponentVersionModels(notificationView, projectVersionLinksToLookFor))
                .collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll);

        return projectVersionComponentVersionModels;
    }

    // not a good practice, but right now, I do not know a better way, short of searching the entire BOM, to match up a BOM component with a component/version
    // ejk - 2018-01-15
    public String getProjectVersionComponentLink(final ProjectVersionView projectVersionView, final ComponentVersionView componentVersionView) {
        final String componentVersionLink = componentVersionView._meta.href;
        final String projectVersionLink = projectVersionView._meta.href;
        final String apiComponentsLinkPrefix = "/api/components/";
        final int apiComponentsStart = componentVersionLink.indexOf(apiComponentsLinkPrefix) + apiComponentsLinkPrefix.length();
        return projectVersionLink + "/components/" + componentVersionLink.substring(apiComponentsStart);
    }

}
