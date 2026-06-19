package com.flipkart.resilience4all.metrics.eventstream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.metrics.Timer;
import rx.exceptions.OnErrorNotImplementedException;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class HystrixThreadPoolLikeMetrics implements HystrixMetrics {

  @JsonIgnore private final ThreadPoolBulkhead threadPoolBulkhead;
  /**
   * All circuit breakers sharing this thread pool (commonThreadpool pattern).
   * We aggregate their buffered-call counts to compute total executions through the pool.
   */
  @JsonIgnore private final List<CircuitBreaker> circuitBreakers;
  @JsonIgnore private final Timer serverTimer;

  public HystrixThreadPoolLikeMetrics(
          String commandName,
          ThreadPoolBulkhead threadPoolBulkhead,
          List<CircuitBreaker> circuitBreakers,
          Timer serverTimer) {
    if (Objects.isNull(threadPoolBulkhead)) {
      throw new OnErrorNotImplementedException(
          "Please provide threadpoolbulkhead for: " + commandName,
          new IllegalArgumentException(commandName));
    }
    this.threadPoolBulkhead = threadPoolBulkhead;
    this.circuitBreakers = circuitBreakers != null ? circuitBreakers : Collections.emptyList();
    this.serverTimer = serverTimer;
  }

  public String getType() {
    return "HystrixThreadPool";
  }

  public String getName() {
    if (Objects.nonNull(threadPoolBulkhead)) {
      return threadPoolBulkhead.getName();
    }
    throw new IllegalStateException("resilience4j object can not be null.");
  }

  public Number getCurrentActiveCount() {
    return 0;
  }

  public Number getCurrentCompletedTaskCount() {
    return 0;
  }

  public Number getCurrentCorePoolSize() {
    return threadPoolBulkhead.getMetrics().getCoreThreadPoolSize();
  }

  public Number getCurrentLargestPoolSize() {
    return 0;
  }

  public Number getCurrentMaximumPoolSize() {
    return threadPoolBulkhead.getMetrics().getMaximumThreadPoolSize();
  }

  public Number getCurrentPoolSize() {
    return threadPoolBulkhead.getMetrics().getThreadPoolSize();
  }

  public Number getCurrentTaskCount() {
    return 0;
  }

  public Number getCurrentQueueSize() {
    int max = threadPoolBulkhead.getMetrics().getQueueCapacity();
    int available = threadPoolBulkhead.getMetrics().getRemainingQueueCapacity();
    return max - available;
  }

  /**
   * Aggregate buffered calls across all circuit breakers sharing this pool.
   * This is the total number of executions that went through CommonPool.
   */
  public Number getRollingCountThreadsExecuted() {
    if (!circuitBreakers.isEmpty()) {
      return circuitBreakers.stream()
          .mapToLong(cb -> cb.getMetrics().getNumberOfBufferedCalls())
          .sum();
    }
    if (Objects.nonNull(serverTimer)) {
      return serverTimer.getMetrics().getSnapshot().size();
    }
    return 0;
  }

  public Number getRollingMaxActiveThreads() {
    return threadPoolBulkhead.getMetrics().getThreadPoolSize();
  }

  public Number getRollingCountCommandRejections() {
    return 0;
  }

  public Number getPropertyValue_queueSizeRejectionThreshold() {
    return threadPoolBulkhead.getMetrics().getQueueCapacity();
  }

  public Number getPropertyValue_metricsRollingStatisticalWindowInMilliseconds() {
    // Must be non-zero — Hystrix Dashboard divides rollingCountThreadsExecuted by this
    // (converted to seconds) to show req/s. Standard Hystrix default is 10 seconds.
    return 10000;
  }

  public Number getReportingHosts() {
    return 1;
  }

  public long getCurrentTime() {
    return System.currentTimeMillis();
  }
}
