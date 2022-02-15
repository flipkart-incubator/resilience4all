/*
 * Copyright (c) 2022 [The original author]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flipkart.resilienthttpclient.example.core;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.resilienthttpclient.example.model.ExternalServiceResponse;
import com.flipkart.resilienthttpclient.example.model.User;
import lombok.SneakyThrows;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Response;

public class AnotherExternalServiceClient {
  private final AsyncHttpClient asyncHttpClient;
  private final ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public AnotherExternalServiceClient(AsyncHttpClient httpClient) {
    asyncHttpClient = httpClient;
  }

  @SneakyThrows
  public User getUser(int userId) {
    final Response response =
        asyncHttpClient.prepareGet("https://example-service.com/api/users/" + userId).execute().get();
    final ExternalServiceResponse externalServiceResponse =
        objectMapper.readValue(response.getResponseBody(), ExternalServiceResponse.class);
    return externalServiceResponse.getData();
  }
}
