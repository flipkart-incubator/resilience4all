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

package com.flipkart.resilienthttpclient.example;

import com.codahale.metrics.MetricRegistry;
import com.flipkart.resilience4all.metrics.eventstream.Resilience4jMetricsStreamServlet;
import com.flipkart.resilience4all.resilience4j.timer.TimerRegistry;
import com.flipkart.resilienthttpclient.ResilientDomain;
import com.flipkart.resilienthttpclient.example.core.ExternalService;
import com.flipkart.resilienthttpclient.example.core.ExternalServiceClient;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.dropwizard.setup.Environment;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.asynchttpclient.AsyncHttpClient;

import javax.inject.Named;
import java.util.concurrent.Executors;

import static org.asynchttpclient.Dsl.asyncHttpClient;

public class ExampleGuiceModule implements Module {

  private String template;

  public ExampleGuiceModule() {}

  @Override
  public void configure(Binder binder) {}

  @Provides
  @Named("template")
  public String providesTemplate(ExampleConfiguration configuration) {
    return configuration.getTemplate();
  }

  @Provides
  @Singleton
  public MetricRegistry provideRegistry(Environment environment) {
    return environment.metrics();
  }

  @Provides
  @Singleton
  public CircuitBreakerRegistry providesCircuitBreakerRegistry() {
    return CircuitBreakerRegistry.ofDefaults();
  }

  @Provides
  @Singleton
  public BulkheadRegistry providesBulkheadRegistry() {
    return BulkheadRegistry.ofDefaults();
  }

  @Provides
  @Singleton
  public ThreadPoolBulkheadRegistry providesThreadPoolBulkheadRegistry() {
    return ThreadPoolBulkheadRegistry.ofDefaults();
  }

  @Provides
  @Singleton
  public RetryRegistry providesRetryRegistry() {
    return RetryRegistry.ofDefaults();
  }

  @Provides
  @Singleton
  public TimerRegistry providesTimerRegistry(MetricRegistry metricRegistry) {
    return TimerRegistry.ofMetricRegistry(metricRegistry);
  }

  @Provides
  @Singleton
  public TimeLimiterRegistry providesTimeLimiterRegistry(MetricRegistry metricRegistry) {
    return TimeLimiterRegistry.ofDefaults();
  }

  @Provides
  @Singleton
  public ExternalService providesExternalService(
      MetricRegistry metricRegistry,
      CircuitBreakerRegistry circuitBreakerRegistry,
      RetryRegistry retryRegistry,
      BulkheadRegistry bulkheadRegistry,
      TimerRegistry timerRegistry) {
    final AsyncHttpClient normalAsyncHttpClient = asyncHttpClient();

    final AsyncHttpClient resilientDomain =
        ResilientDomain.builder("externalService")
            .asyncHttpClient(normalAsyncHttpClient)
            .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
            .circuitBreakerRegistry(circuitBreakerRegistry)
            .retryConfig(RetryConfig.ofDefaults(), Executors.newScheduledThreadPool(1))
            .retryRegistry(retryRegistry)
            .bulkheadConfig(BulkheadConfig.ofDefaults())
            .bulkheadRegistry(bulkheadRegistry)
            .maxContentLengthInBytes(1024 * 2) // Setting it to 2KB
            .metricRegistry(metricRegistry)
            .timerRegistry(timerRegistry)
            .build();

    return new ExternalServiceClient(resilientDomain);
  }

  @Provides
  @Singleton
  public Resilience4jMetricsStreamServlet providesResilience4jMetricsStreamServlet(
      CircuitBreakerRegistry circuitBreakerRegistry,
      BulkheadRegistry bulkheadRegistry,
      ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry,
      RetryRegistry retryRegistry,
      TimeLimiterRegistry timeLimiterRegistry,
      TimerRegistry timerRegistry) {
    return Resilience4jMetricsStreamServlet.builder()
        .withCircuitBreakerRegistry(circuitBreakerRegistry)
        .withBulkheadRegistry(bulkheadRegistry)
        .withThreadPoolBulkheadRegistry(threadPoolBulkheadRegistry)
        .withRetryRegistry(retryRegistry)
        .withTimeLimiterRegistry(timeLimiterRegistry)
        .withTimerRegistry(timerRegistry)
        .build();
  }
}
