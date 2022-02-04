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

package io.github.resilience4j.circuitbreaker;

import lombok.SneakyThrows;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.*;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

/*
This test file is written to understand the behavior of circuit breaker; therefore it is @Ignore'd.
 */
@Ignore
public class CircuitBreakerTest {
  @Test
  public void testSupplierOfCompletionStage__success()
      throws ExecutionException, InterruptedException, TimeoutException {
    CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
    CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

    assertEquals(metrics.getNumberOfSuccessfulCalls(), 0);

    final CompletableFuture<String> future = new CompletableFuture<>();
    final Supplier<CompletionStage<String>> supplier = () -> future;

    final Supplier<CompletionStage<String>> futureSupplier =
        circuitBreaker.decorateCompletionStage(supplier);

    final CompletionStage<String> completionStage = futureSupplier.get();
    assertEquals(0, metrics.getNumberOfSuccessfulCalls());

    new Thread(
            new Runnable() {
              @SneakyThrows
              @Override
              public void run() {
                Thread.sleep(1000 * 1);
                future.completeExceptionally(new RuntimeException("My custom exception"));
              }
            })
        .start();

    try {
      completionStage.toCompletableFuture().get(11, TimeUnit.SECONDS);
    } catch (ExecutionException ee) {
      assertEquals(ee.getCause().getMessage(), "My custom exception");
    }

    // Verify that this has no relation to the underlying future's completion.
    assertEquals(1, metrics.getNumberOfFailedCalls());
    assertEquals(0, metrics.getNumberOfSuccessfulCalls());
  }
}
