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

package com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.service;

import com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.model.UserDetails;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

public class HystrixGetUserService implements GetUserService {
  private final GetUserService getUserService;

  public HystrixGetUserService(GetUserService getUserService) {
    this.getUserService = getUserService;
  }

  @Override
  public UserDetails getUser(int id) {
    return new GetUserHystrixCommand(id).execute();
  }

  class GetUserHystrixCommand extends HystrixCommand<UserDetails> {
    private final int id;

    protected GetUserHystrixCommand(int id) {
      super(HystrixCommandGroupKey.Factory.asKey("GetUserHystrixCommand"));
      this.id = id;
    }

    @Override
    protected UserDetails run() throws Exception {
      return getUserService.getUser(id);
    }
  }
}
