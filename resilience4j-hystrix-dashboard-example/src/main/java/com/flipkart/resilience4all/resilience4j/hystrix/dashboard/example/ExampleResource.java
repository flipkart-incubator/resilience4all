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

package com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example;

import com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.model.GetUserResponse;
import com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.model.ListUsersResponse;
import com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.service.GetUserService;
import com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.service.ListUsersService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class ExampleResource {

  private final ListUsersService listUsersService;
  private final GetUserService getUserService;

  @Inject
  public ExampleResource(ListUsersService listUsersService, GetUserService getUserService) {
    this.listUsersService = listUsersService;
    this.getUserService = getUserService;
  }

  @GET
  public ListUsersResponse listUsers() {
    return new ListUsersResponse(listUsersService.listUsers());
  }

  @GET
  @Path("{id}")
  public GetUserResponse getUser(@PathParam("id") int id) {
    return new GetUserResponse(getUserService.getUser(id));
  }
}
