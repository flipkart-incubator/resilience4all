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

import com.flipkart.resilienthttpclient.exceptions.ServerSideException;
import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.HttpResponseStatus;

public class AsyncHandlerDecoratorFor5xx<T> extends BaseAsyncHandlerDecorator<T> {

  private int responseStatusCode;

  public AsyncHandlerDecoratorFor5xx(AsyncHandler<T> innerAsyncHandler) {
    super(innerAsyncHandler);
  }

  @Override
  public State onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
    final State state = super.onStatusReceived(responseStatus);
    this.responseStatusCode = responseStatus.getStatusCode();
    return state;
  }

  @Override
  public T onCompleted() throws Exception {
    T t = super.onCompleted();
    if (responseStatusCode >= 500 && responseStatusCode < 600) {
      throw new ServerSideException(t);
    }
    return t;
  }
}
