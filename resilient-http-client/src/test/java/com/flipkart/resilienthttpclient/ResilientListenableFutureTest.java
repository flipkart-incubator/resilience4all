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

package com.flipkart.resilienthttpclient;

import com.flipkart.resilienthttpclient.exceptions.ServerSideException;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.Retry.Metrics;
import io.github.resilience4j.retry.RetryConfig;
import lombok.SneakyThrows;
import org.asynchttpclient.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.asynchttpclient.Dsl.asyncHttpClient;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;

public class ResilientListenableFutureTest {

  //  @ClassRule
  //  public static WireMockClassRule wireMockRule = new
  // WireMockClassRule(wireMockConfig().dynamicPort());

  private final AsyncHttpClient defaultClient = asyncHttpClient();
  private final String url_delaysTwice = "/hello_10000_delaysTwice";
  private final String url_5xx = "/5xx";
  private final String url_5xx_failsTwice = "/5xx_failsTwice";
  @Rule public WireMockRule service = new WireMockRule(wireMockConfig().dynamicPort());

  @Before
  public void before() {
    service.stubFor(get(urlEqualTo("/hello")).willReturn(aResponse().withBody("World")));
    service.stubFor(
        get(urlEqualTo("/hello_1000"))
            .willReturn(aResponse().withFixedDelay(1000).withBody("World")));
    service.stubFor(
        get(urlEqualTo("/hello_10000"))
            .willReturn(aResponse().withFixedDelay(10000).withBody("World")));
    service.stubFor(
        get(urlEqualTo("/hello_100000"))
            .willReturn(aResponse().withFixedDelay(100000).withBody("World")));

    // Specific case to delay a request twice
    service.stubFor(
        get(urlEqualTo(url_delaysTwice))
            .inScenario("DELAYS_TWICE")
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse().withFixedDelay(10000).withBody("World"))
            .willSetStateTo("ONE_REQUEST_SERVED"));
    service.stubFor(
        get(urlEqualTo(url_delaysTwice))
            .inScenario("DELAYS_TWICE")
            .whenScenarioStateIs("ONE_REQUEST_SERVED")
            .willReturn(aResponse().withFixedDelay(10000).withBody("World"))
            .willSetStateTo("TWO_REQUEST_SERVED"));
    service.stubFor(
        get(urlEqualTo(url_delaysTwice))
            .inScenario("DELAYS_TWICE")
            .whenScenarioStateIs("TWO_REQUEST_SERVED")
            .willReturn(aResponse().withBody("World")));

    // Internal Server Error
    service.stubFor(get(urlEqualTo(url_5xx)).willReturn(aResponse().withStatus(500)));
    // Specific case to delay a request twice
    service.stubFor(
        get(urlEqualTo(url_5xx_failsTwice))
            .inScenario(url_5xx_failsTwice)
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse().withStatus(500))
            .willSetStateTo("ONE_REQUEST_SERVED"));
    service.stubFor(
        get(urlEqualTo(url_5xx_failsTwice))
            .inScenario(url_5xx_failsTwice)
            .whenScenarioStateIs("ONE_REQUEST_SERVED")
            .willReturn(aResponse().withStatus(500))
            .willSetStateTo("TWO_REQUEST_SERVED"));
    service.stubFor(
        get(urlEqualTo(url_5xx_failsTwice))
            .inScenario(url_5xx_failsTwice)
            .whenScenarioStateIs("TWO_REQUEST_SERVED")
            .willReturn(aResponse().withBody("World")));
  }

  // ###############################################
  // ############ Private Utiliy functions #########
  // ###############################################

  private Retry createRetry() {
    return Retry.of(
        "Test",
        RetryConfig.custom()
            .retryExceptions(ServerSideException.class, TimeoutException.class)
            .maxAttempts(4)
            .build());
  }

  private ResilientListenableFuture<Response> createResilientListenableFuture() {
    return createResilientListenableFuture(createRequest_default());
  }

  private ResilientListenableFuture<Response> createResilientListenableFuture(Request request) {
    return new ResilientListenableFuture<>(
        defaultClient,
        request,
        completionStageSupplier -> completionStageSupplier,
        AsyncCompletionHandlerBase::new);
  }

  private ResilientListenableFuture<Response> createResilientListenableFuture(Retry retry) {
    return createResilientListenableFuture(retry, createRequest_default());
  }

  private ResilientListenableFuture<Response> createResilientListenableFuture(
      Retry retry, Request request) {
    return new ResilientListenableFuture<>(
        defaultClient,
        request,
        completionStageSupplier ->
            Retry.decorateCompletionStage(
                retry, Executors.newSingleThreadScheduledExecutor(), completionStageSupplier),
        AsyncCompletionHandlerBase::new);
  }

  private void assertRetryMetrics(
      Metrics retryMetrics,
      int successWithoutRetry,
      int successWithRetry,
      int failureWithoutRetry,
      int failureWithRetry) {
    assertEquals(retryMetrics.getNumberOfSuccessfulCallsWithoutRetryAttempt(), successWithoutRetry);
    assertEquals(retryMetrics.getNumberOfSuccessfulCallsWithRetryAttempt(), successWithRetry);
    assertEquals(retryMetrics.getNumberOfFailedCallsWithoutRetryAttempt(), failureWithoutRetry);
    assertEquals(retryMetrics.getNumberOfFailedCallsWithRetryAttempt(), failureWithRetry);
  }

  private void assertRetryMetrics_empty(Metrics retryMetrics) {
    assertRetryMetrics(retryMetrics, 0, 0, 0, 0);
  }

  private String getServiceBaseUrl() {
    return "http://localhost:" + service.port();
  }

  private Request createRequest_default() {
    return Dsl.get(getServiceBaseUrl() + "/hello").build();
  }

  private Request createRequest_timeout() {
    return Dsl.get(getServiceBaseUrl() + "/hello_10000").setRequestTimeout(1000).build();
  }

  private Request createRequest_5xx() {
    return Dsl.get(getServiceBaseUrl() + url_5xx).build();
  }

  private Request createRequest_delaysTwice() {
    return Dsl.get(getServiceBaseUrl() + url_delaysTwice).setRequestTimeout(1000).build();
  }

  private Request createRequest_5xx_failsTwice() {
    return Dsl.get(getServiceBaseUrl() + url_5xx_failsTwice).build();
  }

  // ###############################################
  // ############ Tests start from here ############
  // ###############################################

  // #######
  // Future.get()
  // #######

  @Test
  @SneakyThrows
  public void testFutureGet__happyPath() {
    final ResilientListenableFuture<Response> future = createResilientListenableFuture();

    assertEquals("World", future.get().getResponseBody());
  }

  @Test
  @SneakyThrows
  public void testFutureGet__happyPath__withRetry() {
    final Retry retry = Retry.ofDefaults("test");
    final Metrics retryMetrics = retry.getMetrics();
    final ResilientListenableFuture<Response> future = createResilientListenableFuture(retry);

    // Let's assert retry is fine
    assertRetryMetrics_empty(retryMetrics);

    assertEquals("World", future.get().getResponseBody());

    assertRetryMetrics(retryMetrics, 1, 0, 0, 0);
  }

  @Test
  @SneakyThrows
  public void testFutureGet__twoFailures__withRetry() {
    final Retry retry = createRetry();
    final Metrics retryMetrics = retry.getMetrics();

    final Request timeoutRequest = createRequest_delaysTwice();
    final ResilientListenableFuture<Response> future =
        createResilientListenableFuture(retry, timeoutRequest);

    // Let's assert retry is fine
    assertRetryMetrics_empty(retryMetrics);

    try {
      future.get();
    } catch (Exception ignored) {
    }

    assertRetryMetrics(retryMetrics, 0, 1, 0, 0);

    service.verify(3, getRequestedFor(urlEqualTo(url_delaysTwice)));
  }

  @Test
  @SneakyThrows
  public void testFutureGet__allFailures__withRetry() {
    final Retry retry = createRetry();
    final Metrics retryMetrics = retry.getMetrics();

    final Request timeoutRequest = createRequest_timeout();
    final ResilientListenableFuture<Response> future =
        createResilientListenableFuture(retry, timeoutRequest);

    // Let's assert retry is fine
    assertRetryMetrics_empty(retryMetrics);

    try {
      future.get();
    } catch (Exception e) {
      assertThat(e.getCause(), instanceOf(TimeoutException.class));
    }

    assertRetryMetrics(retryMetrics, 0, 0, 0, 1);
    service.verify(4, getRequestedFor(urlEqualTo("/hello_10000")));
  }

  // #######
  // Future.abort()
  // #######

  @Test
  @SneakyThrows
  public void testFutureAbort() {
    final ResilientListenableFuture<Response> future = createResilientListenableFuture();

    future.abort(new RuntimeException("Aborted by test"));

    try {
      future.get();
      fail("Should never reach here.");
    } catch (Exception e) {
      assertThat(e, instanceOf(ExecutionException.class));
      assertEquals("Aborted by test", e.getCause().getMessage());
    }
  }

  @Test
  @SneakyThrows
  public void testFutureAbort__withRetry() {
    final Retry retry = createRetry();

    final Metrics retryMetrics = retry.getMetrics();

    final ResilientListenableFuture<Response> future = createResilientListenableFuture(retry);

    // Let's assert retry is fine
    assertRetryMetrics_empty(retryMetrics);

    future.abort(new RuntimeException("Aborted by test"));

    try {
      future.get();
      fail("Should never reach here.");
    } catch (Exception e) {
      assertThat(e, instanceOf(ExecutionException.class));
      assertEquals("Aborted by test", e.getCause().getMessage());
    }
    assertRetryMetrics(retryMetrics, 0, 0, 1, 0);
  }

  // #######
  // Future.cancel()
  // #######

  @Test
  @SneakyThrows
  public void testFutureCancel() {
    final ResilientListenableFuture<Response> future = createResilientListenableFuture();

    future.cancel(true);
    assert future.isCancelled();

    try {
      future.get();
      fail("Should never reach here.");
    } catch (Exception e) {
      assertThat(e, instanceOf(CancellationException.class));
    }
  }

  @Test
  @SneakyThrows
  public void testFutureCancel__withRetry() {
    final Retry retry = createRetry();
    final Metrics retryMetrics = retry.getMetrics();
    final ResilientListenableFuture<Response> future = createResilientListenableFuture(retry);

    // Let's assert retry is fine
    assertRetryMetrics_empty(retryMetrics);

    future.cancel(true);
    assert future.isCancelled();
    try {
      future.get();
      fail("Should never reach here.");
    } catch (Exception e) {
      assertThat(e, instanceOf(CancellationException.class));
    }
    assertRetryMetrics(retryMetrics, 0, 0, 1, 0);
  }

  // #######
  // Future.done()
  // #######

  @Test
  @SneakyThrows
  public void testFutureDone() {
    final ResilientListenableFuture<Response> future = createResilientListenableFuture();

    future.done();

    final Response response = future.get();
    assertNull(response);
  }

  @Test
  @SneakyThrows
  public void testFutureDone__withRetry() {
    final Retry retry = createRetry();
    final Metrics retryMetrics = retry.getMetrics();
    final ResilientListenableFuture<Response> future = createResilientListenableFuture(retry);

    // Let's assert retry is fine
    assertRetryMetrics_empty(retryMetrics);

    future.done();

    final Response response = future.get();
    assertNull(response);
    assertRetryMetrics(retryMetrics, 1, 0, 0, 0);
  }

  // #######
  // Future.touch()
  // Not testing as there is nothing to test in it.
  // #######

  // #######
  // Future.addListener()
  // #######

  @Test
  @SneakyThrows
  public void testAddListener() {
    final ResilientListenableFuture<Response> future = createResilientListenableFuture();
    final Runnable runnable = mock(Runnable.class);
    future.addListener(runnable, null);

    future.get();

    Mockito.verify(runnable, timeout(5000).times(1)).run();
  }

  @Test
  @SneakyThrows
  public void testAddListener__success__withRetry() {
    final Retry retry = createRetry();
    final Metrics retryMetrics = retry.getMetrics();
    final ResilientListenableFuture<Response> future =
        createResilientListenableFuture(retry, createRequest_delaysTwice());

    final Runnable runnable = mock(Runnable.class);
    future.addListener(runnable, null);

    assertRetryMetrics_empty(retryMetrics);
    future.get();

    Mockito.verify(runnable, timeout(5000).times(1)).run();
  }

  @Test
  @SneakyThrows
  public void testAddListener__failure__withRetry() {
    final Retry retry = createRetry();
    final Metrics retryMetrics = retry.getMetrics();
    final ResilientListenableFuture<Response> future =
        createResilientListenableFuture(retry, createRequest_timeout());

    final Runnable runnable = mock(Runnable.class);
    future.addListener(runnable, null);

    assertRetryMetrics_empty(retryMetrics);
    try {
      future.get();
      fail("Should never reach here.");
    } catch (Exception e) {
      assertThat(e.getCause(), instanceOf(TimeoutException.class));
    }

    Mockito.verify(runnable, timeout(5000).times(1)).run();
  }

  // #######
  // Future.toCompletableFuture()
  // Not testing as it is better tested using addListener
  // #######
}
