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

import com.flipkart.resilienthttpclient.exceptions.MaxContentLengthExceededException;
import io.netty.handler.codec.http.HttpHeaders;
import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseStatus;

import java.util.Objects;

public class AsyncHandlerDecoratorForMaxContentLength<T> extends BaseAsyncHandlerDecorator<T> {

  private final int MAX_CONTENT_LENGTH_IN_BYTES;
  private int contentLengthSoFar;

  public AsyncHandlerDecoratorForMaxContentLength(
      AsyncHandler<T> innerAsyncHandler, int max_content_length_in_bytes) {
    super(innerAsyncHandler);
    MAX_CONTENT_LENGTH_IN_BYTES = max_content_length_in_bytes;
  }

  @Override
  public State onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
    // Following the reset strategy of AsyncCompletionHandler.onStatusReceived
    contentLengthSoFar = 0;
    return super.onStatusReceived(responseStatus);
  }

  @Override
  public State onHeadersReceived(HttpHeaders headers) throws Exception {
    int contentLength = -1;
    final String contentLengthString = headers.get("Content-Length");
    if (Objects.nonNull(contentLengthString)) {
      try {
        contentLength = Integer.parseInt(contentLengthString);
      } catch (NumberFormatException ignore) {
      }
    }
    throwExceptionIfNecessary(contentLength);
    return super.onHeadersReceived(headers);
  }

  @Override
  public State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
    contentLengthSoFar += bodyPart.length();
    throwExceptionIfNecessary(contentLengthSoFar);
    return super.onBodyPartReceived(bodyPart);
  }

  private void throwExceptionIfNecessary(int contentLength) {
    if (contentLength > MAX_CONTENT_LENGTH_IN_BYTES) {
      throw new MaxContentLengthExceededException(contentLengthSoFar, MAX_CONTENT_LENGTH_IN_BYTES);
    }
  }
}
