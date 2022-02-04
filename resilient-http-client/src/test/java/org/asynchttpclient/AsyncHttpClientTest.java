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

package org.asynchttpclient;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.asynchttpclient.Dsl.asyncHttpClient;
import static org.junit.Assert.assertEquals;

/*
This test file is written to understand the behavior of AsyncHttpClient; therefore it is @Ignore'd.
 */

@Ignore
public class AsyncHttpClientTest {
  @ClassRule
  public static WireMockClassRule wireMockRule =
      new WireMockClassRule(wireMockConfig().dynamicPort());

  @Rule public WireMockClassRule service = wireMockRule;

  private String getServiceBaseUrl() {
    return "http://localhost:" + service.port();
  }

  @Test
  public void testAsyncHttpClient() throws ExecutionException, InterruptedException {
    service.stubFor(get(urlEqualTo("/hello")).willReturn(aResponse().withBody("World")));
    Request request = org.asynchttpclient.Dsl.get(getServiceBaseUrl() + "/hello").build();
    ListenableFuture<String> future =
        asyncHttpClient().executeRequest(request, new MyAsyncHttpHandler());
    assertEquals(future.get(), "World");
  }
}

class MyAsyncHttpHandler extends AsyncCompletionHandler<String> {

  @Override
  public String onCompleted(Response response) throws Exception {
    throw new RuntimeException("Let's see this");
  }
}
