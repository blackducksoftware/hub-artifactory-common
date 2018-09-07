package com.synopsys.integration.blackduck.artifactory.scan;

import org.artifactory.fs.FileLayoutInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: This should be replaced with a property in artifactory to override project name and version
 * The intent of this was to allow customers to define their own project name / version name in the groovy script.
 * This being in hub-artifactory-common breaks the intended functionality
 */
class FileLayoutUtil {
    private static final Logger logger = LoggerFactory.getLogger(FileLayoutUtil.class);

    /**
     * Takes a FileLayoutInfo object and returns the project name as it will appear in the Hub. (By default, this returns the module of the FileLayoutInfo object)
     *
     * Feel free to modify this method to transform the FileLayoutInfo object as necessary to construct your desired project name.
     */
    static String getProjectNameFromFileLayoutInfo(final FileLayoutInfo fileLayoutInfo) {
        logger.info("Constructing project name...");

        final String constructedProjectName = fileLayoutInfo.getModule();

        logger.info("...project name constructed");
        return constructedProjectName;
    }

    /**
     * Takes a FileLayoutInfo object and returns the project version name for as it will appear in the Hub. (By default, this returns the baseRevision of the FileLayoutInfo object)
     *
     * Feel free to modify this method to transform the FileLayoutInfo object as necessary to construct your desired project version name.
     */
    static String getProjectVersionNameFromFileLayoutInfo(final FileLayoutInfo fileLayoutInfo) {
        logger.info("Constructing project version name...");

        final String constructedProjectVersionName = fileLayoutInfo.getBaseRevision();

        logger.info("...project version constructed");
        return constructedProjectVersionName;
    }
}
