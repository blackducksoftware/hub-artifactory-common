package com.synopsys.integration.blackduck.artifactory.policy;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.artifactory.exception.CancelException;
import org.artifactory.repo.RepoPath;

import com.synopsys.integration.blackduck.api.generated.enumeration.PolicySummaryStatusType;
import com.synopsys.integration.blackduck.artifactory.ArtifactoryPropertyService;
import com.synopsys.integration.blackduck.artifactory.BlackDuckArtifactoryProperty;
import com.synopsys.integration.blackduck.artifactory.analytics.AnalyticsCollector;
import com.synopsys.integration.blackduck.artifactory.analytics.Analyzable;
import com.synopsys.integration.blackduck.artifactory.analytics.FeatureAnalyticsCollector;

public class PolicyModule implements Analyzable {
    private final PolicyModuleConfig policyModuleConfig;
    private final ArtifactoryPropertyService artifactoryPropertyService;
    private final FeatureAnalyticsCollector featureAnalyticsCollector;

    public PolicyModule(final PolicyModuleConfig policyModuleConfig, final ArtifactoryPropertyService artifactoryPropertyService, final FeatureAnalyticsCollector featureAnalyticsCollector) {
        this.policyModuleConfig = policyModuleConfig;
        this.artifactoryPropertyService = artifactoryPropertyService;
        this.featureAnalyticsCollector = featureAnalyticsCollector;
    }

    public void handleBeforeDownloadEvent(final RepoPath repoPath) throws CancelException {
        String reason = null;
        BlockReason blockReason = BlockReason.NO_BLOCK;
        if (shouldCancelOnPolicyViolation(repoPath)) {
            reason = "because it violates a policy in your Black Duck Hub.";
            blockReason = BlockReason.IN_VIOLATION;
        } else if (shouldCancelOnMetadataBlock(repoPath)) {
            reason = "because it lacks BlackDuck metadata";
            blockReason = BlockReason.METADATA_BLOCK;
        }

        featureAnalyticsCollector.logFeatureHit("handleBeforeDownloadEvent", blockReason.toString());

        if (reason != null) {
            throw new CancelException(String.format("BlackDuck PolicyModule has prevented the download of %s %s", repoPath.toPath(), reason), 403);
        }
    }

    @Override
    public List<AnalyticsCollector> getAnalyticsCollectors() {
        return Arrays.asList(featureAnalyticsCollector);
    }

    private boolean shouldCancelOnPolicyViolation(final RepoPath repoPath) {
        final Optional<String> policyStatusProperty = artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS);

        return policyStatusProperty
                   .filter(policyStatus -> policyStatus.equalsIgnoreCase(PolicySummaryStatusType.IN_VIOLATION.name()))
                   .isPresent();
    }

    private boolean shouldCancelOnMetadataBlock(final RepoPath repoPath) {
        final boolean missingMetadata = !artifactoryPropertyService.getProperty(repoPath, BlackDuckArtifactoryProperty.POLICY_STATUS).isPresent();
        final boolean shouldBlock = policyModuleConfig.isMetadataBlockEnabled();

        return shouldBlock && missingMetadata;
    }

    private enum BlockReason {
        IN_VIOLATION,
        METADATA_BLOCK,
        NO_BLOCK
    }
}
