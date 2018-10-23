package com.synopsys.integration.blackduck.artifactory.inspect.metadata

import com.synopsys.integration.blackduck.api.UriSingleResponse
import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView
import com.synopsys.integration.blackduck.artifactory.modules.inspection.inspect.metadata.CompositeComponentManager
import org.junit.Assert
import org.junit.Test

class VersionBomComponentLinkTest {
    @Test
    void testCreatingVersionBomComponentLink() {
        CompositeComponentManager compositeComponentManager = new CompositeComponentManager(null, null);
        String projectVersionLink = 'https://int-hub04.dc1.lan/api/projects/19569890-08e9-4a4f-af7e-b28709a05f90/versions/525fd05c-ecc3-40fc-9368-fa11ac6f7ef3'
        String componentVersionLink = 'https://int-hub04.dc1.lan/api/components/dc3dee66-4939-4dea-b22f-ead288b4f117/versions/f9e2e6ff-7340-4fb3-a29f-a6fa98a10bfe'

        UriSingleResponse<ProjectVersionView> projectVersionViewUriResponse = new UriSingleResponse<>(projectVersionLink, ProjectVersionView.class);
        UriSingleResponse<ComponentVersionView> componentVersionViewUriResponse = new UriSingleResponse<>(componentVersionLink, ComponentVersionView.class);

        String expectedVersionBomComponentLink = projectVersionLink + '/components/dc3dee66-4939-4dea-b22f-ead288b4f117/versions/f9e2e6ff-7340-4fb3-a29f-a6fa98a10bfe'

        Assert.assertEquals(expectedVersionBomComponentLink, compositeComponentManager.getVersionBomComponentUriResponse(projectVersionViewUriResponse, componentVersionViewUriResponse).uri)
    }
}
