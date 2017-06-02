/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jclouds.azurecompute.arm.domain;

import java.util.List;

import org.jclouds.javax.annotation.Nullable;
import org.jclouds.json.SerializedNames;

import com.google.auto.value.AutoValue;

/**
 * A Metric definition for a resource
 */
@AutoValue
public abstract class MetricDefinition {

   @Nullable
   public abstract String resourceid();

   public abstract MetricDefinition.MetricName name();

   @Nullable
   public abstract Boolean isDimensionRequired();

   public abstract String unit();

   public abstract String primaryAggregationType();

   public abstract List<MetricDefinition.MetricAvailability> metricAvailabilities();

   public abstract String id();

   @SerializedNames({ "resourceid", "name", "isDimensionRequired", "unit", "primaryAggregationType",
         "metricAvailabilities", "id" })
   public static MetricDefinition create(final String resourceid, final MetricName name,
         final Boolean isDimensionRequired, final String unit, final String primaryAggregationType,
         List<MetricAvailability> metricAvailabilities, final String id) {
      return new AutoValue_MetricDefinition(resourceid, name, isDimensionRequired, unit, primaryAggregationType,
            metricAvailabilities, id);
   }

   @AutoValue
   public abstract static class MetricAvailability {

      public abstract String timeGrain();

      public abstract String retention();

      MetricAvailability() {

      }

      @SerializedNames({ "timeGrain", "retention" })
      public static MetricDefinition.MetricAvailability create(String timeGrain, String retention) {
         return new AutoValue_MetricDefinition_MetricAvailability(timeGrain, retention);
      }
   }

   @AutoValue
   public abstract static class MetricName {

      public abstract String value();

      public abstract String localizedValue();

      MetricName() {

      }

      @SerializedNames({ "value", "localizedValue" })
      public static MetricDefinition.MetricName create(String value, String localizedValue) {
         return new AutoValue_MetricDefinition_MetricName(value, localizedValue);
      }
   }
}
