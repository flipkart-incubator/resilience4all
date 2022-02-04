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

package com.flipkart.resilience4all.metrics.eventstream;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LatencyHistogram {

  @JsonProperty("0")
  private final int zero;

  @JsonProperty("25")
  private final int twentyFive;

  @JsonProperty("50")
  private final int fifty;

  @JsonProperty("75")
  private final int seventyFive;

  @JsonProperty("90")
  private final int ninty;

  @JsonProperty("95")
  private final int nintyFive;

  @JsonProperty("99")
  private final int nintyNine;

  @JsonProperty("99.5")
  private final int nintyNineFive;

  @JsonProperty("100")
  private final int hundred;

  public LatencyHistogram(
      int zero,
      int twentyFive,
      int fifty,
      int seventyFive,
      int ninty,
      int nintyFive,
      int nintyNine,
      int nintyNineFive,
      int hundred) {
    this.zero = zero;
    this.twentyFive = twentyFive;
    this.fifty = fifty;
    this.seventyFive = seventyFive;
    this.ninty = ninty;
    this.nintyFive = nintyFive;
    this.nintyNine = nintyNine;
    this.nintyNineFive = nintyNineFive;
    this.hundred = hundred;
  }
}
