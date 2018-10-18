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
package com.synopsys.integration.blackduck.artifactory.analytics;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AnalyticsCollector {

    protected static Map<String, String> convertMapValueToString(final Map<String, ?> map) {
        return map.entrySet().stream()
                   .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toString()));
    }

    /**
     * A utility method for combining maps where the key and value are strings.
     * @param map1
     * @param map2
     * @return A combined map. Throws an {@code IllegalStateException} if there are duplicate keys. See {@link Collectors#toMap(java.util.function.Function, java.util.function.Function)}
     */
    protected static Map<String, String> joinMaps(final Map<String, String> map1, final Map<String, String> map2) {
        return Stream.of(map1, map2)
                   .map(Map::entrySet)
                   .flatMap(Collection::stream)
                   .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public abstract Map<String, String> getMetadataMap();

    public abstract void clear();
}
