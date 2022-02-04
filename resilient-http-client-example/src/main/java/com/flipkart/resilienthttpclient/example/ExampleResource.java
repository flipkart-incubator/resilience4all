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
import com.codahale.metrics.annotation.Timed;
import com.flipkart.resilience4all.resilience4j.timer.TimerConfig;
import com.flipkart.resilience4all.resilience4j.timer.TimerRegistry;
import com.flipkart.resilienthttpclient.example.core.ExternalService;
import com.flipkart.resilienthttpclient.example.model.User;
import com.flipkart.resilienthttpclient.example.model.UserResponse;
import com.flipkart.resilienthttpclient.example.model.UuidResponse;
import com.netflix.hystrix.HystrixCommand.Setter;
import com.netflix.hystrix.HystrixCommandGroupKey.Factory;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.metrics.CircuitBreakerMetrics;
import io.github.resilience4j.metrics.RetryMetrics;
import io.github.resilience4j.metrics.ThreadPoolBulkheadMetrics;
import io.github.resilience4j.metrics.Timer;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.SneakyThrows;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class ExampleResource {

  private final String template;
  private final ExternalService externalService;

  private final String resilience4jCommand = "UuidResilience4jCommand";
  private final AtomicLong counter;
  private final CircuitBreaker circuitBreaker;
  private final ThreadPoolBulkhead bulkhead;
  private final Retry retry;
  private final Timer serverTimer;
  private final Timer clientTimer;

  @Inject
  public ExampleResource(
      @Named("template") String template,
      ExternalService externalService,
      MetricRegistry metricRegistry,
      CircuitBreakerRegistry circuitBreakerRegistry,
      ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry,
      RetryRegistry retryRegistry,
      TimerRegistry timerRegistry) {
    this.template = template;
    this.counter = new AtomicLong();
    this.externalService = externalService;

    circuitBreaker =
        circuitBreakerRegistry.circuitBreaker(
            resilience4jCommand,
            CircuitBreakerConfig.from(circuitBreakerRegistry.getDefaultConfig())
                .slidingWindowType(SlidingWindowType.TIME_BASED)
                .slidingWindowSize(10)
                .build());

    ThreadPoolBulkheadConfig config =
        ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(30)
            .coreThreadPoolSize(30)
            .queueCapacity(20)
            .build();

    bulkhead = threadPoolBulkheadRegistry.bulkhead(resilience4jCommand, config);

    retry = retryRegistry.retry(resilience4jCommand, RetryConfig.custom().maxAttempts(1).build());

    final String serverTimerName = resilience4jCommand + ".server";
    serverTimer =
        timerRegistry.timer(
            serverTimerName,
            TimerConfig.custom()
                .name("com.flipkart.resilience4all.example." + serverTimerName)
                .build());
    final String clientTimerName = resilience4jCommand + ".client";
    clientTimer =
        timerRegistry.timer(
            clientTimerName,
            TimerConfig.custom()
                .name("com.flipkart.resilience4all.example." + clientTimerName)
                .build());
    metricRegistry.registerAll(CircuitBreakerMetrics.ofCircuitBreaker(circuitBreaker));
    metricRegistry.registerAll(ThreadPoolBulkheadMetrics.ofBulkhead(bulkhead));
    metricRegistry.registerAll(RetryMetrics.ofRetry(retry));
  }

  @SneakyThrows
  @GET
  @Timed
  @Path("{id}")
  public UserResponse getUser(@PathParam("id") int id) {
    final Setter config = Setter.withGroupKey(Factory.asKey("RemoteServiceGroupThreadPool"));

    HystrixCommandProperties.Setter commandProperties = HystrixCommandProperties.Setter();
    commandProperties.withExecutionTimeoutInMilliseconds(10_000);
    config.andCommandPropertiesDefaults(commandProperties);
    config.andThreadPoolPropertiesDefaults(
        HystrixThreadPoolProperties.Setter()
            .withMaxQueueSize(10)
            .withCoreSize(3)
            .withQueueSizeRejectionThreshold(10));

    final String uuid = new UuidHystrixCommand(config).execute();

    final User user = externalService.getUser(id);
    final String greeting = String.format(template, user.getFirstName());

    return new UserResponse(counter.incrementAndGet(), uuid, greeting, user);
  }

  @SneakyThrows
  @GET
  @Timed
  @Path("/uuid")
  public UuidResponse getUuid() {
    final String resilience4jUuid = getResilience4jUuid();
    final String hystrixUuid = getHystrixThreadPoolUuid();
    final String hystrixSempahoreUuid = getHystrixSemaphoreUuid();

    return new UuidResponse(hystrixUuid, hystrixSempahoreUuid, resilience4jUuid);
  }

  @SneakyThrows
  private String getResilience4jUuid() {

    try {
      final Supplier<String> stringSupplier =
          Timer.decorateSupplier(serverTimer, UuidBusinessLogic::getUuid);

      final Supplier<String> supplier1 = circuitBreaker.decorateSupplier(stringSupplier);

      final Supplier<CompletionStage<String>> completionStageSupplier =
          ThreadPoolBulkhead.decorateSupplier(bulkhead, supplier1);

      final Supplier<CompletionStage<String>> completionStageSupplier1 =
          Retry.decorateCompletionStage(retry, null, completionStageSupplier);

      final Supplier<CompletionStage<String>> completionStageSupplier2 =
          Timer.decorateCompletionStageSupplier(clientTimer, completionStageSupplier1);

      return completionStageSupplier2.get().toCompletableFuture().get();
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  private String getHystrixThreadPoolUuid() {
    try {
      final Setter config =
          Setter.withGroupKey(Factory.asKey("UuidThreadPool"))
              .andCommandKey(HystrixCommandKey.Factory.asKey("UuidThreadPoolCommand"));

      HystrixCommandProperties.Setter commandProperties = HystrixCommandProperties.Setter();
      commandProperties.withExecutionTimeoutInMilliseconds(10_000);
      config.andCommandPropertiesDefaults(commandProperties);

      config.andThreadPoolPropertiesDefaults(
          HystrixThreadPoolProperties.Setter()
              .withMaxQueueSize(10)
              .withCoreSize(30)
              .withQueueSizeRejectionThreshold(10));

      return new UuidHystrixCommand(config).execute();
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  private String getHystrixSemaphoreUuid() {
    try {
      final Setter config =
          Setter.withGroupKey(Factory.asKey("UuidSempahore"))
              .andCommandKey(HystrixCommandKey.Factory.asKey("UuidSemaphoreCommand"));

      HystrixCommandProperties.Setter commandProperties = HystrixCommandProperties.Setter();
      commandProperties.withExecutionTimeoutInMilliseconds(10_000);
      commandProperties.withExecutionIsolationStrategy(ExecutionIsolationStrategy.SEMAPHORE);
      commandProperties.withExecutionIsolationSemaphoreMaxConcurrentRequests(30);
      config.andCommandPropertiesDefaults(commandProperties);

      return new UuidHystrixCommand(config).execute();
    } catch (Exception e) {
      return e.getMessage();
    }
  }
}
