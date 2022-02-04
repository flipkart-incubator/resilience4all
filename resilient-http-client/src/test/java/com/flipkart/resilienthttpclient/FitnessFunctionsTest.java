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

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

@Slf4j
public class FitnessFunctionsTest {

  private static final AtomicBoolean hasFirstRun = new AtomicBoolean(false);
  private static final AtomicBoolean hasSecondRun = new AtomicBoolean(false);
  @Rule public Timeout testTimeout = new Timeout(1, TimeUnit.MINUTES); // 60k ms = 1 minute

  @Test
  public void test__resilientDomain__allExecuteMethodsAreOverwritten() {
    // Why is this fitness function needed?
    // This fitness function is ensuring we catch manual misses/mistakes that compile time checks
    // could not find.
    final Method[] declaredMethods = ResilientDomain.class.getDeclaredMethods();
    final Method[] allMethods = ResilientDomain.class.getMethods();
    final Set<Method> prepareDeclaredMethods =
        Arrays.stream(declaredMethods)
            .filter(m -> m.getName().startsWith("execute"))
            .collect(Collectors.toSet());
    final Set<Method> prepareAllMethods =
        Arrays.stream(allMethods)
            .filter(m -> m.getName().startsWith("execute"))
            .collect(Collectors.toSet());
    assertEquals(
        "It is expected that every executeRequest method is overwritten by "
            + "ResilientDomain, because if ResilientDomain does not override any executeRequest"
            + "method, and uses default implementation of BaseResilientAsyncHttpClientDecorator"
            + "Then resilience constructs might be skipped as it would mean using innerClient.",
        prepareDeclaredMethods,
        prepareAllMethods);
  }

  @SneakyThrows
  @Test
  public void test__testRunInParallel__first() {
    System.out.println("Running first");
    hasFirstRun.set(true);
    while (!hasSecondRun.get()) {
      logInfo(
          "Testing parallel test execution. First thread has already run. Waiting for second to run....");
      Thread.sleep(1000);
    }
    logInfo("Second has run as well.");
  }

  @SneakyThrows
  @Test
  public void test__testRunInParallel__second() {
    while (!hasFirstRun.get()) {
      logInfo(
          "Testing parallel test execution. Second thread will run after first. Waiting for first to run...");
      Thread.sleep(1000);
    }
    logInfo("First has run. Running second.");
    hasSecondRun.set(true);
  }

  private void logInfo(String s) {
    System.out.println(s);
  }
}
