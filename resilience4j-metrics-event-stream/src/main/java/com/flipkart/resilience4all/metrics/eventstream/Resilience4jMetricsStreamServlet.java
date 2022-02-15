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

package com.flipkart.resilience4all.metrics.eventstream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.resilience4all.resilience4j.timer.TimerRegistry;
import com.netflix.hystrix.metric.consumer.HystrixDashboardStream;
import com.netflix.hystrix.metric.consumer.HystrixDashboardStream.DashboardData;
import com.netflix.hystrix.serial.SerialHystrixDashboardData;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.metrics.Timer;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Func1;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Resilience4jMetricsStreamServlet extends Resilience4jSampleSseServlet {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(Resilience4jMetricsStreamServlet.class);
  private static final long serialVersionUID = -7548505095303313237L;

  /* used to track number of connections and throttle */
  private static final AtomicInteger concurrentConnections = new AtomicInteger(0);
  private static final int DEFAULT_PAUSE_POLLER_THREAD_DELAY_IN_MS = 1000;

  /* package-private */ Resilience4jMetricsStreamServlet(Observable<String> sampleStream) {
    super(sampleStream);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  protected int getMaxNumberConcurrentConnectionsAllowed() {
    return 5;
  }

  @Override
  protected int getNumberCurrentConnections() {
    return concurrentConnections.get();
  }

  @Override
  protected int incrementAndGetCurrentConcurrentConnections() {
    return concurrentConnections.incrementAndGet();
  }

  @Override
  protected void decrementCurrentConcurrentConnections() {
    concurrentConnections.decrementAndGet();
  }

  public static class Builder {
    private final ObjectMapper objectMapper = new ObjectMapper();
    Map<String, CircuitBreaker> circuitBreakers = new HashMap<>();
    Map<String, Bulkhead> bulkheads = new HashMap<>();
    Map<String, ThreadPoolBulkhead> threadPoolBulkheads = new HashMap<>();
    Map<String, Retry> retries = new HashMap<>();
    Map<String, TimeLimiter> timeLimiters = new HashMap<>();
    Map<String, Timer> clientTimers = new HashMap<>();
    Map<String, Timer> serverTimers = new HashMap<>();
    CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
    BulkheadRegistry bulkheadRegistry = BulkheadRegistry.ofDefaults();
    ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry = ThreadPoolBulkheadRegistry.ofDefaults();
    RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
    TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
    TimerRegistry timerRegistry = TimerRegistry.ofDefaults();

    public Builder andCircuitBreaker(CircuitBreaker circuitBreaker) {
      circuitBreakers.put(circuitBreaker.getName(), circuitBreaker);
      return this;
    }

    public Builder andBulkhead(Bulkhead bulkhead) {
      bulkheads.put(bulkhead.getName(), bulkhead);
      return this;
    }

    public Builder andThreadPoolBulkhed(ThreadPoolBulkhead bulkhead) {
      threadPoolBulkheads.put(bulkhead.getName(), bulkhead);
      return this;
    }

    public Builder andRetry(Retry retry) {
      retries.put(retry.getName(), retry);
      return this;
    }

    public Builder andTimeLimiter(TimeLimiter timeLimiter) {
      timeLimiters.put(timeLimiter.getName(), timeLimiter);
      return this;
    }

    public Builder withCircuitBreakerRegistry(CircuitBreakerRegistry circuitBreakerRegistry) {
      this.circuitBreakerRegistry = circuitBreakerRegistry;
      return this;
    }

    public Builder withBulkheadRegistry(BulkheadRegistry bulkheadRegistry) {
      this.bulkheadRegistry = bulkheadRegistry;
      return this;
    }

    public Builder withThreadPoolBulkheadRegistry(
        ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry) {
      this.threadPoolBulkheadRegistry = threadPoolBulkheadRegistry;
      return this;
    }

    public Builder withRetryRegistry(RetryRegistry registry) {
      this.retryRegistry = registry;
      return this;
    }

    public Builder withTimeLimiterRegistry(TimeLimiterRegistry timeLimiterRegistry) {
      this.timeLimiterRegistry = timeLimiterRegistry;
      return this;
    }

    public Builder withTimerRegistry(TimerRegistry registry) {
      this.timerRegistry = registry;
      return this;
    }

    public Resilience4jMetricsStreamServlet build() {
      final Observable<List<HystrixMetrics>> commandObservable =
          Observable.interval(1, TimeUnit.SECONDS)
              .map(
                  i -> {
                    final HashMap<String, CircuitBreaker> circuitBreakerMap =
                        new HashMap<>(circuitBreakers);
                    for (CircuitBreaker circuitBreaker :
                        circuitBreakerRegistry.getAllCircuitBreakers()) {
                      circuitBreakerMap.put(circuitBreaker.getName(), circuitBreaker);
                    }

                    final HashMap<String, Bulkhead> bulkheadMap = new HashMap<>(bulkheads);
                    for (Bulkhead bulkhead : bulkheadRegistry.getAllBulkheads()) {
                      bulkheadMap.put(bulkhead.getName(), bulkhead);
                    }

                    final HashMap<String, ThreadPoolBulkhead> threadPoolBulkheadMap =
                        new HashMap<>(threadPoolBulkheads);
                    for (ThreadPoolBulkhead threadPoolBulkhead :
                        threadPoolBulkheadRegistry.getAllBulkheads()) {
                      threadPoolBulkheadMap.put(threadPoolBulkhead.getName(), threadPoolBulkhead);
                    }

                    final HashMap<String, Retry> retryMap = new HashMap<>(retries);
                    for (Retry retry : retryRegistry.getAllRetries()) {
                      retryMap.put(retry.getName(), retry);
                    }

                    final HashMap<String, TimeLimiter> timeLimiterMap = new HashMap<>(timeLimiters);
                    for (TimeLimiter timeLimiter : timeLimiterRegistry.getAllTimeLimiters()) {
                      timeLimiterMap.put(timeLimiter.getName(), timeLimiter);
                    }

                    final HashMap<String, Timer> clientTimerMap = new HashMap<>();
                    final HashMap<String, Timer> serverTimerMap = new HashMap<>();
                    final Map<String, Timer> allTimers = timerRegistry.getAllTimers();
                    for (String timerName : allTimers.keySet()) {
                      if (timerName.endsWith(".client")) {
                        final String[] split = timerName.split(".client");
                        final String s = split[0];
                        clientTimerMap.put(s, allTimers.get(timerName));
                      }
                      if (timerName.endsWith(".server")) {
                        final String[] split = timerName.split(".server");
                        final String s = split[0];
                        serverTimerMap.put(s, allTimers.get(timerName));
                      }
                    }

                    final Set<String> keySet = new HashSet<>();
                    keySet.addAll(circuitBreakerMap.keySet());
                    keySet.addAll(bulkheadMap.keySet());
                    keySet.addAll(threadPoolBulkheadMap.keySet());
                    keySet.addAll(retryMap.keySet());
                    keySet.addAll(clientTimerMap.keySet());
                    keySet.addAll(serverTimerMap.keySet());
                    keySet.addAll(timeLimiterMap.keySet());

                    Stream<HystrixCommandLikeMetrics> hystrixCommandLikeMetricsStream =
                        keySet.stream()
                            .map(
                                key ->
                                    new HystrixCommandLikeMetrics(
                                        key,
                                        circuitBreakerMap.get(key),
                                        bulkheadMap.get(key),
                                        threadPoolBulkheadMap.get(key),
                                        retryMap.get(key),
                                        timeLimiterMap.get(key),
                                        clientTimerMap.get(key),
                                        serverTimerMap.get(key)));

                    Stream<HystrixThreadPoolLikeMetrics> hystrixThreadPoolLikeMetricsStream =
                        threadPoolBulkheadMap.keySet().stream()
                            .map(
                                key ->
                                    new HystrixThreadPoolLikeMetrics(
                                        key,
                                        threadPoolBulkheadMap.get(key),
                                        circuitBreakerMap.get(key)));

                    return Stream.concat(
                            hystrixCommandLikeMetricsStream, hystrixThreadPoolLikeMetricsStream)
                        .collect(Collectors.toList());
                  });
      final Observable<HystrixMetrics> commandObservableMap =
          commandObservable.flatMap(Observable::from);
      final Observable<String> serializedCommandObservable =
          commandObservableMap.map(
              m -> {
                try {
                  return objectMapper.writeValueAsString(m);
                } catch (Exception e) {
                  LOGGER.warn("Error in resilience.stream", e);
                  return "";
                }
              });

      final Observable<String> hystrixObservable =
          HystrixDashboardStream.getInstance()
              .observe()
              .concatMap(
                  (Func1<DashboardData, Observable<String>>)
                      dashboardData ->
                          Observable.from(
                              SerialHystrixDashboardData.toMultipleJsonStrings(dashboardData)));

      final Observable<String> finalObservable =
          serializedCommandObservable.mergeWith(hystrixObservable).share().onBackpressureDrop();

      return new Resilience4jMetricsStreamServlet(finalObservable);
    }
  }
}
