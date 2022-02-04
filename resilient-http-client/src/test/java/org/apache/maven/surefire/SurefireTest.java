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

package org.apache.maven.surefire;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import static org.junit.Assert.assertNotEquals;

/*
This test file is written to understand the behavior of Surefire; therefore it is @Ignore'd.
 */

@Slf4j
@Ignore
public class SurefireTest {

  private final TestName testName = new TestName();
  private final TestRule loggingRule =
      new ExternalResource() {
        protected void before() throws Throwable {}
      };
  @Rule public RuleChain ruleChain = RuleChain.outerRule(testName).around(loggingRule);

  @SneakyThrows
  @Test
  public void test1() {
    System.out.println(
        "Running test "
            + testName.getMethodName()
            + " In thread:"
            + Thread.currentThread().getName());
    Thread.sleep(100000);
    assertNotEquals("Hello", "World");
  }

  @Test
  public void test2() throws InterruptedException {
    System.out.println(
        "Running test "
            + testName.getMethodName()
            + " In thread:"
            + Thread.currentThread().getName());
    Thread.sleep(100000);
    assertNotEquals("Hello", "World");
  }

  @Test
  public void test3() throws InterruptedException {
    System.out.println(
        "Running test "
            + testName.getMethodName()
            + " In thread:"
            + Thread.currentThread().getName());
    Thread.sleep(100000);
    assertNotEquals("Hello", "World");
  }

  @Test
  public void test4() throws InterruptedException {
    System.out.println(
        "Running test "
            + testName.getMethodName()
            + " In thread:"
            + Thread.currentThread().getName());
    Thread.sleep(10000);
    assertNotEquals("Hello", "World");
  }

  @Test
  public void test5() throws InterruptedException {
    System.out.println(
        "Running test "
            + testName.getMethodName()
            + " In thread:"
            + Thread.currentThread().getName());
    Thread.sleep(10000);
    assertNotEquals("Hello", "World");
  }

  @Test
  public void test6() throws InterruptedException {
    System.out.println(
        "Running test "
            + testName.getMethodName()
            + " In thread:"
            + Thread.currentThread().getName());
    Thread.sleep(10000);
    assertNotEquals("Hello", "World");
  }
}
