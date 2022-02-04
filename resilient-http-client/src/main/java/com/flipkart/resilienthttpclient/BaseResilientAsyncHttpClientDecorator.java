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

import org.asynchttpclient.*;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BaseResilientAsyncHttpClientDecorator implements ResilientAsyncHttpClient {

  private final AsyncHttpClient innerAsyncHttpClient;

  public BaseResilientAsyncHttpClientDecorator(AsyncHttpClient innerAsyncHttpClient) {
    this.innerAsyncHttpClient = innerAsyncHttpClient;
  }

  protected AsyncHttpClient getInnerAsyncHttpClient() {
    return innerAsyncHttpClient;
  }

  @Override
  public boolean isClosed() {
    return innerAsyncHttpClient.isClosed();
  }

  @Override
  public AsyncHttpClient setSignatureCalculator(SignatureCalculator signatureCalculator) {
    return innerAsyncHttpClient.setSignatureCalculator(signatureCalculator);
  }

  @Override
  public BoundRequestBuilder prepare(String s, String s1) {
    return new ResilientBoundRequestBuilder(this, innerAsyncHttpClient.prepare(s, s1));
  }

  @Override
  public BoundRequestBuilder prepareGet(String url) {
    return new ResilientBoundRequestBuilder(this, innerAsyncHttpClient.prepareGet(url));
  }

  @Override
  public BoundRequestBuilder prepareConnect(String url) {
    return new ResilientBoundRequestBuilder(this, innerAsyncHttpClient.prepareConnect(url));
  }

  @Override
  public BoundRequestBuilder prepareOptions(String url) {
    return new ResilientBoundRequestBuilder(this, innerAsyncHttpClient.prepareOptions(url));
  }

  @Override
  public BoundRequestBuilder prepareHead(String url) {
    return new ResilientBoundRequestBuilder(this, innerAsyncHttpClient.prepareHead(url));
  }

  @Override
  public BoundRequestBuilder preparePost(String url) {
    return new ResilientBoundRequestBuilder(this, innerAsyncHttpClient.preparePost(url));
  }

  @Override
  public BoundRequestBuilder preparePut(String url) {
    return new ResilientBoundRequestBuilder(this, innerAsyncHttpClient.preparePut(url));
  }

  @Override
  public BoundRequestBuilder prepareDelete(String url) {
    return new ResilientBoundRequestBuilder(this, innerAsyncHttpClient.prepareDelete(url));
  }

  @Override
  public BoundRequestBuilder preparePatch(String url) {
    return new ResilientBoundRequestBuilder(this, innerAsyncHttpClient.preparePatch(url));
  }

  @Override
  public BoundRequestBuilder prepareTrace(String url) {
    return new ResilientBoundRequestBuilder(this, innerAsyncHttpClient.prepareTrace(url));
  }

  @Override
  public BoundRequestBuilder prepareRequest(Request request) {
    return new ResilientBoundRequestBuilder(this, innerAsyncHttpClient.prepareRequest(request));
  }

  @Override
  public BoundRequestBuilder prepareRequest(RequestBuilder requestBuilder) {
    return new ResilientBoundRequestBuilder(
        this, innerAsyncHttpClient.prepareRequest(requestBuilder));
  }

  @Override
  public <T> ListenableFuture<T> executeRequest(Request request, AsyncHandler<T> handler) {
    return innerAsyncHttpClient.executeRequest(request, handler);
  }

  @Override
  public <T> ListenableFuture<T> executeRequest(
      RequestBuilder requestBuilder, AsyncHandler<T> handler) {
    return innerAsyncHttpClient.executeRequest(requestBuilder, handler);
  }

  @Override
  public ListenableFuture<Response> executeRequest(Request request) {
    return innerAsyncHttpClient.executeRequest(request);
  }

  @Override
  public ListenableFuture<Response> executeRequest(RequestBuilder requestBuilder) {
    return innerAsyncHttpClient.executeRequest(requestBuilder);
  }

  @Override
  public ClientStats getClientStats() {
    return innerAsyncHttpClient.getClientStats();
  }

  @Override
  public void flushChannelPoolPartitions(Predicate<Object> predicate) {
    innerAsyncHttpClient.flushChannelPoolPartitions(predicate);
  }

  @Override
  public AsyncHttpClientConfig getConfig() {
    return innerAsyncHttpClient.getConfig();
  }

  @Override
  public void close() throws IOException {
    innerAsyncHttpClient.close();
  }

  @Override
  public <T> ListenableFuture<T> executeRequest(
      Request request, Supplier<AsyncHandler<T>> handlerSupplier) {
    return executeRequest(request, handlerSupplier.get());
  }

  @Override
  public <T> ListenableFuture<T> executeRequest(
      RequestBuilder requestBuilder, Supplier<AsyncHandler<T>> handlerSupplier) {
    return executeRequest(requestBuilder, handlerSupplier.get());
  }
}
