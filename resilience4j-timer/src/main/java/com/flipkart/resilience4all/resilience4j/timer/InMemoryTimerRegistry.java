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

package com.flipkart.resilience4all.resilience4j.timer;

import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.core.registry.AbstractRegistry;
import io.github.resilience4j.metrics.Timer;

import java.util.Collections;
import java.util.Map;

public class InMemoryTimerRegistry extends AbstractRegistry<Timer, TimerConfig>
    implements TimerRegistry {
  private final MetricRegistry metricRegistry;

  /*package private*/ InMemoryTimerRegistry(MetricRegistry metricRegistry) {
    super(TimerConfig.ofDefaults());
    this.metricRegistry = metricRegistry;
  }

  public Timer timer(String name, final TimerConfig timerConfig) {
    return computeIfAbsent(
        name, () -> Timer.ofMetricRegistry(timerConfig.getName(), metricRegistry));
  }

  @Override
  public Map<String, Timer> getAllTimers() {
    return Collections.unmodifiableMap(entryMap);
  }
}
