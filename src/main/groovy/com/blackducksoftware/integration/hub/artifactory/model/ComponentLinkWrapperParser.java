package com.blackducksoftware.integration.hub.artifactory.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.blackducksoftware.integration.hub.api.generated.view.ProjectVersionView;
import com.blackducksoftware.integration.hub.api.generated.view.VersionBomComponentView;
import com.blackducksoftware.integration.hub.api.view.CommonNotificationState;

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

    public List<ComponentLinkWrapper> parseNotifications(final List<CommonNotificationState> commonNotifications, final List<ProjectVersionView> projectVersionViewsToLookFor) {
        projectVersionLinksToLookFor = projectVersionViewsToLookFor.stream().collect(Collectors.toMap(it -> it._meta.href, it -> it));
        final List<ComponentLinkWrapper> componentLinkWrappers = commonNotifications
                .stream()
                .filter(state -> state.getContent() != null && state.getContent().providesProjectComponentDetails())
                .map(state -> parseNotification(state))
                .collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll);

        return componentLinkWrappers;
    }

    public List<ComponentLinkWrapper> parseNotification(final CommonNotificationState commonNotification) {
        final List<ComponentLinkWrapper> componentLinkWrappers = new ArrayList<>();
        commonNotification.getContent().getNotificationContentLinks().forEach(link -> {
            if (projectVersionLinksToLookFor.containsKey(link.getProjectVersionLink())) {
                if (link.hasComponentVersion()) {
                    componentLinkWrappers.add(createComponentLinkWrapper(link.getProjectVersionLink(), link.getComponentVersionLink()));
                } else {
                    // this is likely NOT what we want to do but something that was hidden
                    // in the "current" impl. is that componentVersionLink can be null if
                    // the notification only has knowledge of the component and NOT the
                    // version so artifactory has to decide what to do with this
                    componentLinkWrappers.add(createComponentLinkWrapper(link.getProjectVersionLink(), link.getComponentLink()));
                }
            }
        });

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
