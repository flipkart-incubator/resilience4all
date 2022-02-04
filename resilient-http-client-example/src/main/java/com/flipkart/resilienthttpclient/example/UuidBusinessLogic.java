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

import lombok.SneakyThrows;

import java.util.UUID;

public class UuidBusinessLogic {

  @SneakyThrows
  public static String getUuid() {
    Thread.sleep(10);
    if (Math.random() < 0.2) {
      throw new RuntimeException();
    }
    return UUID.randomUUID().toString();
  }
}
