package com.blackducksoftware.integration.hub.artifactory.scan;

import org.artifactory.fs.FileLayoutInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Talk to Rich about this. Not sure this class or its function is needed
public class FileLayoutUtil {
    private static final Logger logger = LoggerFactory.getLogger(FileLayoutUtil.class);

    /**
     * Takes a FileLayoutInfo object and returns the project name as it will appear in the Hub. (By default, this returns the module of the FileLayoutInfo object)
     *
     * Feel free to modify this method to transform the FileLayoutInfo object as necessary to construct your desired project name.
     */
    public static String getProjectNameFromFileLayoutInfo(final FileLayoutInfo fileLayoutInfo) {
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
    public static String getProjectVersionNameFromFileLayoutInfo(final FileLayoutInfo fileLayoutInfo) {
        logger.info("Constructing project version name...");

        final String constructedProjectVersionName = fileLayoutInfo.getBaseRevision();

        logger.info("...project version constructed");
        return constructedProjectVersionName;
    }
}
