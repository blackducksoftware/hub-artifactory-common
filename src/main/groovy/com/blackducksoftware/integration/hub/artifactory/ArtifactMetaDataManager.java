package com.blackducksoftware.integration.hub.artifactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.api.item.MetaService;
import com.blackducksoftware.integration.hub.model.view.ProjectVersionView;
import com.blackducksoftware.integration.hub.model.view.VulnerableComponentView;
import com.blackducksoftware.integration.hub.service.HubResponseService;

public class ArtifactMetaDataManager {
    public List<ArtifactMetaData> getMetaData(final HubResponseService hubResponseService, final ProjectVersionView projectVersionView) throws IntegrationException {
        final Map<String, ArtifactMetaData> idToArtifactMetaData = new HashMap<>();

        final List<VulnerableComponentView> vulnerableComponentViews = hubResponseService.getAllItemsFromLink(projectVersionView, MetaService.VULNERABLE_COMPONENTS_LINK, VulnerableComponentView.class);
        for (final VulnerableComponentView vulnerableComponentView : vulnerableComponentViews) {
            final String forge = vulnerableComponentView.componentVersionOriginName;
            final String originId = vulnerableComponentView.componentVersionOriginId;
            final String severity = vulnerableComponentView.vulnerabilityWithRemediation.severity;
            if (!idToArtifactMetaData.containsKey(key(forge, originId))) {
                final ArtifactMetaData artifactMetaData = new ArtifactMetaData();
                artifactMetaData.forge = forge;
                artifactMetaData.originId = originId;
                idToArtifactMetaData.put(key(forge, originId), artifactMetaData);
            }

            if ("LOW".equals(severity)) {
                idToArtifactMetaData.get(key(forge, originId)).lowSeverityCount++;
            } else if ("MEDIUM".equals(severity)) {
                idToArtifactMetaData.get(key(forge, originId)).mediumSeverityCount++;
            } else if ("HIGH".equals(severity)) {
                idToArtifactMetaData.get(key(forge, originId)).highSeverityCount++;
            }
        }

        final List<VersionBomComponentRevisedView> versionBomComponents = hubResponseService.getAllItemsFromLink(projectVersionView, MetaService.COMPONENTS_LINK, VersionBomComponentRevisedView.class);
        versionBomComponents.forEach(versionBomComponent -> {
            versionBomComponent.origins.forEach(origin -> {
                final String forge = origin.externalNamespace;
                final String originId = origin.externalId;
                final String policyStatus = versionBomComponent.policyStatus;
                if (!idToArtifactMetaData.containsKey(key(forge, originId))) {
                    final ArtifactMetaData artifactMetaData = new ArtifactMetaData();
                    artifactMetaData.forge = forge;
                    artifactMetaData.originId = originId;
                    idToArtifactMetaData.put(key(forge, originId), artifactMetaData);
                }

                idToArtifactMetaData.get(key(forge, originId)).policyStatus = policyStatus;
            });
        });

        return new ArrayList<>(idToArtifactMetaData.values());
    }

    private String key(final String forge, final String originId) {
        return forge + ":" + originId;
    }
}
