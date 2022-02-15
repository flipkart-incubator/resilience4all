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

import com.codahale.metrics.Snapshot;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.metrics.Timer;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import rx.exceptions.OnErrorNotImplementedException;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class HystrixCommandLikeMetrics implements HystrixMetrics {
  @JsonIgnore private final CircuitBreaker circuitBreaker;
  @JsonIgnore private final Bulkhead bulkhead;
  @JsonIgnore private final ThreadPoolBulkhead threadPoolBulkhead;
  @JsonIgnore private final Retry retry;
  @JsonIgnore private final TimeLimiter timeLimiter;
  @JsonIgnore private final Timer clientTimer;
  @JsonIgnore private final Timer serverTimer;

  public HystrixCommandLikeMetrics(
      String commandName,
      CircuitBreaker circuitBreaker,
      Bulkhead bulkhead,
      ThreadPoolBulkhead threadPoolBulkhead,
      Retry retry,
      TimeLimiter timeLimiter,
      Timer clientTimer,
      Timer serverTimer) {
    this.clientTimer = clientTimer;
    this.serverTimer = serverTimer;
    if (Objects.isNull(circuitBreaker)
        && Objects.isNull(bulkhead)
        && Objects.isNull(threadPoolBulkhead)
        && Objects.isNull(retry)) {
      throw new OnErrorNotImplementedException(
          "Please provide at least one of circuitbreaker, bulkhead, threadpoolbulkhead and retry for: "
              + commandName,
          new IllegalArgumentException(commandName));
    }

    this.circuitBreaker = circuitBreaker;
    this.bulkhead = bulkhead;
    this.threadPoolBulkhead = threadPoolBulkhead;
    this.retry = retry;
    this.timeLimiter = timeLimiter;
  }

  public String getType() {
    return "HystrixCommand";
  }

  public String getName() {
    if (Objects.nonNull(circuitBreaker)) {
      return circuitBreaker.getName();
    }
    if (Objects.nonNull(bulkhead)) {
      return bulkhead.getName();
    }
    if (Objects.nonNull(threadPoolBulkhead)) {
      return threadPoolBulkhead.getName();
    }
    if (Objects.nonNull(retry)) {
      return retry.getName();
    }
    throw new IllegalStateException("All of the 4 resilience4j objects can not be null.");
  }

  public String getGroup() {
    return getName();
  }

  public Long getCurrentTime() {
    return System.currentTimeMillis();
  }

  @JsonProperty("isCircuitBreakerOpen")
  public boolean isCircuitBreakerOpen() {
    if (Objects.nonNull(circuitBreaker)) {
      return circuitBreaker.getState() == State.OPEN;
    }
    return false;
  }

  public Float getErrorPercentage() {
    if (Objects.nonNull(circuitBreaker)) {
      final float failureRate = circuitBreaker.getMetrics().getFailureRate();
      if (failureRate > 0.0) { // It returns -1 if `minimumNumberOfCalls` is not met.
        return failureRate;
      }
    }

    // Cant return failure rate from retry metrics as retry does not have rolling counts
    return 0.0f;
  }

  public long getErrorCount() {
    if (Objects.nonNull(circuitBreaker)) {
      return circuitBreaker.getMetrics().getNumberOfFailedCalls();
    }
    return 0;
  }

  public long getRequestCount() {
    if (Objects.nonNull(circuitBreaker)) {
      return circuitBreaker.getMetrics().getNumberOfBufferedCalls();
    }
    return 0;
  }

  public long getRollingCountBadRequests() {
    // Can't implement
    return 0;
  }

  public long getRollingCountCollapsedRequests() {
    // Can't implement
    return 0;
  }

  public long getRollingCountEmit() {
    // Can't implement
    return 0;
  }

  public long getRollingCountExceptionsThrown() {
    // Same as error count
    return getErrorCount();
  }

  public long getRollingCountFailure() {
    // Same as error count
    return getErrorCount();
  }

  public long getRollingCountFallbackEmit() {
    // Can't implement
    return 0;
  }

  public long getRollingCountFallbackFailure() {
    // Can't implement
    return 0;
  }

  public long getRollingCountFallbackMissing() {
    // same as errorCount
    return getErrorCount();
  }

  public long getRollingCountFallbackRejection() {
    // Fallback is always missing
    return 0;
  }

  public long getRollingCountFallbackSuccess() {
    // Fallback is always missing
    return 0;
  }

  public long getRollingCountResponsesFromCache() {
    // TODO::Implement this when we start using resilience4j-cache
    return 0;
  }

  public long getRollingCountSemaphoreRejected() {
    // Can't implement, can submit PR for this, maybe not as it needs rolling
    return 0;
  }

  public long getRollingCountShortCircuited() {
    if (Objects.nonNull(circuitBreaker)) {
      return circuitBreaker.getMetrics().getNumberOfNotPermittedCalls();
    }
    return 0;
  }

  public long getRollingCountSuccess() {
    if (Objects.nonNull(circuitBreaker)) {
      return circuitBreaker.getMetrics().getNumberOfSuccessfulCalls();
    }
    return 0;
  }

  public long getRollingCountThreadPoolRejected() {
    if (Objects.nonNull(circuitBreaker)) {
      return circuitBreaker.getMetrics().getNumberOfFailedCalls();
    }
    return 0;
  }

  public long getRollingCountTimeout() {
    // Can't implement
    return 0;
  }

  public long getCurrentConcurrentExecutionCount() {
    if (Objects.nonNull(bulkhead)) {
      final int available = bulkhead.getMetrics().getAvailableConcurrentCalls();
      final int max = bulkhead.getMetrics().getMaxAllowedConcurrentCalls();

      return max - available;
    } else if (Objects.nonNull(threadPoolBulkhead)) {
      final int max = threadPoolBulkhead.getMetrics().getMaximumThreadPoolSize();
      final int available = threadPoolBulkhead.getMetrics().getThreadPoolSize();

      return max - available;
    }
    return 0;
  }

  public long getRollingMaxConcurrentExecutionCount() {
    // Can't implement
    return 0;
  }

  public int getLatencyExecute_mean() {
    if (Objects.nonNull(serverTimer)) {
      final double mean = serverTimer.getMetrics().getSnapshot().getMean();
      return (int) TimeUnit.MILLISECONDS.convert((long) mean, TimeUnit.NANOSECONDS);
    }
    return 0;
  }

  public LatencyHistogram getLatencyExecute() {
    if (Objects.nonNull(serverTimer)) {
      final Snapshot snapshot = serverTimer.getMetrics().getSnapshot();
      return new LatencyHistogram(
          (int) TimeUnit.MILLISECONDS.convert(snapshot.getMin(), TimeUnit.NANOSECONDS),
          (int) TimeUnit.MILLISECONDS.convert((long) snapshot.getMedian(), TimeUnit.NANOSECONDS),
          (int) TimeUnit.MILLISECONDS.convert((long) snapshot.getMedian(), TimeUnit.NANOSECONDS),
          (int)
              TimeUnit.MILLISECONDS.convert(
                  (long) snapshot.get75thPercentile(), TimeUnit.NANOSECONDS),
          (int)
              TimeUnit.MILLISECONDS.convert(
                  (long) snapshot.get95thPercentile(), TimeUnit.NANOSECONDS),
          (int)
              TimeUnit.MILLISECONDS.convert(
                  (long) snapshot.get95thPercentile(), TimeUnit.NANOSECONDS),
          (int)
              TimeUnit.MILLISECONDS.convert(
                  (long) snapshot.get99thPercentile(), TimeUnit.NANOSECONDS),
          (int)
              TimeUnit.MILLISECONDS.convert(
                  (long) snapshot.get999thPercentile(), TimeUnit.NANOSECONDS),
          (int) TimeUnit.MILLISECONDS.convert(snapshot.getMax(), TimeUnit.NANOSECONDS));
    }
    return new LatencyHistogram(0, 0, 0, 0, 0, 0, 0, 0, 0);
  }

  // TODO
  @JsonProperty("latencyTotal_mean")
  public int getLatencyTotal_mean() {
    if (Objects.nonNull(clientTimer)) {
      final double mean = clientTimer.getMetrics().getSnapshot().getMean();
      return (int) TimeUnit.MILLISECONDS.convert((long) mean, TimeUnit.NANOSECONDS);
    }
    return 0;
  }

  // TODO
  public LatencyHistogram getLatencyTotal() {
    if (Objects.nonNull(clientTimer)) {
      final Snapshot snapshot = clientTimer.getMetrics().getSnapshot();
      return new LatencyHistogram(
          (int) TimeUnit.MILLISECONDS.convert(snapshot.getMin(), TimeUnit.NANOSECONDS),
          (int) TimeUnit.MILLISECONDS.convert((long) snapshot.getMedian(), TimeUnit.NANOSECONDS),
          (int) TimeUnit.MILLISECONDS.convert((long) snapshot.getMedian(), TimeUnit.NANOSECONDS),
          (int)
              TimeUnit.MILLISECONDS.convert(
                  (long) snapshot.get75thPercentile(), TimeUnit.NANOSECONDS),
          (int)
              TimeUnit.MILLISECONDS.convert(
                  (long) snapshot.get95thPercentile(), TimeUnit.NANOSECONDS),
          (int)
              TimeUnit.MILLISECONDS.convert(
                  (long) snapshot.get95thPercentile(), TimeUnit.NANOSECONDS),
          (int)
              TimeUnit.MILLISECONDS.convert(
                  (long) snapshot.get99thPercentile(), TimeUnit.NANOSECONDS),
          (int)
              TimeUnit.MILLISECONDS.convert(
                  (long) snapshot.get999thPercentile(), TimeUnit.NANOSECONDS),
          (int) TimeUnit.MILLISECONDS.convert(snapshot.getMax(), TimeUnit.NANOSECONDS));
    }
    return new LatencyHistogram(0, 0, 0, 0, 0, 0, 0, 0, 0);
  }

  public int getPropertyValue_circuitBreakerRequestVolumeThreshold() {
    if (Objects.nonNull(circuitBreaker)) {
      return circuitBreaker.getCircuitBreakerConfig().getMinimumNumberOfCalls();
    }
    return 0;
  }

  public long getPropertyValue_circuitBreakerSleepWindowInMilliseconds() {
    if (Objects.nonNull(circuitBreaker)) {
      return circuitBreaker.getCircuitBreakerConfig().getWaitIntervalFunctionInOpenState().apply(1);
    }
    return 0;
  }

  public String getPropertyValue_circuitBreakerSlidingWindowType() {
    if (Objects.nonNull(circuitBreaker)) {
      return circuitBreaker.getCircuitBreakerConfig().getSlidingWindowType().name();
    }
    return "UNKNOWN";
  }

  public int getPropertyValue_circuitBreakerErrorThresholdPercentage() {
    if (Objects.nonNull(circuitBreaker)) {
      return (int) circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold();
    }
    return 0;
  }

  public boolean getPropertyValue_circuitBreakerForceOpen() {
    if (Objects.nonNull(circuitBreaker)) {
      return circuitBreaker.getState() == State.FORCED_OPEN;
    }
    return false;
  }

  public boolean getPropertyValue_circuitBreakerForceClosed() {
    if (Objects.nonNull(circuitBreaker)) {
      return circuitBreaker.getState() == State.DISABLED;
    }
    return false;
  }

  public HystrixCommandProperties.ExecutionIsolationStrategy
      getPropertyValue_executionIsolationStrategy() {
    if (Objects.nonNull(bulkhead)) {
      return ExecutionIsolationStrategy.SEMAPHORE;
    }
    if (Objects.nonNull(threadPoolBulkhead)) {
      return ExecutionIsolationStrategy.THREAD;
    }
    return ExecutionIsolationStrategy.SEMAPHORE;
  }

  public int getPropertyValue_executionIsolationThreadTimeoutInMilliseconds() {
    if (Objects.nonNull(timeLimiter)) {
      return (int) (timeLimiter.getTimeLimiterConfig().getTimeoutDuration().getSeconds() * 1000);
    }
    return 0;
  }

  public int getPropertyValue_executionTimeoutInMilliseconds() {
    if (Objects.nonNull(timeLimiter)) {
      return (int) (timeLimiter.getTimeLimiterConfig().getTimeoutDuration().getSeconds() * 1000);
    }
    return 0;
  }

  public boolean getPropertyValue_executionIsolationThreadInterruptOnTimeout() {
    // Can't implement; timeout is managed per request
    return false;
  }

  public String getPropertyValue_executionIsolationThreadPoolKeyOverride() {
    // Can't implement; default is null
    return null;
  }

  public int getPropertyValue_executionIsolationSemaphoreMaxConcurrentRequests() {
    if (Objects.nonNull(bulkhead)) {
      return bulkhead.getMetrics().getMaxAllowedConcurrentCalls();
    }
    return 0;
  }

  public int getPropertyValue_fallbackIsolationSemaphoreMaxConcurrentRequests() {
    // Fallbacks are not managed
    return 0;
  }

  public int getPropertyValue_metricsRollingStatisticalWindowInMilliseconds() {
    if (Objects.nonNull(circuitBreaker)) {
      return circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize() * 1000;
    }
    return 0;
  }

  public boolean getPropertyValue_requestCacheEnabled() {
    // TODO:: When cache integration is done
    return false;
  }

  public boolean getPropertyValue_requestLogEnabled() {
    // No logging integration
    return false;
  }

  public String getThreadPool() {
    if (Objects.nonNull(threadPoolBulkhead)) {
      return threadPoolBulkhead.getName();
    }
    return null;
  }
}
