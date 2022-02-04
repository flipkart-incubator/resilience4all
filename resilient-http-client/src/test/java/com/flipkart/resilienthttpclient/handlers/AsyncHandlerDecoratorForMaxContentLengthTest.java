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
import lombok.SneakyThrows;
import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.HttpResponseBodyPart;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AsyncHandlerDecoratorForMaxContentLengthTest {

  @Mock AsyncHandler<String> asyncHandler;

  @Mock HttpHeaders httpHeaders;

  @Mock HttpResponseBodyPart httpResponseBodyPart;

  @SneakyThrows
  @Before
  public void before() {
    when(httpHeaders.get("Content-Length")).thenReturn("99");
    when(httpResponseBodyPart.length()).thenReturn(99);
    when(asyncHandler.onCompleted()).thenReturn("World");
  }

  // ###############################################
  // ############ Private Utiliy functions #########
  // ###############################################

  private AsyncHandlerDecoratorForMaxContentLength<String> createContentLengthHandler() {
    return new AsyncHandlerDecoratorForMaxContentLength<>(asyncHandler, 100);
  }

  // ###############################################
  // ############ Tests start from here ############
  // ###############################################

  @SneakyThrows
  @Test
  public void test__happyPath() {
    final AsyncHandlerDecoratorForMaxContentLength<String> contentLengthHandler =
        createContentLengthHandler();

    contentLengthHandler.onHeadersReceived(httpHeaders);
    contentLengthHandler.onBodyPartReceived(httpResponseBodyPart);
    assertEquals("World", contentLengthHandler.onCompleted()); // Assert that we reached here.
  }

  @SneakyThrows
  @Test
  public void test__happyPath__noContentLengthHeader() {
    final AsyncHandlerDecoratorForMaxContentLength<String> contentLengthHandler =
        createContentLengthHandler();

    when(httpHeaders.get("Content-Length")).thenReturn(null);

    contentLengthHandler.onHeadersReceived(httpHeaders);
    contentLengthHandler.onBodyPartReceived(httpResponseBodyPart);
    assertEquals("World", contentLengthHandler.onCompleted()); // Assert that we reached here.
  }

  @SneakyThrows
  @Test
  public void test__happyPath__equalToMax() {
    final AsyncHandlerDecoratorForMaxContentLength<String> contentLengthHandler =
        createContentLengthHandler();

    when(httpHeaders.get("Content-Length")).thenReturn("100");
    when(httpResponseBodyPart.length()).thenReturn(100);

    contentLengthHandler.onHeadersReceived(httpHeaders);
    contentLengthHandler.onBodyPartReceived(httpResponseBodyPart);
    assertEquals("World", contentLengthHandler.onCompleted()); // Assert that we reached here.
  }

  @SneakyThrows
  @Test
  public void test__happyPath__multipleBodyParts() {
    final AsyncHandlerDecoratorForMaxContentLength<String> contentLengthHandler =
        createContentLengthHandler();

    when(httpHeaders.get("Content-Length")).thenReturn("100");
    when(httpResponseBodyPart.length()).thenReturn(50);

    contentLengthHandler.onHeadersReceived(httpHeaders);
    contentLengthHandler.onBodyPartReceived(httpResponseBodyPart);
    contentLengthHandler.onBodyPartReceived(httpResponseBodyPart);
    assertEquals("World", contentLengthHandler.onCompleted()); // Assert that we reached here.
  }

  @SneakyThrows
  @Test(expected = MaxContentLengthExceededException.class)
  public void test__negativePath__header() {
    final AsyncHandlerDecoratorForMaxContentLength<String> contentLengthHandler =
        createContentLengthHandler();

    when(httpHeaders.get("Content-Length")).thenReturn("101");

    contentLengthHandler.onHeadersReceived(httpHeaders);
  }

  @SneakyThrows
  @Test(expected = MaxContentLengthExceededException.class)
  public void test__negativePath__bodyPart() {
    final AsyncHandlerDecoratorForMaxContentLength<String> contentLengthHandler =
        createContentLengthHandler();

    when(httpHeaders.get("Content-Length")).thenReturn(null);
    when(httpResponseBodyPart.length()).thenReturn(101);

    contentLengthHandler.onHeadersReceived(httpHeaders);
    contentLengthHandler.onBodyPartReceived(httpResponseBodyPart);
  }

  @SneakyThrows
  @Test(expected = MaxContentLengthExceededException.class)
  public void test__negativePath__multipleBodyParts() {
    final AsyncHandlerDecoratorForMaxContentLength<String> contentLengthHandler =
        createContentLengthHandler();

    when(httpHeaders.get("Content-Length")).thenReturn(null);
    when(httpResponseBodyPart.length()).thenReturn(51);

    contentLengthHandler.onHeadersReceived(httpHeaders);
    contentLengthHandler.onBodyPartReceived(httpResponseBodyPart);
    contentLengthHandler.onBodyPartReceived(httpResponseBodyPart);
  }
}
