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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.apache.http.HttpHeaders;
import org.apache.iceberg.BaseTable;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.TableMetadata;
import org.apache.iceberg.TableOperations;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.rest.HTTPRequest.HTTPMethod;
import org.apache.iceberg.rest.responses.LoadTableResponse;
import org.junit.jupiter.api.Test;

class TestRESTCatalogAdapter {
  private static final TableIdentifier TABLE = TableIdentifier.of("ns", "t");

  @Test
  void loadTableOmitsETagWhenMetadataLocationIsAbsent() {
    TableMetadata metadata =
        TableMetadata.newTableMetadata(
            SCHEMA, PartitionSpec.unpartitioned(), "file:/tmp/t", Map.of());
    TableOperations ops = mock(TableOperations.class);
    when(ops.current()).thenReturn(metadata);
    Catalog catalog = mock(Catalog.class);
    when(catalog.loadTable(TABLE)).thenReturn(new BaseTable(ops, TABLE.toString()));

    RESTCatalogAdapter adapter = new RESTCatalogAdapter(catalog);
    HTTPRequest request =
        adapter.buildRequest(
            HTTPMethod.GET,
            ResourcePaths.forCatalogProperties(Map.of()).table(TABLE),
            Map.of(),
            Map.of(),
            null);
    Map<String, String> responseHeaders = Maps.newHashMap();

    LoadTableResponse response =
        adapter.execute(
            request,
            LoadTableResponse.class,
            ErrorHandlers.tableErrorHandler(),
            responseHeaders::putAll);

    assertThat(response.metadataLocation()).isNull();
    assertThat(responseHeaders).doesNotContainKey(HttpHeaders.ETAG);
  }
}
