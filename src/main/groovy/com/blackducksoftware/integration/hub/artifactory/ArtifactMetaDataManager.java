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
import com.blackducksoftware.integration.hub.model.view.VulnerabilityView;
import com.blackducksoftware.integration.hub.service.HubService;

public class ArtifactMetaDataManager {
    private final Logger logger = LoggerFactory.getLogger(ArtifactMetaDataManager.class);

    public List<ArtifactMetaData> getMetaData(final HubService hubService, final ProjectVersionView projectVersionView) throws IntegrationException {
        final Map<String, ArtifactMetaData> idToArtifactMetaData = new HashMap<>();

        final List<VersionBomComponentRevisedView> versionBomComponents = hubService.getAllViewsFromLink(projectVersionView, MetaHandler.COMPONENTS_LINK, VersionBomComponentRevisedView.class);
        versionBomComponents.forEach(versionBomComponent -> {
            final String componentVersionLink = versionBomComponent.componentVersion;
            try {
                final ComponentVersionView componentVersionView = hubService.getView(componentVersionLink, ComponentVersionView.class);
                final List<OriginView> originViews = hubService.getAllViewsFromLinkSafely(componentVersionView, "origins", OriginView.class);
                originViews.forEach(originView -> {
                    final String forge = originView.originName;
                    final String originId = originView.originId;
                    if (StringUtils.isNotBlank(forge) && StringUtils.isNotBlank(originId) && !idToArtifactMetaData.containsKey(key(forge, originId))) {
                        final ArtifactMetaData artifactMetaData = new ArtifactMetaData();
                        artifactMetaData.forge = forge;
                        artifactMetaData.originId = originId;
                        artifactMetaData.policyStatus = versionBomComponent.policyStatus;
                        populateVulnerabilityCounts(artifactMetaData, versionBomComponent, hubService);

                        idToArtifactMetaData.put(key(forge, originId), artifactMetaData);
                    }
                });
            } catch (final IntegrationException e) {
                logger.error(String.format("Couldn't get data from %s: %s", componentVersionLink, e.getMessage()));
            }
        });

        return new ArrayList<>(idToArtifactMetaData.values());
    }

    private void populateVulnerabilityCounts(final ArtifactMetaData artifactMetaData, final VersionBomComponentRevisedView versionBomComponent, final HubService hubService) {
        final String vulnerabilitiesLink = hubService.getFirstLinkSafely(versionBomComponent, MetaHandler.VULNERABILITIES_LINK);
        try {
            final List<VulnerabilityView> componentVulnerabilities = hubService.getAllViews(vulnerabilitiesLink, VulnerabilityView.class);
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
            logger.error(String.format("Can't populate vulnerability counts for %s: %s", vulnerabilitiesLink, e.getMessage()));
        }
    }

    private String key(final String forge, final String originId) {
        return forge + ":" + originId;
    }
}
