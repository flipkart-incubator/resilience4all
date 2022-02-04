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

import com.flipkart.resilienthttpclient.exceptions.ServerSideException;
import com.flipkart.resilienthttpclient.handlers.AsyncHandlerSupplier;
import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Request;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class ResilientListenableFuture<T> implements ListenableFuture<T> {

  // The object of this class is returned to the user from the
  // ResilientAsyncHttpClient.executeRequest
  // Lock is needed to ensure that we are always working on the latest responseListenableFuture
  // As the supplier returned by prepareSupplier might be called more than once by a retry, we need
  // to ensure that any user interaction with an object of this class is proxied to latest
  // responseListenableFuture. Therefore everytime we access responseListenableFuture we use a
  // synchronized lock.
  private final Object lock = new Object();
  // There is only one instance of CompletableFuture; therefore we don't use lock while accessing
  // this.
  private final CompletableFuture<T> completableFuture;
  private final AsyncHttpClient asyncHttpClient;
  private final Request request;
  private ListenableFuture<T> responseListenableFuture;

  public ResilientListenableFuture(
      AsyncHttpClient asyncHttpClient,
      Request request,
      UnaryOperator<Supplier<CompletionStage<T>>> decorator,
      AsyncHandlerSupplier<T> handlerSupplier) {
    this.asyncHttpClient = asyncHttpClient;
    this.request = request;
    this.completableFuture = prepareCompletionStage(decorator, handlerSupplier);
  }

  @SuppressWarnings("unchecked")
  private CompletableFuture<T> prepareCompletionStage(
      UnaryOperator<Supplier<CompletionStage<T>>> decorator,
      AsyncHandlerSupplier<T> handlerSupplier) {
    // This returns a hot completion stage
    final Supplier<CompletionStage<T>> supplier = decorator.apply(prepareSupplier(handlerSupplier));

    // Here let's ensure that resulting future does not throw ServerSideException
    // But rather returns a Response object essentially ensuring DefaultAsyncHttpClient behavior
    // for 5xx; which is - Client gets a Response object with statusCode = 5xx.
    final Supplier<CompletionStage<T>> supplier2 =
        () -> {
          final CompletableFuture<T> promise = new CompletableFuture<>();
          supplier
              .get()
              .whenComplete(
                  (response, throwable) -> {
                    try {
                      if (Objects.isNull(throwable)) {
                        promise.complete(response); // A successful response
                        return;
                      }
                      if (throwable instanceof ServerSideException) {
                        // ServerSideExeption is made to look like a successful response
                        promise.complete((T) ((ServerSideException) throwable).getResponse());
                        return;
                      }
                      // Let's just complete this promise with the exception we received.
                      promise.completeExceptionally(throwable);
                    } catch (Throwable newThrowable) {
                      // This is only a catch all. We should ofcourse never reach here!
                      promise.completeExceptionally(newThrowable);
                    }
                  });
          return promise;
        };

    return supplier2.get().toCompletableFuture();
  }

  private Supplier<CompletionStage<T>> prepareSupplier(
      final AsyncHandlerSupplier<T> handlerSupplier) {
    return () -> {
      synchronized (lock) {
        // We are taking a handlerSupplier and not handler because this ensures that we are creating
        // new handler everytime this supplier is called.
        // And this supplier is called multiple times for the same request during retries.
        final AsyncHandler<T> handler1 = handlerSupplier.get();
        responseListenableFuture = asyncHttpClient.executeRequest(request, handler1);
        return responseListenableFuture.toCompletableFuture();
      }
    };
  }

  @Override
  public void done() {
    synchronized (lock) {
      responseListenableFuture.done();
    }
  }

  @Override
  public void abort(Throwable throwable) {
    synchronized (lock) {
      responseListenableFuture.abort(throwable);
    }
  }

  @Override
  public void touch() {
    synchronized (lock) {
      responseListenableFuture.touch();
    }
  }

  @Override
  public ListenableFuture<T> addListener(Runnable runnable, Executor exec) {
    // The implementation of this method has been copied from NettyResponseFuture.addListener
    if (exec == null) {
      exec = Runnable::run;
    }
    completableFuture.whenCompleteAsync((r, v) -> runnable.run(), exec);
    return this;
  }

  @Override
  public CompletableFuture<T> toCompletableFuture() {
    return completableFuture;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    synchronized (lock) {
      // We need to cancel 2 futures here -
      // First is the completableFuture
      final boolean result = completableFuture.cancel(mayInterruptIfRunning);
      // Second is NettyResponseFuture so that netty can cleanup.
      responseListenableFuture.cancel(mayInterruptIfRunning);
      return result;
    }
  }

  @Override
  public boolean isCancelled() {
    return completableFuture.isCancelled();
  }

  @Override
  public boolean isDone() {
    synchronized (lock) {
      return responseListenableFuture.isDone();
    }
  }

  @Override
  public T get() throws InterruptedException, ExecutionException {
    return completableFuture.get();
  }

  @Override
  public T get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return completableFuture.get(timeout, unit);
  }
}
