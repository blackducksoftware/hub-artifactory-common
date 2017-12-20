package com.blackducksoftware.integration.hub.artifactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.api.view.MetaHandler;
import com.blackducksoftware.integration.hub.model.view.ComponentVersionView;
import com.blackducksoftware.integration.hub.model.view.ProjectVersionView;
import com.blackducksoftware.integration.hub.model.view.VulnerableComponentView;
import com.blackducksoftware.integration.hub.service.HubService;

public class ArtifactMetaDataManager {
    private final Logger logger = LoggerFactory.getLogger(ArtifactMetaDataManager.class);

    public List<ArtifactMetaData> getMetaData(final HubService hubService, final ProjectVersionView projectVersionView) throws IntegrationException {
        final Map<String, ArtifactMetaData> idToArtifactMetaData = new HashMap<>();

        final List<VulnerableComponentView> vulnerableComponentViews = hubService.getAllViewsFromLink(projectVersionView, MetaHandler.VULNERABLE_COMPONENTS_LINK, VulnerableComponentView.class);
        for (final VulnerableComponentView vulnerableComponentView : vulnerableComponentViews) {
            final String forge = vulnerableComponentView.componentVersionOriginName;
            final String originId = vulnerableComponentView.componentVersionOriginId;
            if (StringUtils.isNotBlank(forge) && StringUtils.isNotBlank(originId)) {
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
        }

        final List<VersionBomComponentRevisedView> versionBomComponents = hubService.getAllViewsFromLink(projectVersionView, MetaHandler.COMPONENTS_LINK, VersionBomComponentRevisedView.class);
        versionBomComponents.forEach(versionBomComponent -> {
            final String componentVersionLink = versionBomComponent.componentVersion;
            try {
                final ComponentVersionView componentVersionView = hubService.getView(componentVersionLink, ComponentVersionView.class);
                final List<OriginView> originViews = hubService.getAllViewsFromLinkSafely(componentVersionView, "origins", OriginView.class);
                originViews.forEach(originView -> {
                    final String forge = originView.originName;
                    final String originId = originView.originId;
                    if (StringUtils.isNotBlank(forge) && StringUtils.isNotBlank(originId)) {
                        final String policyStatus = versionBomComponent.policyStatus;
                        if (!idToArtifactMetaData.containsKey(key(forge, originId))) {
                            final ArtifactMetaData artifactMetaData = new ArtifactMetaData();
                            artifactMetaData.forge = forge;
                            artifactMetaData.originId = originId;
                            idToArtifactMetaData.put(key(forge, originId), artifactMetaData);
                        }

                        idToArtifactMetaData.get(key(forge, originId)).policyStatus = policyStatus;
                    }
                });
            } catch (final IntegrationException e) {
                logger.error(String.format("Couldn't get data from %s: %s", componentVersionLink, e.getMessage()));
            }
        });

        return new ArrayList<>(idToArtifactMetaData.values());
    }

    private String key(final String forge, final String originId) {
        return forge + ":" + originId;
    }
}
