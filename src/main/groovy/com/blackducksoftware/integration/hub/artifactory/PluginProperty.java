package com.blackducksoftware.integration.hub.artifactory;

public enum PluginProperty {
    HUB_URL("hub.url"),
    HUB_API_KEY("hub.api.key"),
    HUB_TIMEOUT("hub.timeout"),
    HUB_PROXY_HOST("hub.proxy.host"),
    HUB_PROXY_PORT("hub.proxy.port"),
    HUB_PROXY_USERNAME("hub.proxy.username"),
    HUB_PROXY_PASSWORD("hub.proxy.password"),
    HUB_ALWAYS_TRUST_CERT("hub.trust.cert"),

    HUB_ARTIFACTORY_INSPECT_REPOS("hub.artifactory.inspect.repos"),
    HUB_ARTIFACTORY_INSPECT_REPOS_CSV_PATH("hub.artifactory.inspect.repos.csv.path"),
    HUB_ARTIFACTORY_INSPECT_PATTERNS_RUBYGEMS("hub.artifactory.inspect.patterns.rubygems"),
    HUB_ARTIFACTORY_INSPECT_PATTERNS_MAVEN("hub.artifactory.inspect.patterns.maven"),
    HUB_ARTIFACTORY_INSPECT_PATTERNS_GRADLE("hub.artifactory.inspect.patterns.gradle"),
    HUB_ARTIFACTORY_INSPECT_PATTERNS_PYPI("hub.artifactory.inspect.patterns.pypi"),
    HUB_ARTIFACTORY_INSPECT_PATTERNS_NUGET("hub.artifactory.inspect.patterns.nuget"),
    HUB_ARTIFACTORY_INSPECT_PATTERNS_NPM("hub.artifactory.inspect.patterns.npm"),
    HUB_ARTIFACTORY_INSPECT_DATE_TIME_PATTERN("hub.artifactory.inspect.date.time.pattern"),
    HUB_ARTIFACTORY_INSPECT_CRON_LOG_VERBOSE("hub.artifactory.inspect.cron.log.verbose"),

    HUB_ARTIFACTORY_SCAN_REPOS("hub.artifactory.scan.repos"),
    HUB_ARTIFACTORY_SCAN_REPOS_CSV_PATH("hub.artifactory.scan.repos.csv.path"),
    HUB_ARTIFACTORY_SCAN_NAME_PATTERNS("hub.artifactory.scan.name.patterns"),
    HUB_ARTIFACTORY_SCAN_BINARIES_DIRECTORY_PATH("hub.artifactory.scan.binaries.directory.path"),
    HUB_ARTIFACTORY_SCAN_MEMORY("hub.artifactory.scan.memory"),
    HUB_ARTIFACTORY_SCAN_DRY_RUN("hub.artifactory.scan.dry.run"),
    HUB_ARTIFACTORY_SCAN_DATE_TIME_PATTERN("hub.artifactory.scan.date.time.pattern"),
    HUB_ARTIFACTORY_SCAN_CUTOFF_DATE("hub.artifactory.scan.cutoff.date"),
    HUB_ARTIFACTORY_SCAN_CRON_LOG_VERBOSE("hub.artifactory.scan.cron.log.verbose");

    private final String key;

    private PluginProperty(final String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

}
