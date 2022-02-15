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

package com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.util;

import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.metrics.Timer;

import java.util.Random;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public class Utils {
  public static void addRandomArtificalDelay(int bound) {
    try {
      Thread.sleep(new Random().nextInt(bound));
    } catch (Exception ignore) {
      // Nothing to do in this case
    }
  }

  public static <T> Supplier<CompletionStage<T>> decorateSupplierWithResilience(
      Supplier<T> baseSupplier,
      CircuitBreaker circuitBreaker,
      ThreadPoolBulkhead threadPoolBulkhead,
      Timer serverTimer,
      Timer clientTimer) {
    Supplier<T> serverTimerSupplier = Timer.decorateSupplier(serverTimer, baseSupplier);

    Supplier<T> cbSupplier = circuitBreaker.decorateSupplier(serverTimerSupplier);

    Supplier<CompletionStage<T>> tpbSupplier = threadPoolBulkhead.decorateSupplier(cbSupplier);

    Supplier<CompletionStage<T>> clientTimerSupplier =
        Timer.decorateCompletionStageSupplier(clientTimer, tpbSupplier);
    return clientTimerSupplier;
  }
}
