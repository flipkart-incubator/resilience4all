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

package com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.module;

import com.codahale.metrics.MetricRegistry;
import com.flipkart.resilience4all.metrics.eventstream.Resilience4jMetricsStreamServlet;
import com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.ExampleConfiguration;
import com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.service.DummyGetUserService;
import com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.service.DummyListUsersService;
import com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.service.GetUserService;
import com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.service.ListUsersService;
import com.flipkart.resilience4all.resilience4j.timer.TimerRegistry;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.dropwizard.setup.Environment;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;

import javax.inject.Named;

public class ExampleGuiceModule implements Module {

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
  public MetricRegistry providesRegistry(Environment environment) {
    return environment.metrics();
  }

  @Provides
  @Singleton
  public CircuitBreakerRegistry providesCircuitBreakerRegistry() {

    CircuitBreakerConfig circuitBreakerConfig =
        CircuitBreakerConfig.custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
            .slidingWindowSize(10)
            .build();

    return CircuitBreakerRegistry.of(circuitBreakerConfig);
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
  @Named("default")
  public ListUsersService providesListUsersService() {
    return new DummyListUsersService();
  }

  @Provides
  @Singleton
  @Named("default")
  public GetUserService providesGetUserService() {
    return new DummyGetUserService();
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
