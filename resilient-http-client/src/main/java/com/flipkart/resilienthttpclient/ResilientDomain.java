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
import com.flipkart.resilience4all.resilience4j.timer.TimerRegistry;
import com.flipkart.resilienthttpclient.exceptions.ClientSideException;
import com.flipkart.resilienthttpclient.handlers.AsyncHandlerDecoratorFor5xx;
import com.flipkart.resilienthttpclient.handlers.AsyncHandlerDecoratorForMaxContentLength;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.metrics.BulkheadMetrics;
import io.github.resilience4j.metrics.CircuitBreakerMetrics;
import io.github.resilience4j.metrics.RetryMetrics;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.asynchttpclient.*;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public abstract class ResilientDomain extends BaseResilientAsyncHttpClientDecorator {

  private final ResilientDomainDecoratorFactory decoratorFactory;

  private ResilientDomain(
      AsyncHttpClient innerAsyncHttpClient, ResilientDomainDecoratorFactory decoratorFactory) {
    super(innerAsyncHttpClient);
    this.decoratorFactory = decoratorFactory;
  }

  public static Builder builder(String domainName) {
    return new Builder(domainName);
  }

  @Override
  public ListenableFuture<Response> executeRequest(RequestBuilder requestBuilder) {
    return executeRequest(requestBuilder.build());
  }

  @Override
  public <T> ListenableFuture<T> executeRequest(
      RequestBuilder requestBuilder, AsyncHandler<T> handler) {
    return executeRequest(requestBuilder.build(), handler);
  }

  @Override
  public ListenableFuture<Response> executeRequest(Request request) {
    return executeRequest(request, AsyncCompletionHandlerBase::new);
  }

  @Override
  public <T> ListenableFuture<T> executeRequest(Request request, AsyncHandler<T> handler) {
    return executeRequest(request, () -> handler);
  }

  // This is the primary method that does the actual execution
  @Override
  public <T> ListenableFuture<T> executeRequest(
      Request request, Supplier<AsyncHandler<T>> handlerSupplier) {
    /*
    Using a hanlderSupplier is critical for supporting retries. In the default case we use
    `AsyncCompletionHandlerBase::new` as the handler supplier. In case a retry happens, a new instance
     of AsyncCompletionHandlerBase is created, which ensures there is no residue of previous
     attempt's AsyncCompletionHandlerBase instance.
     A user is advised to use the handlerSupplier api exposed by RichAsyncHttpClient for passing
     custom handlers.
     */

    return new ResilientListenableFuture<>(
        getInnerAsyncHttpClient(),
        request,
        this.getDecorator(),
        () -> decorateAsyncHandler(handlerSupplier.get()));
  }

  @Override
  public <T> ListenableFuture<T> executeRequest(
      RequestBuilder requestBuilder, Supplier<AsyncHandler<T>> handlerSupplier) {
    return executeRequest(requestBuilder.build(), handlerSupplier);
  }

  protected <T> UnaryOperator<Supplier<CompletionStage<T>>> getDecorator() {
    return decoratorFactory.create();
  }

  protected abstract <T> AsyncHandler<T> decorateAsyncHandler(AsyncHandler<T> handler);

  public static class Builder {
    private final String domainName;

    private AsyncHttpClient asyncHttpClient;
    private CircuitBreakerConfig circuitBreakerConfig;
    private CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
    private String circuitBreakerName;
    private RetryConfig retryConfig;
    private ScheduledExecutorService retryExecutor;
    private String retryName;
    private RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
    private BulkheadConfig bulkheadConfig;
    private String bulkheadName;
    private BulkheadRegistry bulkheadRegistry = BulkheadRegistry.ofDefaults();
    private MetricRegistry metricRegistry;
    private TimerRegistry timerRegistry;

    // Configs
    private int maxContentLengthInBytes = -1;
    private boolean record5xxAsFailure = true;

    public Builder(String domainName) {
      this.domainName = circuitBreakerName = retryName = bulkheadName = domainName;
    }

    public Builder asyncHttpClient(AsyncHttpClient httpClient) {
      asyncHttpClient = httpClient;
      return this;
    }

    public Builder circuitBreakerConfig(CircuitBreakerConfig config) {
      circuitBreakerConfig = config;
      return this;
    }

    public Builder circuitBreakerName(String name) {
      circuitBreakerName = name;
      return this;
    }

    public Builder circuitBreakerRegistry(CircuitBreakerRegistry circuitBreakerRegistry) {
      this.circuitBreakerRegistry = circuitBreakerRegistry;
      return this;
    }

    public Builder retryConfig(RetryConfig config, ScheduledExecutorService executor) {
      retryConfig = config;
      retryExecutor = executor;
      return this;
    }

    public Builder retryName(String name) {
      retryName = name;
      return this;
    }

    public Builder retryRegistry(RetryRegistry retryRegistry) {
      this.retryRegistry = retryRegistry;
      return this;
    }

    public Builder bulkheadConfig(BulkheadConfig config) {
      bulkheadConfig = config;
      return this;
    }

    public Builder bulkheadName(String name) {
      bulkheadName = name;
      return this;
    }

    public Builder bulkheadRegistry(BulkheadRegistry bulkheadRegistry) {
      this.bulkheadRegistry = bulkheadRegistry;
      return this;
    }

    public Builder metricRegistry(MetricRegistry metricRegistry) {
      this.metricRegistry = metricRegistry;
      return this;
    }

    public Builder timerRegistry(TimerRegistry timerRegistry) {
      this.timerRegistry = timerRegistry;
      return this;
    }

    public Builder maxContentLengthInBytes(int maxContentLengthInBytes) {
      this.maxContentLengthInBytes = maxContentLengthInBytes;
      return this;
    }

    public Builder record5xxAsFailure(boolean record5xxAsFailure) {
      this.record5xxAsFailure = record5xxAsFailure;
      return this;
    }

    public AsyncHttpClient build() {
      if (Objects.isNull(asyncHttpClient)) {
        throw new IllegalArgumentException("AsyncHttpClient is a mandatory parameter to build.");
      }

      if (Objects.isNull(metricRegistry)) {
        throw new IllegalArgumentException("MetricRegistry is a mandatory parameter to build.");
      }

      if (Objects.isNull(timerRegistry)) {
        timerRegistry = TimerRegistry.ofMetricRegistry(metricRegistry);
      }

      CircuitBreaker circuitBreaker = null;
      if (Objects.nonNull(circuitBreakerConfig)) {
        final CircuitBreakerConfig.Builder builder =
            CircuitBreakerConfig.from(circuitBreakerConfig);

        // We are following the below approach - make an ignoreExceptionsPredicate
        // The problem with this approach is - ClientSideExceptions will not be counted as
        // success or failure. You will have no metrics for them at all.
        final Predicate<Throwable> ignoreExceptionsPredicate =
            circuitBreakerConfig
                .getIgnoreExceptionPredicate()
                .or(t -> t instanceof ClientSideException);
        builder.ignoreException(ignoreExceptionsPredicate);

        // The other approach would be to change recordExceptionPredicate, like code snippet below
        /*
        final Predicate<Throwable> recordExceptionPredicate = circuitBreakerConfig
            .getRecordExceptionPredicate().and(t ->
                (!(t instanceof ClientSideException))
            );
        builder.recordException(recordExceptionPredicate);
         */
        // The problem with above approach is ClientSideException would be counted as success
        // and you will see a metrics of success for same. Which I believe completely wrong.
        // Therefore we will use ignoreExceptionPredicate and build some mechanism to provide
        // metrics on clientSideExceptions

        circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName, builder.build());
        metricRegistry.registerAll(CircuitBreakerMetrics.ofCircuitBreaker(circuitBreaker));
      }

      Bulkhead bulkhead = null;
      if (Objects.nonNull(bulkheadConfig)) {
        bulkhead = bulkheadRegistry.bulkhead(bulkheadName, bulkheadConfig);
        metricRegistry.registerAll(BulkheadMetrics.ofBulkhead(bulkhead));
      }

      if (Objects.isNull(retryConfig)) {
        // By default creating maxAttempt 1 retry config, so as to get needed metrics.
        // With this, ClientSideException will also be counted correctly in failedWithoutRetry
        retryConfig = RetryConfig.custom().maxAttempts(1).build();

        // IMPORTANT:
        // One thing to note here is that when we are setting maxAttempts=1, retryExecutor is in
        // fact
        // null. That does not cause any issue because the calls are never retried.
        // So even if executorService is null, there is never a nullPointerException because
        // maxAttempts = 1 and therefore nothing ever can be/needs to be retried.
      }
      final RetryConfig.Builder<Object> builder = RetryConfig.from(retryConfig);
      final Predicate<Throwable> retryPredicate =
          retryConfig.getExceptionPredicate().and(t -> (!(t instanceof ClientSideException)));
      builder.retryOnException(retryPredicate);
      Retry retry = retryRegistry.retry(retryName, builder.build());
      metricRegistry.registerAll(RetryMetrics.ofRetry(retry));

      final ResilientDomainDecoratorFactory decoratorFactory =
          ResilientDomainDecoratorFactory.builder()
              .circuitBreaker(circuitBreaker)
              .bulkhead(bulkhead)
              .retry(retry)
              .retryExecutor(retryExecutor)
              .domainName(domainName)
              .timerRegistry(timerRegistry)
              .build();

      // Creating new variables so that if a client calls builder.withXXX() after builder.build()
      // It should not have any adverse effect.
      final boolean record5xxAsFailure = this.record5xxAsFailure;
      final int maxContentLengthInBytes = this.maxContentLengthInBytes;
      return new ResilientDomain(asyncHttpClient, decoratorFactory) {
        @Override
        protected <T> AsyncHandler<T> decorateAsyncHandler(AsyncHandler<T> handler) {
          if (record5xxAsFailure) {
            handler = new AsyncHandlerDecoratorFor5xx<>(handler);
          }
          if (maxContentLengthInBytes > -1) {
            handler =
                new AsyncHandlerDecoratorForMaxContentLength<>(handler, maxContentLengthInBytes);
          }
          return handler;
        }
      };
    }
  }
}
