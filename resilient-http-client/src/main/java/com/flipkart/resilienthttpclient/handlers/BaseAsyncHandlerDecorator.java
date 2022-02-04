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

package com.flipkart.resilienthttpclient.handlers;

import io.netty.handler.codec.http.HttpHeaders;
import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseStatus;

public class BaseAsyncHandlerDecorator<T> implements AsyncHandler<T> {

  private final AsyncHandler<T> innerAsyncHandler;

  public BaseAsyncHandlerDecorator(AsyncHandler<T> innerAsyncHandler) {
    this.innerAsyncHandler = innerAsyncHandler;
  }

  @Override
  public State onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
    return innerAsyncHandler.onStatusReceived(responseStatus);
  }

  @Override
  public State onHeadersReceived(HttpHeaders headers) throws Exception {
    return innerAsyncHandler.onHeadersReceived(headers);
  }

  @Override
  public State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
    return innerAsyncHandler.onBodyPartReceived(bodyPart);
  }

  @Override
  public void onThrowable(Throwable t) {
    innerAsyncHandler.onThrowable(t);
  }

  @Override
  public T onCompleted() throws Exception {
    return innerAsyncHandler.onCompleted();
  }
}
