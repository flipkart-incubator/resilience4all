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

package com.flipkart.resilienthttpclient.example;

import com.flipkart.resilienthttpclient.ResilientDomain;
import com.flipkart.resilienthttpclient.example.core.AnotherExternalServiceClient;
import com.flipkart.resilienthttpclient.example.core.ExternalServiceClient;
import com.flipkart.resilienthttpclient.example.model.User;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.RetryConfig;
import lombok.SneakyThrows;
import org.asynchttpclient.AsyncHttpClient;

import java.util.concurrent.Executors;

import static org.asynchttpclient.Dsl.asyncHttpClient;

public class ConsoleApplication {
  @SneakyThrows
  public static void main(String[] args) {
    /*
    Normal usage of AsyncHttpClient
     */
    final AsyncHttpClient normalAsyncHttpClient = asyncHttpClient();
    final ExternalServiceClient normalExternalServiceClient =
        new ExternalServiceClient(normalAsyncHttpClient);

    final User user1 = normalExternalServiceClient.getUser(1);
    System.out.println(user1.getFirstName());
    assert new Integer(1).equals(user1.getId());

    /*
    Using ResilientAsyncHttpClient
     */

    // STEP 1: Create default AsyncHttpClient and configure it the way you like.
    final AsyncHttpClient defaultAsyncHttpClient = asyncHttpClient(); // Using default

    // STEP 2: Create ResilientDomain
    final AsyncHttpClient resilientDomain =
        ResilientDomain.builder("externalService")
            .asyncHttpClient(defaultAsyncHttpClient)
            .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
            .retryConfig(RetryConfig.ofDefaults(), Executors.newScheduledThreadPool(1))
            .bulkheadConfig(BulkheadConfig.ofDefaults())
            .maxContentLengthInBytes(1024 * 2) // Setting it to 2KB
            .build();

    // STEP 3: pass  to the ExternalServiceClient
    // As ExternalServiceClient is coded to the interface; we can provide the implementation of
    // ResilientAsyncHttpClient
    final ExternalServiceClient externalServiceClient = new ExternalServiceClient(resilientDomain);

    // STEP 4: Call External Service
    final User user2 = externalServiceClient.getUser(2);
    System.out.println(user2.getFirstName());
    assert new Integer(2).equals(user1.getId());

    // STEP 5: You can create as many resilientDomains as you like on the same asyncHttpClient
    final AsyncHttpClient anotherAsyncHttpClient =
        ResilientDomain.builder("test")
            .asyncHttpClient(defaultAsyncHttpClient)
            .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
            .retryConfig(RetryConfig.ofDefaults(), Executors.newScheduledThreadPool(1))
            .bulkheadConfig(BulkheadConfig.ofDefaults())
            .maxContentLengthInBytes(1024 * 2) // Setting it to 2KB
            .build();

    AnotherExternalServiceClient anotherExternalServiceClient =
        new AnotherExternalServiceClient(anotherAsyncHttpClient);
    final User user3 = anotherExternalServiceClient.getUser(3);
    System.out.println(user3.getFirstName());
    assert new Integer(3).equals(user1.getId());

    normalAsyncHttpClient.close();
    resilientDomain.close();
    anotherAsyncHttpClient.close();
  }
}
