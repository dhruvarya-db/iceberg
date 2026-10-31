/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iceberg.rest;

import static org.apache.iceberg.TestBase.SCHEMA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.TableMetadata;
import org.apache.iceberg.TableProperties;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableMap;
import org.apache.iceberg.rest.responses.ErrorResponse;
import org.apache.iceberg.rest.responses.LoadTableResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;

class TestRESTTableOperations {
  private static final String PATH = "v1/namespaces/ns/tables/t";

  @Test
  void refreshAdoptsFormatVersion4ResponseWithoutMetadataLocation() {
    RESTTableOperations ops = opsReturningResponsesWithoutMetadataLocation(4);

    assertThat(ops.refresh().properties()).containsEntry("version", "1");
    assertThat(ops.refresh().properties()).containsEntry("version", "2");
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  void refreshKeepsMetadataForPreV4ResponseWithoutMetadataLocation(int formatVersion) {
    RESTTableOperations ops = opsReturningResponsesWithoutMetadataLocation(formatVersion);

    assertThat(ops.refresh().properties()).doesNotContainKey("version");
    assertThat(ops.refresh().properties()).doesNotContainKey("version");
  }

  private static RESTTableOperations opsReturningResponsesWithoutMetadataLocation(
      int formatVersion) {
    TableMetadata initial =
        TableMetadata.newTableMetadata(
            SCHEMA,
            PartitionSpec.unpartitioned(),
            "file:/tmp/t",
            Map.of(TableProperties.FORMAT_VERSION, String.valueOf(formatVersion)));
    TableMetadata first = withVersion(initial, "1");
    TableMetadata second = withVersion(initial, "2");

    RESTClient client = mock(RESTClient.class);
    when(client.get(
            anyString(),
            anyMap(),
            eq(LoadTableResponse.class),
            ArgumentMatchers.<Supplier<Map<String, String>>>any(),
            ArgumentMatchers.<Consumer<ErrorResponse>>any()))
        .thenReturn(loadResponse(first), loadResponse(second));

    return new RESTTableOperations(
        client, PATH, Map::of, null, initial, Set.of(Endpoint.V1_LOAD_TABLE));
  }

  private static TableMetadata withVersion(TableMetadata base, String version) {
    return TableMetadata.buildFrom(base).setProperties(ImmutableMap.of("version", version)).build();
  }

  private static LoadTableResponse loadResponse(TableMetadata metadata) {
    return LoadTableResponse.builder().withTableMetadata(metadata).build();
  }
}
