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

package com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.module;

import com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.service.HystrixListUsersService;
import com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.service.ListUsersService;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import javax.inject.Named;

public class HystrixListUsersGuiceModule implements Module {
  @Override
  public void configure(Binder binder) {}

  @Provides
  @Singleton
  public ListUsersService providesResilience4jGetUserService(
      @Named("default") ListUsersService listUsersService) {
    return new HystrixListUsersService(listUsersService);
  }
}
