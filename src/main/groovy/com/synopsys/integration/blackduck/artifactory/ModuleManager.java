package com.synopsys.integration.blackduck.artifactory;

import java.io.IOException;

import org.artifactory.exception.CancelException;
import org.artifactory.fs.ItemInfo;
import org.artifactory.repo.RepoPath;

import com.synopsys.integration.blackduck.artifactory.analytics.AnalyticsModule;
import com.synopsys.integration.blackduck.artifactory.inspect.InspectionModule;
import com.synopsys.integration.blackduck.artifactory.inspect.InspectionModuleConfig;
import com.synopsys.integration.blackduck.artifactory.policy.PolicyModule;
import com.synopsys.integration.blackduck.artifactory.scan.ScanModule;
import com.synopsys.integration.blackduck.artifactory.scan.ScanModuleConfig;

/**
 * This class is the public API for the blackDuckPlugin groovy script.
 * Changing this interface should be avoided if possible and any changes
 * made here must be reflected in the blackDuckPlugin.groovy file
 * in hub-artifactory
 */
public class ModuleManager {
    private final ScanModule scanModule;
    private final InspectionModule inspectionModule;
    private final PolicyModule policyModule;
    private final AnalyticsModule analyticsModule;

    public ModuleManager(final ScanModule scanModule, final InspectionModule inspectionModule, final PolicyModule policyModule, final AnalyticsModule analyticsModule) {
        this.scanModule = scanModule;
        this.inspectionModule = inspectionModule;
        this.policyModule = policyModule;
        this.analyticsModule = analyticsModule;
    }

    public ScanModuleConfig getScanModuleConfig() {
        return scanModule.getScanModuleConfig();
    }

    public void triggerScan(final TriggerType triggerType) throws IOException {
        scanModule.triggerScan(triggerType);
    }

    public void addPolicyStatus(final TriggerType triggerType) throws IOException {
        scanModule.addPolicyStatus(triggerType);
    }

    public void deleteScanProperties(final TriggerType triggerType) {
        scanModule.deleteScanProperties(triggerType);
    }

    public void deleteScanPropertiesFromFailures(final TriggerType triggerType) {
        scanModule.deleteScanPropertiesFromFailures(triggerType);
    }

    public void deleteScanPropertiesFromOutOfDate(final TriggerType triggerType) {
        scanModule.deleteScanPropertiesFromOutOfDate(triggerType);
    }

    public void updateDeprecatedScanProperties(final TriggerType triggerType) {
        scanModule.updateDeprecatedProperties(triggerType);
    }

    public void updateDeprecatedInspectionProperties(final TriggerType triggerType) {
        inspectionModule.updateDeprecatedProperties(triggerType);
    }

    public void handleAfterCreateEvent(final ItemInfo itemInfo, final TriggerType triggerType) {
        inspectionModule.handleAfterCreateEvent(itemInfo, triggerType);
    }

    public String getStatusCheckMessage(final TriggerType triggerType) {
        return scanModule.getStatusCheckMessage(triggerType);
    }

    public InspectionModuleConfig getInspectionModuleConfig() {
        return inspectionModule.getInspectionModuleConfig();
    }

    public void identifyArtifacts(final TriggerType triggerType) {
        inspectionModule.identifyArtifacts(triggerType);
    }

    public void populateMetadata(final TriggerType triggerType) {
        inspectionModule.populateMetadata(triggerType);
    }

    public void updateMetadata(final TriggerType triggerType) {
        inspectionModule.updateMetadata(triggerType);
    }

    public void deleteInspectionProperties(final TriggerType triggerType) {
        inspectionModule.deleteInspectionProperties(triggerType);
    }

    public void handleBeforeDownloadEvent(final RepoPath repoPath) throws CancelException {
        policyModule.handleBeforeDownloadEvent(repoPath);
    }

    public void submitAnalytics(final TriggerType triggerType) {
        analyticsModule.submitAnalytics(triggerType);
    }
}
