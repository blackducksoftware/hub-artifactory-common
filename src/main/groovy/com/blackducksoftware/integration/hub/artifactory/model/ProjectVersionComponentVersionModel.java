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

import com.blackducksoftware.integration.hub.artifactory.view.OriginView;
import com.blackducksoftware.integration.hub.artifactory.view.VersionBomComponentRevisedView;
import com.blackducksoftware.integration.hub.model.view.ComponentVersionView;
import com.blackducksoftware.integration.hub.model.view.ProjectVersionView;

public class ProjectVersionComponentVersionModel {
    public ProjectVersionView projectVersionView;
    public VersionBomComponentRevisedView versionBomComponentRevisedView;
    public ComponentVersionView componentVersionView;
    public List<OriginView> originViews;

    public ProjectVersionComponentVersionModel() {
    }

    public ProjectVersionComponentVersionModel(final ProjectVersionView projectVersionView, final VersionBomComponentRevisedView versionBomComponentRevisedView, final ComponentVersionView componentVersionView,
            final List<OriginView> originViews) {
        this.projectVersionView = projectVersionView;
        this.versionBomComponentRevisedView = versionBomComponentRevisedView;
        this.componentVersionView = componentVersionView;
        this.originViews = originViews;
    }

}
