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

import com.codahale.metrics.MetricRegistry;
import com.flipkart.resilience4all.metrics.eventstream.Resilience4jMetricsStreamServlet;
import com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.module.ExampleGuiceModule;
import com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.module.HystrixListUsersGuiceModule;
import com.flipkart.resilience4all.resilience4j.hystrix.dashboard.example.module.Resilience4jGetUserGuiceModule;
import com.netflix.hystrix.contrib.codahalemetricspublisher.HystrixCodaHaleMetricsPublisher;
import com.netflix.hystrix.strategy.HystrixPlugins;
import io.dropwizard.Application;
import io.dropwizard.jetty.MutableServletContextHandler;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlet.ServletHolder;
import ru.vyarus.dropwizard.guice.GuiceBundle;
import ru.vyarus.dropwizard.guice.injector.lookup.InjectorLookup;

public class HystrixAndResilience4jExampleApplication extends Application<ExampleConfiguration> {

  public HystrixAndResilience4jExampleApplication() {}

  public static void main(String[] args) throws Exception {
    new HystrixAndResilience4jExampleApplication().run(args);
  }

  @Override
  public void initialize(Bootstrap<ExampleConfiguration> bootstrap) {
    bootstrap.addBundle(
        GuiceBundle.builder()
            .modules(
                new ExampleGuiceModule(),
                new HystrixListUsersGuiceModule(),
                new Resilience4jGetUserGuiceModule())
            .extensions(ExampleResource.class)
            .build());
  }

  @Override
  public void run(ExampleConfiguration exampleConfiguration, Environment environment)
      throws Exception {
    final MutableServletContextHandler applicationContext = environment.getApplicationContext();
    final Resilience4jMetricsStreamServlet servlet1 =
        InjectorLookup.getInjector(this).get().getInstance(Resilience4jMetricsStreamServlet.class);

    final ServletHolder servlet = new ServletHolder(servlet1);
    applicationContext.addServlet(servlet, "/hystrix.stream");

    final MetricRegistry metrics = environment.metrics();

    HystrixPlugins.reset();
    final HystrixPlugins instance = HystrixPlugins.getInstance();
    instance.registerMetricsPublisher(new HystrixCodaHaleMetricsPublisher(metrics));
  }
}
