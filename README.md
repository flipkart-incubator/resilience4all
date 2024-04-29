# What is it

`resilience4all` aims to enhance existing client-server integration libraries and frameworks with necessary resilience
constructs. For ex: resilient-http-client provides built in support for resiliency at the client side for integration
over http.

# Modules

## resilience4j-metrics-event-stream

This module enables Hystrix Dashboard for resilience4j metrics.

Step 1: Add the following dependency




```xml
<dependency>
    <groupId>com.flipkart.resilience4all</groupId>
    <artifactId>resilience4j-metrics-event-stream</artifactId>
    <version>0.0.1</version>
</dependency>
```

And add the clojars in the repositories section

```xml
<repository>
    <id>clojars</id>
    <name>Clojars repository</name>
    <url>https://clojars.org/repo</url>
</repository>
```

Step 2: Initialize and register `Resilience4jMetricsStreamServlet` servlet. Example code for a Dropwizard Application

```java
    final MutableServletContextHandler applicationContext = environment.getApplicationContext();
    final Resilience4jMetricsStreamServlet servlet1 =
        InjectorLookup.getInjector(this).get().getInstance(Resilience4jMetricsStreamServlet.class);

    final ServletHolder servlet = new ServletHolder(servlet1);
    applicationContext.addServlet(servlet, "/hystrix.stream");
```

Refer `Resilience4jExampleApplication` class in `resilience4j-hystrix-dashboard-example` module for a working dropwizard
application showcasing the use of Hystrix Dashboard with resilience4j metrics.

## resilient-http-client

A drop-in replacement for [AsyncHttpClient](https://github.com/AsyncHttpClient/async-http-client)
with added support to configure
[resilience4j-circuitbreaker](https://resilience4j.readme.io/docs/circuitbreaker),
[resilience4j-retry](https://resilience4j.readme.io/docs/retry),
[resilience4j-bulkhead](https://resilience4j.readme.io/docs/bulkhead).

### Features

1. Integration with [Circuitbreaker](https://resilience4j.readme.io/docs/circuitbreaker)
2. Intergation with [Retry](https://resilience4j.readme.io/docs/retry)
3. Intergation with [Bulkhead](https://resilience4j.readme.io/docs/bulkhead)
4. Separate treatment for ClientSideExceptions from ServerSideExceptions.  
   a. Ex: A ClientSideException like MaxContentSizeExceeded need not be retried. It will always result in 4xx. Whereas,
   ServerSideException like 5xx should be retried. b. Ex: A ClientSideException like MaxContentSizeExceeded should not
   trip the circuitbreaker. It tells us nothing about Server's state. But a ServerSideException like 5xx should trip the
   circuitbreaker.
4. 5xx responses are considered failures for both circuitbreaker and retry.
5. User can configure max size of the response body to safeguard against jvm going OOM.
6. Bulkhead exceptions are retried.
7. Circuitbreaker exceptions are retried.
8. Timeout and IOExceptions are retried.
9. 4xx calls are *NOT* retried as it is a client side exception.
10. MaxContentLengthExceededExceptions are not retried as it is a client side exception.
11. Metrics   
    a. Metrics available for observed behavior of server.   
    b. Metrics available for what client experienced.  
    c. Ex: If a server call fails after 100 ms and retry passes after next 50 ms - server metrics will show 1 successful
    and 1 failed call whereas client metrics will show 1 successful call with 150 (100ms for first call that failed +
    50ms for second call that succeeded ) ms latency.  
    d. All metrics provided by resilience4j-metrics.
12. Freely configure asyncHttpClient as you wish.
13. Freely configure resilience4j as you wish.
14. All of the above with zero business logic code change - configured and controlled at the dependency injection layer.

All the above features are either provided by `AsyncHttpClient` or by `resilience4j`. This library aims to make the
usage easier while eliminating common mistakes. For ex: More often than not we have seen that event ClientSideExceptions
are also retried.

### Usage examples

The code for the examples below can be found in module `resilient-http-client-example` in the
[source code](https://github.com/flipkart-incubator/resilience4all)

A. Code to AsyncHttpClient interface

```java
public class ExternalServiceClient {
  private AsyncHttpClient asyncHttpClient;

...

  public User getUser(int userId) {
    return asyncHttpClient
        .prepareGet("https://example-service.com/api/users/" + userId)
        .execute()
        .toCompletableFuture()
        
        //Successful response handling
        .thenApply((Response response) -> {
          return objectMapper
              .readValue(response.getResponseBody(), UserResponse.class)
              .getData();
        })
        
        //Handle Exception, Provide Fallback
        .exceptionally((Throwable throwable) -> new User())
        .get();
  }
}
```

Please not the exception handling and providing fallback in the `.exceptionally()` method.

B. Build `ResilientDomain` and configure for ExternalService as per business domain needs.

```java
final ResilientDomain resilientDomain = ResilientDomain.builder("externalService")
    .asyncHttpClient(defaultAsyncHttpClient)
    .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
    .retryConfig(RetryConfig.ofDefaults(), Executors.newScheduledThreadPool(1))
    .bulkheadConfig(BulkheadConfig.ofDefaults())
    .maxContentLengthInBytes(1024 * 2) //No response can be more than 2KB
    .build();
```

A `ResilientDomain` is a user-configured bundle of applied resilience constructs for a particular business domain. It
encapsulates all the configurations of circuitBreaker, retry, bulkhead, asyncHttpClient, maxContentLength etc.

A client would typically create different `ResilientDomain`s for all the services it integrates with. For ex: If
OrderService has to integrate with UserService and InventoryService, it will create one ResilientDomain instance for
each.

A client can freely configure circuitBreaker, retry, bulkhead etc as per needs of the business domain. For ex:
OrderService might configure `retryConfig.maxAttempts` = 3 for UserService whereas for
InventoryService `retryConfig.maxAttempts` = 2.

Note: Same asyncHttpClient can be used for more than one `ResilientDomain` instance.

C. Inject `ResilientDomain` in `ExternalServiceClient`

```java
final ExternalServiceClient externalServiceClient = new ExternalServiceClient(
resilientDomain);
```

As `ResilientDomain` implements `AsyncHttpClient`, it acts as a drop-in replacement for
`AsyncHttpClient` in existing code bases.

For migration, one has to simply update the dependency injection configuration so as to start injecting appropriate
instances of `ResilientDomain`s in relevant classes.

D. Interact with ExternalService like before.

```java
final User user2 = externalServiceClient.getUser(2);
```

Complete working code of the example can be found
[here](https://github.com/flipkart-incubator/resilience4all/tree/master/resilient-http-client-example/src/main/java/com/flipkart/resilienthttpclient/example)
.

### Available Metrics

#### Circuit Breaker

- resilience4j.circuitbreaker.<resilient-domain-name>.buffered
- resilience4j.circuitbreaker.<resilient-domain-name>.failed
- resilience4j.circuitbreaker.<resilient-domain-name>.failure_rate
- resilience4j.circuitbreaker.<resilient-domain-name>.not_permitted
- resilience4j.circuitbreaker.<resilient-domain-name>.slow
- resilience4j.circuitbreaker.<resilient-domain-name>.slow_call_rate
- resilience4j.circuitbreaker.<resilient-domain-name>.slow_failed
- resilience4j.circuitbreaker.<resilient-domain-name>.slow_successful = 0
- resilience4j.circuitbreaker.<resilient-domain-name>.state = 1
- resilience4j.circuitbreaker.<resilient-domain-name>.successful = 0

#### Bulkhead

- resilience4j.bulkhead.<resilient-domain-name>.available_concurrent_calls
- resilience4j.bulkhead.<resilient-domain-name>.max_allowed_concurrent_calls

#### Retry

- resilience4j.retry.<resilient-domain-name>.failed_calls_without_retry
- resilience4j.retry.<resilient-domain-name>.failed_calls_with_retry
- resilience4j.retry.<resilient-domain-name>.successful_calls_without_retry
- resilience4j.retry.<resilient-domain-name>.successful_calls_with_retry

#### resilience4all

There are client side and server side metrics available. Both server and metrics are Timers.

- com.flipkart.resilience4all.resilienthttpclient.<resilient-domain-name>.server
- com.flipkart.resilience4all.resilienthttpclient.<resilient-domain-name>.client

#### Future Work for resilient-http-client

1. Caching: Integration with resilience4j-cache
2. Dynamic updations of config.
3. Client side load balancing.

## resilience4j-metrics-event-stream

`resilience4j-metrics-event-stream` provides the capability of building Hystrix Dashboard over resilience4j based
projects. If you have been using Hystrix and love the Hystrix Dashboard, you can now use both resilience4j and Hystrix
Dashboard; best of both worlds. Needless to say, it is compatible with Netflix Turbine as well.

#### How to enable hystrix dashboard

A. Add dependency of resilience4j-metrics-event-stream

```xml
<dependency>
    <groupId>com.flipkart.resilience4all</groupId>
    <artifactId>resilience4j-metrics-event-stream</artifactId>
    <version>${resilience4all.version}</version>
</dependency>
```

B. Add dependency of hystrix-metrics-event-stream

```xml
<dependency>
    <groupId>com.netflix.hystrix</groupId>
    <artifactId>hystrix-metrics-event-stream</artifactId>
    <version>${hystrix.version}</version>
</dependency>
```   

C. This is a critical step to configuring `resilience4j-metrics-event-stream`. `resilience4j-metrics-event-stream`
queries `CircuitBreakerRegistry`, `BulkheadRegistry`, `ThreadPoolBulkheadRegistry`, `RetryRegistry`,
`TimeRegistry` to access the currently existing circuitBreakers, bulkheads, threadpoolBulkheads, retries and timers.
Let's first create a resilientDomain that uses these objects. Check the code in
`resilient-http-client-example` class `com.flipkart.resilienthttpclient.example.ExampleGuiceModule`.

```java
  @Provides
  @Singleton
  public ExternalService providesExternalService(MetricRegistry metricRegistry,
      CircuitBreakerRegistry circuitBreakerRegistry,
      RetryRegistry retryRegistry,
      BulkheadRegistry bulkheadRegistry,
      TimerRegistry timerRegistry) {
    final AsyncHttpClient normalAsyncHttpClient = asyncHttpClient();

    final AsyncHttpClient resilientDomain = ResilientDomain.builder("externalService")
        .asyncHttpClient(normalAsyncHttpClient)
        .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
        .circuitBreakerRegistry(circuitBreakerRegistry)
        .retryConfig(RetryConfig.ofDefaults(), Executors.newScheduledThreadPool(1))
        .retryRegistry(retryRegistry)
        .bulkheadConfig(BulkheadConfig.ofDefaults())
        .bulkheadRegistry(bulkheadRegistry)
        .maxContentLengthInBytes(1024 * 2) //Setting it to 2KB
        .metricRegistry(metricRegistry)
        .timerRegistry(timerRegistry)
        .build();

    return new ExternalServiceClient(resilientDomain);
  }

```

D. Use the same singleton instances to create `Resilience4jMetricsStreamServlet` (in ExampleGuiceModule)

```java
  @Provides
  @Singleton
  public Resilience4jMetricsStreamServlet providesResilience4jMetricsStreamServlet(CircuitBreakerRegistry circuitBreakerRegistry,
      BulkheadRegistry bulkheadRegistry,
      ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry,
      RetryRegistry retryRegistry,
      TimerRegistry timerRegistry) {
    return Resilience4jMetricsStreamServlet
        .builder()
        .withCircuitBreakerRegistry(circuitBreakerRegistry)
        .withBulkheadRegistry(bulkheadRegistry)
        .withThreadPoolBulkheadRegistry(threadPoolBulkheadRegistry)
        .withRetryRegistry(retryRegistry)
        .withTimerRegistry(timerRegistry)
        .build();
  }
```   

E. Register the servlet in application context. Check `ExampleApplication`.`run()`

```java
    final MutableServletContextHandler applicationContext = environment
        .getApplicationContext();
    final Resilience4jMetricsStreamServlet servlet1 = InjectorLookup.getInjector(this).get()
        .getInstance(Resilience4jMetricsStreamServlet.class);

    final ServletHolder servlet = new ServletHolder(servlet1);
    applicationContext.addServlet(servlet, "/hystrix.stream");

```

F. Please note that in the step above, we have registered `Resilience4jMetricsStreamServlet` at the
url `/hystrix.stream`. `Resilience4jMetricsStreamServlet` internally queries Hystrix and creates a concatenated stream
of both hystrix and resilience4j. There is NO need to register hystrix servlet if resilience4j servlet is registered.

NOTE:
As resilience4j and Hystrix are two different libraries; it is rather a tricky process to map resilience4j based metrics
and properties to Hystrix command like metrics and properties. Nonetheless, this project bridges that gap and puts best
effort into mapping both. Please look into `class HystrixCommandLikeMetrics` for details of how resilience4j metrics are
being mapped to Hystrix streams.

## Example Application

`resilient-http-client-example` module is dropwizard application that demonstrates the use of
`resilient-http-client` and `resilience4j-metrics-event-stream`. Once you run the application, following urls will be
available

1. http://localhost:9000/users/<uid> where uid is a number.
2. http://localhost:9000/users/uuid which does not call any external service, yet wraps business logic in Hystrix
   commands and resilience4j
3. http://localhost:9000/hystrix.stream which returns the stream of metrics that is compatible with Hystrix Dashboard
   and Turbine.

## Future Work for resilience4all

1. resilience4j-load-balance: for client side load balancing.
2. resilient-es-http-client: An es-http-client that uses resilient-http-client and provides specific es-customizations.
   For ex:
   [search.mx_buckets limit error](https://discuss.elastic.co/t/search-max-buckets-limit-error-on-7-0-1/179989)
   should be considered a client side exception and not trip the circuit.
3. resilient-mysql-client: For client-side load balancing.

## Further documentation

We create a retry object with maxAttempts 1 so that you get the metrics S without Retry - S with Retry - these would
show up as extra in bulkhead and circuit breaker F without retry - client side exceptions F with Retry - these must be =
maxAttempts * circuitBreaker.failed, not necessarily bulkhead could have rejected it too. Total = S + SR + F + FR

Timeout is available per request or per asyncHttpClient, continue to use the same.

Handler has to be careful of state handling because we might call the same handler multiple times. You can do
ResilientAsyncHttpClient and then call executeRequest with handlerSupplier in case you can't maintain state.
