package com.blackducksoftware.integration.hub.artifactory.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.blackducksoftware.integration.hub.api.generated.enumeration.NotificationType;
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

public class ComponentLinkWrapperParser {
    private Map<String, ProjectVersionView> projectVersionLinksToLookFor;

    public ComponentLinkWrapperParser() {
        projectVersionLinksToLookFor = new HashMap<>();
    }

    public List<ComponentLinkWrapper> parseBom(final ProjectVersionView projectVersionView, final List<VersionBomComponentView> versionBomComponentViews) {
        final String projectVersionLink = projectVersionView._meta.href;
        final List<ComponentLinkWrapper> componentLinkWrappers = versionBomComponentViews
                .stream()
                .map(versionBomComponentView -> versionBomComponentView.componentVersion)
                .map(componentVersionLink -> createComponentLinkWrapper(projectVersionLink, componentVersionLink))
                .collect(Collectors.toList());
        return componentLinkWrappers;
    }

    public List<ComponentLinkWrapper> parseNotifications(final List<ReducedNotificationView> notificationViews, final List<ProjectVersionView> projectVersionViewsToLookFor) {
        projectVersionLinksToLookFor = projectVersionViewsToLookFor.stream().collect(Collectors.toMap(it -> it._meta.href, it -> it));
        final List<ComponentLinkWrapper> componentLinkWrappers = notificationViews
                .stream()
                .filter(view -> NotificationType.POLICY_OVERRIDE == view.type
                        || NotificationType.RULE_VIOLATION == view.type
                        || NotificationType.RULE_VIOLATION_CLEARED == view.type
                        || NotificationType.VULNERABILITY == view.type)
                .map(notificationView -> parseNotification(notificationView))
                .collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll);

        return componentLinkWrappers;
    }

    public List<ComponentLinkWrapper> parseNotification(final ReducedNotificationView reducedNotificationView) {
        final List<ComponentLinkWrapper> componentLinkWrappers;
        if (NotificationType.POLICY_OVERRIDE == reducedNotificationView.type) {
            componentLinkWrappers = parsePolicyOverrideNotification((PolicyOverrideNotificationView) reducedNotificationView);
        } else if (NotificationType.RULE_VIOLATION == reducedNotificationView.type) {
            componentLinkWrappers = parseRuleViolationNotification((RuleViolationNotificationView) reducedNotificationView);
        } else if (NotificationType.RULE_VIOLATION_CLEARED == reducedNotificationView.type) {
            componentLinkWrappers = parseRuleViolationClearedNotification((RuleViolationClearedNotificationView) reducedNotificationView);
        } else if (NotificationType.VULNERABILITY == reducedNotificationView.type) {
            componentLinkWrappers = parseVulnerabilityNotification((VulnerabilityNotificationView) reducedNotificationView);
        } else {
            componentLinkWrappers = new ArrayList<>();
        }

        return componentLinkWrappers;
    }

    public List<ComponentLinkWrapper> parsePolicyOverrideNotification(final PolicyOverrideNotificationView policyOverrideNotificationView) {
        final List<ComponentLinkWrapper> componentLinkWrappers = new ArrayList<>();
        final PolicyOverrideNotificationContent policyOverrideNotificationContent = policyOverrideNotificationView.content;
        final String projectVersionLink = policyOverrideNotificationContent.projectVersionLink;
        final String componentVersionLink = policyOverrideNotificationContent.componentVersionLink;
        if (projectVersionLinksToLookFor.containsKey(projectVersionLink)) {
            final ComponentLinkWrapper projectVersionComponentVersionModel = createComponentLinkWrapper(projectVersionLink, componentVersionLink);
            componentLinkWrappers.add(projectVersionComponentVersionModel);
        }
        return componentLinkWrappers;
    }

    public List<ComponentLinkWrapper> parseRuleViolationNotification(final RuleViolationNotificationView ruleViolationNotificationView) {
        List<ComponentLinkWrapper> componentLinkWrappers = new ArrayList<>();
        final RuleViolationNotificationContent ruleViolationNotificationContent = ruleViolationNotificationView.content;
        final String projectVersionLink = ruleViolationNotificationContent.projectVersionLink;
        if (projectVersionLinksToLookFor.containsKey(projectVersionLink)) {
            componentLinkWrappers = ruleViolationNotificationContent.componentVersionStatuses
                    .stream()
                    .map(componentVersionStatus -> componentVersionStatus.componentVersionLink)
                    .map(componentVersionLink -> createComponentLinkWrapper(projectVersionLink, componentVersionLink))
                    .collect(Collectors.toList());
        }
        return componentLinkWrappers;
    }

    public List<ComponentLinkWrapper> parseRuleViolationClearedNotification(final RuleViolationClearedNotificationView ruleViolationClearedNotificationView) {
        List<ComponentLinkWrapper> componentLinkWrappers = new ArrayList<>();
        final RuleViolationClearedNotificationContent ruleViolationClearedNotificationContent = ruleViolationClearedNotificationView.content;
        final String projectVersionLink = ruleViolationClearedNotificationContent.projectVersionLink;
        if (projectVersionLinksToLookFor.containsKey(projectVersionLink)) {
            componentLinkWrappers = ruleViolationClearedNotificationContent.componentVersionStatuses
                    .stream()
                    .map(componentVersionStatus -> componentVersionStatus.componentVersionLink)
                    .map(componentVersionLink -> createComponentLinkWrapper(projectVersionLink, componentVersionLink))
                    .collect(Collectors.toList());
        }
        return componentLinkWrappers;
    }

    public List<ComponentLinkWrapper> parseVulnerabilityNotification(final VulnerabilityNotificationView vulnerabilityNotificationView) {
        final VulnerabilityNotificationContent vulnerabilityNotificationContent = vulnerabilityNotificationView.content;
        final String componentVersionLink = vulnerabilityNotificationContent.componentVersionLink;
        final List<ComponentLinkWrapper> componentLinkWrappers = vulnerabilityNotificationContent.affectedProjectVersions
                .stream()
                .map(affectedProjectVersion -> affectedProjectVersion.projectVersion)
                .filter(projectVersionLink -> projectVersionLinksToLookFor.containsKey(projectVersionLink))
                .map(versionBomComponentLink -> createComponentLinkWrapper(componentVersionLink, versionBomComponentLink))
                .collect(Collectors.toList());
        return componentLinkWrappers;
    }

    private ComponentLinkWrapper createComponentLinkWrapper(final String projectVersionLink, final String componentVersionLink) {
        final String versionBomComponentLink = getVersionBomComponentLink(projectVersionLink, componentVersionLink);
        return new ComponentLinkWrapper(componentVersionLink, versionBomComponentLink);
    }

    // not a good practice, but right now, I do not know a better way, short of searching the entire BOM, to match up a BOM component with a component/version
    // ejk - 2018-01-15
    private String getVersionBomComponentLink(final String projectVersionLink, final String componentVersionLink) {
        final String apiComponentsLinkPrefix = "/api/components/";
        final int apiComponentsStart = componentVersionLink.indexOf(apiComponentsLinkPrefix) + apiComponentsLinkPrefix.length();
        return projectVersionLink + "/components/" + componentVersionLink.substring(apiComponentsStart);
    }

}
