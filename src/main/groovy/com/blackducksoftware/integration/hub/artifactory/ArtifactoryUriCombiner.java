/**
 * hub-artifactory-common
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.hub.artifactory;

import java.net.URISyntaxException;
import java.net.URL;

import org.apache.http.client.utils.URIBuilder;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.rest.UriCombiner;

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
