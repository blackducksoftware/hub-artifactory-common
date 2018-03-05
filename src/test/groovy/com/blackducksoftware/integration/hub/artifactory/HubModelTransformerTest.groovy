package com.blackducksoftware.integration.hub.artifactory

import org.junit.Assert
import org.junit.Test

import com.blackducksoftware.integration.hub.api.generated.component.ResourceMetadata
import com.blackducksoftware.integration.hub.api.generated.view.ComponentVersionView
import com.blackducksoftware.integration.hub.api.generated.view.ProjectVersionView

class HubModelTransformerTest {
    @Test
    void testCreatingProjectVersionComponentLink() {
        HubModelTransformer hubModelTransformer = new HubModelTransformer(null, null)
        String projectVersionLink = 'https://int-hub04.dc1.lan/api/projects/19569890-08e9-4a4f-af7e-b28709a05f90/versions/525fd05c-ecc3-40fc-9368-fa11ac6f7ef3'
        def projectVersionView = new ProjectVersionView();
        def projectVersionViewMeta = new ResourceMetadata();
        projectVersionViewMeta.href = projectVersionLink;
        projectVersionView.meta = projectVersionViewMeta;

        String componentVersionLink = 'https://int-hub04.dc1.lan/api/components/dc3dee66-4939-4dea-b22f-ead288b4f117/versions/f9e2e6ff-7340-4fb3-a29f-a6fa98a10bfe'
        def componentVersionView = new ComponentVersionView();
        def componentVersionViewMeta = new ResourceMetadata();
        componentVersionViewMeta.href = componentVersionLink;
        componentVersionView.meta = componentVersionViewMeta;

        String expectedProjectVersionComponentLink = projectVersionLink + '/components/dc3dee66-4939-4dea-b22f-ead288b4f117/versions/f9e2e6ff-7340-4fb3-a29f-a6fa98a10bfe'
        Assert.assertEquals(expectedProjectVersionComponentLink, hubModelTransformer.getProjectVersionComponentLink(projectVersionView, componentVersionView))
    }
}
