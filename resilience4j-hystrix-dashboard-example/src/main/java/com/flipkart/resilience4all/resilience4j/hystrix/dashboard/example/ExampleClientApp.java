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

package com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example;

import com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.util.Utils;
import org.asynchttpclient.AsyncHttpClient;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.asynchttpclient.Dsl.asyncHttpClient;

public class ExampleClientApp {
  private final AsyncHttpClient httpClient = asyncHttpClient();
  volatile boolean isStopped = false;
  ExecutorService listUsersExecutor = Executors.newFixedThreadPool(5);
  ExecutorService getUserExecutor = Executors.newFixedThreadPool(5);

  public static void main(String[] args) {
    ExampleClientApp exampleClientApp = new ExampleClientApp();
    Runtime.getRuntime().addShutdownHook(new Thread(exampleClientApp::shutdown));
    exampleClientApp.run();
  }

  private void run() {
    for (int i = 0; i < 5; i++) {
      submitUrl(listUsersExecutor, "http://localhost:9000/users");
      submitUrl(getUserExecutor, "http://localhost:9000/users/1");
    }
  }

  private void submitUrl(ExecutorService executorService, String url) {
    executorService.submit(
        () -> {
          while (!isStopped) {
            Utils.addRandomArtificalDelay(1000);
            try {
              httpClient.prepareGet(url).setRequestTimeout(200).setReadTimeout(200).execute().get();
            } catch (Exception ignore) {
              // Ignore exception.
            }
          }
        });
  }

  private void shutdown() {
    isStopped = true;
    listUsersExecutor.shutdown();
    getUserExecutor.shutdown();
    try {
      listUsersExecutor.awaitTermination(10, TimeUnit.SECONDS);
    } catch (InterruptedException ignore) {
      // Ignore;
    }
    try {
      getUserExecutor.awaitTermination(10, TimeUnit.SECONDS);
    } catch (InterruptedException ignore) {
      // ignore;
    }
    try {
      httpClient.close();
    } catch (IOException ignore) {
      // Ignore;
    }
  }
}
