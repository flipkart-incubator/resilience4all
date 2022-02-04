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

import com.flipkart.resilience4all.resilience4j.timer.TimerConfig;
import com.flipkart.resilience4all.resilience4j.timer.TimerRegistry;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.metrics.Timer;
import io.github.resilience4j.retry.Retry;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

class ResilientDomainDecoratorFactory {

  private final CircuitBreaker circuitBreaker;
  private final Bulkhead bulkhead;
  private final Retry retry;
  private final ScheduledExecutorService retryExecutor;
  private final Timer serverTimer;
  private final Timer clientTimer;

  private ResilientDomainDecoratorFactory(
      CircuitBreaker circuitBreaker,
      Bulkhead bulkhead,
      Retry retry,
      ScheduledExecutorService retryExecutor,
      String domainName,
      TimerRegistry timerRegistry) {
    this.circuitBreaker = circuitBreaker;
    this.bulkhead = bulkhead;
    this.retry = retry;
    this.retryExecutor = retryExecutor;

    serverTimer =
        timerRegistry.timer(
            domainName + ".server",
            TimerConfig.custom()
                .name("com.flipkart.resilience4all.resilienthttpclient." + domainName + ".server")
                .build());
    clientTimer =
        timerRegistry.timer(
            domainName + ".client",
            TimerConfig.custom()
                .name("com.flipkart.resilience4all.resilienthttpclient." + domainName + ".client")
                .build());
  }

  // ##########
  // Package private functions
  // ##########

  // ##########
  // Builder
  // ##########
  static Builder builder() {
    return new Builder();
  }

  // ##########
  // Private Utility functions
  // ##########

  /*package*/ <T> UnaryOperator<Supplier<CompletionStage<T>>> create() {
    final UnaryOperator<Supplier<CompletionStage<T>>> identityDecorator = supplier -> supplier;
    final UnaryOperator<Supplier<CompletionStage<T>>> serverTimerDecorator =
        createTimerDecorator(serverTimer, identityDecorator);
    final UnaryOperator<Supplier<CompletionStage<T>>> circuitBreakerDecorator =
        createCircuitBreakerDecorator(serverTimerDecorator);
    final UnaryOperator<Supplier<CompletionStage<T>>> bulkheadDecorator =
        createBulkheadDecorator(circuitBreakerDecorator);
    final UnaryOperator<Supplier<CompletionStage<T>>> retryDecorator =
        createRetryDecorator(bulkheadDecorator);
    final UnaryOperator<Supplier<CompletionStage<T>>> clientTimerDecorator =
        createTimerDecorator(clientTimer, retryDecorator);

    return clientTimerDecorator;
  }

  private <T> UnaryOperator<Supplier<CompletionStage<T>>> createCircuitBreakerDecorator(
      final UnaryOperator<Supplier<CompletionStage<T>>> decorator) {
    if (Objects.nonNull(circuitBreaker)) {
      return completionStageSupplier ->
          circuitBreaker.decorateCompletionStage(decorator.apply(completionStageSupplier));
    }
    return decorator;
  }

  private <T> UnaryOperator<Supplier<CompletionStage<T>>> createRetryDecorator(
      final UnaryOperator<Supplier<CompletionStage<T>>> decorator) {
    if (Objects.nonNull(retry)) {
      return completionStageSupplier ->
          Retry.decorateCompletionStage(
              retry, retryExecutor, decorator.apply(completionStageSupplier));
    }
    return decorator;
  }

  private <T> UnaryOperator<Supplier<CompletionStage<T>>> createBulkheadDecorator(
      final UnaryOperator<Supplier<CompletionStage<T>>> decorator) {
    if (Objects.nonNull(bulkhead)) {
      return completionStageSupplier ->
          Bulkhead.decorateCompletionStage(bulkhead, decorator.apply(completionStageSupplier));
    }
    return decorator;
  }

  private <T> UnaryOperator<Supplier<CompletionStage<T>>> createTimerDecorator(
      Timer timer, UnaryOperator<Supplier<CompletionStage<T>>> decorator) {
    return completionStageSupplier -> {
      return Timer.decorateCompletionStageSupplier(timer, decorator.apply(completionStageSupplier));
    };
  }

  static class Builder {
    private CircuitBreaker circuitBreaker;
    private Bulkhead bulkhead;
    private Retry retry;
    private ScheduledExecutorService retryExecutor;
    private String domainName;
    private TimerRegistry timerRegistry;

    Builder circuitBreaker(CircuitBreaker circuitBreaker) {
      this.circuitBreaker = circuitBreaker;
      return this;
    }

    Builder bulkhead(Bulkhead bulkhead) {
      this.bulkhead = bulkhead;
      return this;
    }

    Builder retry(Retry retry) {
      this.retry = retry;
      return this;
    }

    Builder retryExecutor(ScheduledExecutorService retryExecutor) {
      this.retryExecutor = retryExecutor;
      return this;
    }

    Builder domainName(String domainName) {
      this.domainName = domainName;
      return this;
    }

    Builder timerRegistry(TimerRegistry timerRegistry) {
      this.timerRegistry = timerRegistry;
      return this;
    }

    ResilientDomainDecoratorFactory build() {
      return new ResilientDomainDecoratorFactory(
          circuitBreaker, bulkhead, retry, retryExecutor, domainName, timerRegistry);
    }
  }
}
