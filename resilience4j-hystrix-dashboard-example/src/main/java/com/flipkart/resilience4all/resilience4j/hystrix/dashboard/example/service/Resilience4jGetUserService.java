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

package com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.service;

import com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.model.UserDetails;
import com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.util.Utils;
import com.flipkart.resilience4all.resilience4j.timer.TimerConfig;
import com.flipkart.resilience4all.resilience4j.timer.TimerRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.metrics.Timer;
import lombok.SneakyThrows;

import java.util.function.Supplier;

public class Resilience4jGetUserService implements GetUserService {
  public static final String COMMAND_NAME = "GetUserResilience4jCommand";
  private final GetUserService getUserService;
  private final CircuitBreaker circuitBreaker;
  private final ThreadPoolBulkhead threadPoolBulkhead;
  private final Timer serverTimer;
  private final Timer clientTimer;

  public Resilience4jGetUserService(
      GetUserService getUserService,
      CircuitBreakerRegistry circuitBreakerRegistry,
      ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry,
      TimerRegistry timerRegistry) {
    this.getUserService = getUserService;
    threadPoolBulkhead = threadPoolBulkheadRegistry.bulkhead(COMMAND_NAME);
    circuitBreaker = circuitBreakerRegistry.circuitBreaker(COMMAND_NAME);
    serverTimer = timerRegistry.timer(COMMAND_NAME + ".server", TimerConfig.ofDefaults());
    clientTimer = timerRegistry.timer(COMMAND_NAME + ".client", TimerConfig.ofDefaults());
  }

  @Override
  @SneakyThrows
  public UserDetails getUser(int id) {
    Supplier<UserDetails> baseSupplier = () -> getUserService.getUser(id);
    return Utils.decorateSupplierWithResilience(
            baseSupplier, circuitBreaker, threadPoolBulkhead, serverTimer, clientTimer)
        .get()
        .toCompletableFuture()
        .get();
  }
}
