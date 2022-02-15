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

import com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.service.GetUserService;
import com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.service.Resilience4jGetUserService;
import com.flipkart.resilience4all.resilience4j.timer.TimerRegistry;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import javax.inject.Named;

public class Resilience4jGetUserGuiceModule implements Module {
  @Provides
  @Singleton
  public GetUserService providesResilience4jGetUserService(
      @Named("default") GetUserService getUsersService,
      CircuitBreakerRegistry circuitBreakerRegistry,
      ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry,
      TimerRegistry timerRegistry) {
    return new Resilience4jGetUserService(
        getUsersService, circuitBreakerRegistry, threadPoolBulkheadRegistry, timerRegistry);
  }

  @Override
  public void configure(Binder binder) {}
}
