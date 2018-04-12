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
package com.blackducksoftware.integration.hub.artifactory.model;

import java.util.List;
import java.util.stream.Collectors;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.api.generated.view.ComponentVersionView;
import com.blackducksoftware.integration.hub.api.generated.view.OriginView;
import com.blackducksoftware.integration.hub.api.generated.view.VersionBomComponentView;
import com.blackducksoftware.integration.hub.service.HubService;
import com.blackducksoftware.integration.log.IntLogger;

public class CompositeComponentManager {
    private final IntLogger intLogger;
    private final HubService hubService;

    public CompositeComponentManager(final IntLogger intLogger, final HubService hubService) {
        this.intLogger = intLogger;
        this.hubService = hubService;
    }

    public List<CompositeComponentModel> generateCompositeComponentModels(final List<ComponentLinkWrapper> componentLinkWrappers) {
        final List<CompositeComponentModel> compositeComponentModels;
        compositeComponentModels = componentLinkWrappers
                .stream()
                .map(compositeComponentUris -> generateCompositeComponentModel(compositeComponentUris))
                .collect(Collectors.toList());
        return compositeComponentModels;
    }

    public CompositeComponentModel generateCompositeComponentModel(final ComponentLinkWrapper componentLinkWrapper) {
        try {
            final String versionBomComponentLink = componentLinkWrapper.getVersionBomComponentLink();
            final String componentVersionLink = componentLinkWrapper.getComponentVersionLink();
            final ComponentVersionView componentVersionView = hubService.getResponse(componentVersionLink, ComponentVersionView.class);
            final VersionBomComponentView versionBomComponentView = hubService.getResponse(versionBomComponentLink, VersionBomComponentView.class);
            final List<OriginView> originViews = hubService.getAllResponses(componentVersionView, ComponentVersionView.ORIGINS_LINK_RESPONSE);
            return new CompositeComponentModel(versionBomComponentView, componentVersionView, originViews);
        } catch (final IntegrationException e) {
            intLogger.error(String.format("Could not create the CompositeComponentModel: %s", e.getMessage()), e);
        }

        return new CompositeComponentModel();
    }
}
