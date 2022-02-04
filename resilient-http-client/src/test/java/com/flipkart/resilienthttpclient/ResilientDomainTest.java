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

import com.codahale.metrics.MetricRegistry;
import com.flipkart.resilienthttpclient.ResilientDomain.Builder;
import com.flipkart.resilienthttpclient.exceptions.ClientSideException;
import com.flipkart.resilienthttpclient.exceptions.MaxContentLengthExceededException;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.retry.RetryConfig;
import lombok.SneakyThrows;
import org.asynchttpclient.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.asynchttpclient.Dsl.asyncHttpClient;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class ResilientDomainTest {

  // Constants
  private final String url_default = "/hello";
  private final String url_1000 = "/hello_1000";
  private final String url_delaysTwice = "/hello_10000_delaysTwice";
  private final String url_5xx = "/5xx";
  private final String url_5xx_failsTwice = "/5xx_failsTwice";
  @Rule public WireMockRule service = new WireMockRule(wireMockConfig().dynamicPort());
  @Rule public TestName testName = new TestName();
  private MetricRegistry metricRegistry;

  @Before
  public void before() {
    // Initialize new MetricRegistry
    metricRegistry = new MetricRegistry();

    // Initialize WireMock
    service.stubFor(get(urlEqualTo(url_default)).willReturn(aResponse().withBody("World")));
    service.stubFor(
        get(urlEqualTo(url_1000)).willReturn(aResponse().withFixedDelay(1000).withBody("World")));
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

  private String getServiceBaseUrl() {
    return "http://localhost:" + service.port();
  }

  private Request createRequest_default() {
    return Dsl.get(getServiceBaseUrl() + url_default).build();
  }

  private Request createRequest_1000() {
    return Dsl.get(getServiceBaseUrl() + url_1000).build();
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

  private CircuitBreakerConfig createCircuitBreakerConfig_default() {
    return CircuitBreakerConfig.custom()
        .minimumNumberOfCalls(1)
        .slidingWindow(1, 1, SlidingWindowType.COUNT_BASED)
        .waitDurationInOpenState(Duration.ofSeconds(60))
        .failureRateThreshold(1)
        .build();
  }

  private BulkheadConfig createBulkheadConfig__default() {
    return BulkheadConfig.custom()
        .maxConcurrentCalls(1)
        .maxWaitDuration(Duration.ofMillis(10))
        .build();
  }

  private BulkheadConfig createBulkheadConfig__waitFor5Seconds() {
    return BulkheadConfig.custom()
        .maxConcurrentCalls(1)
        .maxWaitDuration(Duration.ofMillis(5000))
        .build();
  }

  private RetryConfig createRetryConfig__default() {
    return RetryConfig.custom().maxAttempts(4).build();
  }

  private AsyncHttpClient createAsyncHttpClient(CircuitBreakerConfig circuitBreakerConfig) {
    return createAsyncHttpClient(circuitBreakerConfig, null, null);
  }

  private AsyncHttpClient createAsyncHttpClient(BulkheadConfig bulkheadConfig) {
    return createAsyncHttpClient(null, bulkheadConfig, null);
  }

  private AsyncHttpClient createAsyncHttpClient(RetryConfig retryConfig) {
    return createAsyncHttpClient(null, null, retryConfig);
  }

  private AsyncHttpClient createAsyncHttpClient(
      CircuitBreakerConfig circuitBreakerConfig,
      BulkheadConfig bulkheadConfig,
      RetryConfig retryConfig) {
    Builder builder =
        ResilientDomain.builder(testName.getMethodName())
            .circuitBreakerConfig(circuitBreakerConfig)
            .bulkheadConfig(bulkheadConfig)
            .asyncHttpClient(asyncHttpClient())
            .metricRegistry(metricRegistry);
    if (Objects.nonNull(retryConfig)) {
      builder = builder.retryConfig(retryConfig, Executors.newScheduledThreadPool(1));
    }
    return builder.build();
  }

  // ASSERTS

  private Response assertRespone(ListenableFuture<Response> future)
      throws InterruptedException, ExecutionException {
    final Response response = future.get();
    assertEquals("World", response.getResponseBody());
    return response;
  }

  private Response assert5xx(ListenableFuture<Response> future)
      throws InterruptedException, ExecutionException {
    final Response response = future.get();
    assertEquals(500, response.getStatusCode());
    return response;
  }

  private void assertClientSideException(ListenableFuture<Response> future) {
    assertThrowsException(future, ClientSideException.class);
  }

  private <T> void assertThrowsException(ListenableFuture<Response> future, Class<T> eClass) {
    try {
      future.get();
      fail("Should never reach here."); // should never reach here
    } catch (Exception e) {
      assertThat(e.getCause(), instanceOf(eClass));
    }
  }

  private void assert__circuitBreaker__state(int state) {
    final Object actualState =
        metricRegistry
            .getGauges()
            .get("resilience4j.circuitbreaker." + testName.getMethodName() + ".state")
            .getValue();

    assertEquals(state, actualState);
  }

  private void assert__circuitBreaker__successful(int i) {
    final Object actualI =
        metricRegistry
            .getGauges()
            .get("resilience4j.circuitbreaker." + testName.getMethodName() + ".successful")
            .getValue();
    assertEquals(i, actualI);
  }

  private void assert__circuitBreaker__failed(int i) {
    final Object actualI =
        metricRegistry
            .getGauges()
            .get("resilience4j.circuitbreaker." + testName.getMethodName() + ".failed")
            .getValue();
    assertEquals(i, actualI);
  }

  private void assert__circuitBreaker__notPermitted(int i) {
    final Object actualI =
        metricRegistry
            .getGauges()
            .get("resilience4j.circuitbreaker." + testName.getMethodName() + ".not_permitted")
            .getValue();
    assertEquals(new Long(i), actualI);
  }

  private void assert__circuitBreaker__calls(int s, int f, int np, boolean isOpen) {
    assert__circuitBreaker__successful(s);
    assert__circuitBreaker__failed(f);
    assert__circuitBreaker__notPermitted(np);
    int state = isOpen ? 1 : 0;
    assert__circuitBreaker__state(state);
  }

  private void assert__bulkhead__availableCalls(int s) {
    final Object actualS =
        metricRegistry
            .getGauges()
            .get(
                "resilience4j.bulkhead." + testName.getMethodName() + ".available_concurrent_calls")
            .getValue();
    assertEquals(s, actualS);
  }

  private void assert__retry__calls(int s, int sr, int f, int fr) {
    final Object actualS =
        metricRegistry
            .getGauges()
            .get(
                "resilience4j.retry."
                    + testName.getMethodName()
                    + ".successful_calls_without_retry")
            .getValue();
    final Object actualSR =
        metricRegistry
            .getGauges()
            .get("resilience4j.retry." + testName.getMethodName() + ".successful_calls_with_retry")
            .getValue();
    final Object actualF =
        metricRegistry
            .getGauges()
            .get("resilience4j.retry." + testName.getMethodName() + ".failed_calls_without_retry")
            .getValue();
    final Object actualFR =
        metricRegistry
            .getGauges()
            .get("resilience4j.retry." + testName.getMethodName() + ".failed_calls_with_retry")
            .getValue();

    //    System.out.println("ROBIN " + testName.getMethodName() + " S " + actualS + " SR " +
    // actualSR + " F "+ actualF + " FR " +actualFR);
    assertEquals(new Long(s), actualS);
    assertEquals(new Long(sr), actualSR);
    assertEquals(new Long(f), actualF);
    assertEquals(new Long(fr), actualFR);
  }

  // ###############################################
  // ############ Tests start from here ############
  // ###############################################

  // ###############################################
  // ############ Circuit Breaker ##################
  // ###############################################

  @SneakyThrows
  @Test
  public void test__circuitBreaker__happyPath() {
    final CircuitBreakerConfig circuitBreakerConfig = createCircuitBreakerConfig_default();
    final AsyncHttpClient httpClient = createAsyncHttpClient(circuitBreakerConfig);

    assertRespone(httpClient.executeRequest(createRequest_default()));

    service.verify(1, getRequestedFor(urlEqualTo(url_default)));
    assert__circuitBreaker__calls(1, 0, 0, false);
    assert__retry__calls(1, 0, 0, 0);
  }

  @SneakyThrows
  @Test
  public void test__circuitBreaker__happyPath__prepare() {
    final CircuitBreakerConfig circuitBreakerConfig = createCircuitBreakerConfig_default();
    final AsyncHttpClient httpClient = createAsyncHttpClient(circuitBreakerConfig);

    assertRespone(httpClient.prepareGet(getServiceBaseUrl() + url_default).execute());

    service.verify(1, getRequestedFor(urlEqualTo(url_default)));
    assert__circuitBreaker__calls(1, 0, 0, false);
    assert__retry__calls(1, 0, 0, 0);
  }

  @SneakyThrows
  @Test
  public void test__circuitBreaker__circuitOpens() {
    final CircuitBreakerConfig circuitBreakerConfig = createCircuitBreakerConfig_default();
    final AsyncHttpClient httpClient = createAsyncHttpClient(circuitBreakerConfig);

    assert5xx(httpClient.executeRequest(createRequest_5xx()));

    Thread.sleep(1000); // Simply allowing circuitbreaker to do it's calculation

    service.verify(1, getRequestedFor(urlEqualTo(url_5xx)));
    assert__circuitBreaker__calls(0, 1, 0, true);
    assert__retry__calls(0, 0, 0, 1);

    // Next call should be not permitted
    assertThrowsException(
        httpClient.executeRequest(createRequest_default()), CallNotPermittedException.class);

    service.verify(0, getRequestedFor(urlEqualTo(url_default)));
    assert__circuitBreaker__calls(0, 1, 1, true);
    assert__retry__calls(0, 0, 0, 2);
  }

  @SneakyThrows
  @Test
  public void test__circuitBreaker__circuitNotOpensForClientSideException() {
    final CircuitBreakerConfig circuitBreakerConfig = createCircuitBreakerConfig_default();
    final AsyncHttpClient httpClient = createAsyncHttpClient(circuitBreakerConfig);

    final ListenableFuture<Response> future =
        httpClient.executeRequest(
            createRequest_default(),
            new AsyncCompletionHandlerBase() {
              @Override
              public Response onCompleted(Response response) throws Exception {
                throw new ClientSideException();
              }
            });

    assertClientSideException(future);
    assert__circuitBreaker__calls(0, 0, 0, false);

    // IMP: Below check ensures clientSideException is being correctly captured with maxAttempts=1.
    service.verify(1, getRequestedFor(urlEqualTo(url_default)));
    assert__retry__calls(0, 0, 1, 0);
  }

  // ###############################################
  // ############ Bulkhead #########################
  // ###############################################

  @Test
  @SneakyThrows
  public void test__bulkhead__happyPath() {
    final BulkheadConfig bulkheadConfig = createBulkheadConfig__default();
    final AsyncHttpClient httpClient = createAsyncHttpClient(bulkheadConfig);

    assertRespone(httpClient.executeRequest(createRequest_default()));

    service.verify(1, getRequestedFor(urlEqualTo(url_default)));
    assert__bulkhead__availableCalls(1);
  }

  @Test
  @SneakyThrows
  public void test__bulkhead__happyPath2() {
    final BulkheadConfig bulkheadConfig = createBulkheadConfig__default();
    final AsyncHttpClient httpClient = createAsyncHttpClient(bulkheadConfig);

    final ListenableFuture<Response> futureDefault =
        httpClient.executeRequest(createRequest_1000());

    assert__bulkhead__availableCalls(0); // Need to check this before waiting for response.
    assertRespone(futureDefault);

    service.verify(1, getRequestedFor(urlEqualTo(url_1000)));
    assert__bulkhead__availableCalls(1);
  }

  @Test
  @SneakyThrows
  public void test__bulkhead__restricts() {
    final BulkheadConfig bulkheadConfig = createBulkheadConfig__default();
    final AsyncHttpClient httpClient = createAsyncHttpClient(bulkheadConfig);

    final ListenableFuture<Response> future1 = httpClient.executeRequest(createRequest_1000());

    assert__bulkhead__availableCalls(0); // Need to check this before waiting for response.

    final ListenableFuture<Response> future2 = httpClient.executeRequest(createRequest_1000());

    assertRespone(future1);
    assert__bulkhead__availableCalls(1);

    try {
      future2.get();
      fail("Should never reach here.");
    } catch (ExecutionException e) {
      assertThat(e.getCause(), instanceOf(BulkheadFullException.class));
    }
    service.verify(1, getRequestedFor(urlEqualTo(url_1000)));
    assert__retry__calls(1, 0, 0, 1);
  }

  @Test
  @SneakyThrows
  public void test__bulkhead__restrictsUntilWaitDuration() {
    // This test case need not be written as it is expected behavior of bulkhead.
    // Yet I have written it just to showcase how it would work.
    final BulkheadConfig bulkheadConfig = createBulkheadConfig__waitFor5Seconds();
    final AsyncHttpClient httpClient = createAsyncHttpClient(bulkheadConfig);

    final ListenableFuture<Response> future1 = httpClient.executeRequest(createRequest_1000());

    assert__bulkhead__availableCalls(0); // Need to check this before waiting for response.

    final ListenableFuture<Response> future2 = httpClient.executeRequest(createRequest_1000());

    Thread.sleep(1500);

    assertRespone(future1);
    assertRespone(future2);

    service.verify(2, getRequestedFor(urlEqualTo(url_1000)));
    assert__bulkhead__availableCalls(1); // 1 now as both the futures have returned.
    assert__retry__calls(2, 0, 0, 0);
  }

  // ###############################################
  // ############ Retry ############################
  // ###############################################

  @Test
  @SneakyThrows
  public void test__retry__happyPath() {
    final RetryConfig retryConfig = createRetryConfig__default();
    final AsyncHttpClient httpClient = createAsyncHttpClient(retryConfig);
    assertRespone(httpClient.executeRequest(createRequest_default()));

    service.verify(1, getRequestedFor(urlEqualTo(url_default)));

    assert__retry__calls(1, 0, 0, 0);
  }

  @Test
  @SneakyThrows
  public void test__retry__twoFailures() {
    final RetryConfig retryConfig = createRetryConfig__default();
    final AsyncHttpClient httpClient = createAsyncHttpClient(retryConfig);
    assertRespone(httpClient.executeRequest(createRequest_5xx_failsTwice()));

    service.verify(3, getRequestedFor(urlEqualTo(url_5xx_failsTwice)));
    assert__retry__calls(0, 1, 0, 0);
  }

  @Test
  @SneakyThrows
  public void test__retry__allFailures() {
    final RetryConfig retryConfig = createRetryConfig__default();
    final AsyncHttpClient httpClient = createAsyncHttpClient(retryConfig);
    assert5xx(httpClient.executeRequest(createRequest_5xx()));

    service.verify(4, getRequestedFor(urlEqualTo(url_5xx)));
    assert__retry__calls(0, 0, 0, 1);
  }

  @Test
  @SneakyThrows
  public void test__retry__clientSideException() {
    final RetryConfig retryConfig = createRetryConfig__default();
    final AsyncHttpClient httpClient = createAsyncHttpClient(retryConfig);
    assertClientSideException(
        httpClient.executeRequest(
            createRequest_default(),
            new AsyncCompletionHandlerBase() {
              @Override
              public Response onCompleted(Response response) throws Exception {
                throw new ClientSideException();
              }
            }));

    service.verify(1, getRequestedFor(urlEqualTo(url_default)));
    assert__retry__calls(0, 0, 1, 0);
  }

  // ###############################################
  // ############ Bulkhead + Retry #################
  // ###############################################

  @Test
  @SneakyThrows
  public void test__bulkheadExceptionsAreRetried() {
    // Retry config will wait for 2 seconds before retrying
    final RetryConfig retryConfig =
        RetryConfig.custom().maxAttempts(2).waitDuration(Duration.ofSeconds(2)).build();

    // Bulkhead should throw a bulkhead exception for second call
    final BulkheadConfig bulkheadConfig = createBulkheadConfig__default();
    final AsyncHttpClient httpClient = createAsyncHttpClient(null, bulkheadConfig, retryConfig);

    // First request will make bulkheadFull
    // second request will get bulkheadFullException and be retried
    final ListenableFuture<Response> future1 = httpClient.executeRequest(createRequest_1000());
    final ListenableFuture<Response> future2 = httpClient.executeRequest(createRequest_default());
    assertRespone(future1);
    assertRespone(future2);

    service.verify(1, getRequestedFor(urlEqualTo(url_1000)));
    service.verify(1, getRequestedFor(urlEqualTo(url_default)));
    assert__retry__calls(1, 1, 0, 0);
  }

  // ###############################################
  // ############ Circuitbreaker + Retry ###########
  // ###############################################

  @Test
  @SneakyThrows
  public void test__circuitBreakerExceptionsAreRetried() {
    // TODO
    assert true;
  }

  // ###############################################
  // ############ Circuitbreaker + bulkhead ########
  // ###############################################

  @Test
  @SneakyThrows
  public void test__bulkheadRejectsBeforeCircuitBreaker() {
    // TODO
    assert true;
  }

  // ###############################################
  // ############ MaxContentLength #################
  // ###############################################
  // Testing only one case of MaxContentLength as detailed testing is done in
  // AsyncHandlerDecoratorForMaxContentLengthTest
  @Test
  @SneakyThrows
  public void test_maxContentLength() {
    final RetryConfig retryConfig = createRetryConfig__default();

    Builder builder =
        ResilientDomain.builder(testName.getMethodName())
            .asyncHttpClient(asyncHttpClient())
            .metricRegistry(metricRegistry)
            .retryConfig(retryConfig, Executors.newScheduledThreadPool(1))
            .maxContentLengthInBytes(3);
    final AsyncHttpClient httpClient = builder.build();

    try {
      httpClient.executeRequest(createRequest_default()).get();
      fail("Expecting MaxContentLengthExceededException to be thrown");
    } catch (ExecutionException e) {
      assertThat(e.getCause(), instanceOf(MaxContentLengthExceededException.class));
    }
    // Asserting that it is not retried.
    service.verify(1, getRequestedFor(urlEqualTo(url_default)));
    assert__retry__calls(0, 0, 1, 0);
  }
}
