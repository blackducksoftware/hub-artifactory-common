package com.blackducksoftware.integration.hub.artifactory.model;

public class ComponentLinkWrapper {
    private final String versionBomComponentLink;
    private final String componentVersionLink;

    public ComponentLinkWrapper(final String componentVersionLink, final String versionBomComponentLink) {
        this.componentVersionLink = componentVersionLink;
        this.versionBomComponentLink = versionBomComponentLink;
    }

    public String getVersionBomComponentLink() {
        return versionBomComponentLink;
    }

    public String getComponentVersionLink() {
        return componentVersionLink;
    }

}
