package com.blackducksoftware.integration.hub.artifactory;

import java.net.URISyntaxException;
import java.net.URL;

import org.apache.http.client.utils.URIBuilder;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.service.model.UriCombiner;

public class ArtifactoryUriCombiner extends UriCombiner {
    @Override
    public String pieceTogetherUri(final URL baseUrl, final String path) throws IntegrationException {
        String uri;
        final String normalizedPath = path.startsWith("/") ? path : "/" + path;
        try {
            final URIBuilder uriBuilder = new URIBuilder(baseUrl.toURI());
            uriBuilder.setPath(normalizedPath);
            uri = uriBuilder.build().toString();
        } catch (final URISyntaxException e) {
            throw new IntegrationException(e.getMessage(), e);
        }
        return uri;
    }

}
