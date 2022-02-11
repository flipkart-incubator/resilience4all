package com.flipkart.resilience4all.metrics.eventstream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import rx.exceptions.OnErrorNotImplementedException;

import java.util.Objects;

public class HystrixThreadPoolLikeMetrics implements HystrixMetrics {

    @JsonIgnore
    private final ThreadPoolBulkhead threadPoolBulkhead;

    public HystrixThreadPoolLikeMetrics(
            String commandName,
            ThreadPoolBulkhead threadPoolBulkhead) {
        if (Objects.isNull(threadPoolBulkhead)) {
            throw new OnErrorNotImplementedException("Please provide threadpoolbulkhead for: " + commandName,
                    new IllegalArgumentException(commandName));
        }

        this.threadPoolBulkhead = threadPoolBulkhead;
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
        return threadPoolBulkhead.getMetrics().getThreadPoolSize();
    }

    public Number getCurrentCompletedTaskCount() {
        //Can't implement;
        return 0;
    }

    public Number getCurrentCorePoolSize() {
        return threadPoolBulkhead.getMetrics().getCoreThreadPoolSize();
    }

    public Number getCurrentLargestPoolSize() {
        //Can't implement;
        return 0;
    }

    public Number getCurrentMaximumPoolSize() {
        return threadPoolBulkhead.getMetrics().getMaximumThreadPoolSize();
    }

    public Number getCurrentPoolSize() {
        return threadPoolBulkhead.getMetrics().getMaximumThreadPoolSize();
    }

    public Number getCurrentTaskCount() {
        //Can't implement;
        return 0;
    }

    public Number getCurrentQueueSize() {
        int max = threadPoolBulkhead.getMetrics().getQueueCapacity();
        int available = threadPoolBulkhead.getMetrics().getRemainingQueueCapacity();
        return max - available;
    }

    public Number getRollingCountThreadsExecuted() {
        //can't implement
        return 0;
    }

    public Number getRollingMaxActiveThreads() {
        return threadPoolBulkhead.getMetrics().getThreadPoolSize();
    }


    public Number getRollingCountCommandRejections() {
        //can't implement
        return 0;
    }

    public Number getPropertyValue_queueSizeRejectionThreshold() {
        return threadPoolBulkhead.getMetrics().getQueueCapacity();
    }

    public Number getPropertyValue_metricsRollingStatisticalWindowInMilliseconds() {
        //can't implement
        return 0;
    }
}