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

import com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.model.BaseUserDetails;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

import java.util.List;

public class HystrixListUsersService implements ListUsersService {
  private final ListUsersService listUsersService;

  public HystrixListUsersService(ListUsersService getUserService) {
    this.listUsersService = getUserService;
  }

  @Override
  public List<BaseUserDetails> listUsers() {
    return new ListUsersHystrixCommand().execute();
  }

  class ListUsersHystrixCommand extends HystrixCommand<List<BaseUserDetails>> {

    protected ListUsersHystrixCommand() {
      super(HystrixCommandGroupKey.Factory.asKey("ListUsersHystrixCommand"));
    }

    @Override
    protected List<BaseUserDetails> run() throws Exception {
      return listUsersService.listUsers();
    }
  }
}
