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
package org.jclouds.azurecompute.arm.compute.functions;

import static com.google.common.base.Preconditions.checkArgument;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jclouds.azurecompute.arm.AzureComputeApi;
import org.jclouds.azurecompute.arm.compute.options.AzureTemplateOptions;
import org.jclouds.azurecompute.arm.domain.AvailabilitySet;
import org.jclouds.azurecompute.arm.domain.ResourceGroup;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.javax.annotation.Nullable;
import org.jclouds.logging.Logger;

import com.google.common.base.Function;
import com.google.common.cache.LoadingCache;

@Singleton
public class TemplateToAvailabilitySet implements Function<Template, AvailabilitySet> {

   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;

   private final AzureComputeApi api;
   private final LoadingCache<String, ResourceGroup> resourceGroupMap;

   @Inject
   TemplateToAvailabilitySet(AzureComputeApi api, LoadingCache<String, ResourceGroup> resourceGroupMap) {
      this.api = api;
      this.resourceGroupMap = resourceGroupMap;
   }

   @Nullable
   @Override
   public AvailabilitySet apply(final Template input) {
      checkArgument(input.getOptions() instanceof AzureTemplateOptions, "An AzureTemplateOptions object is required");
      AzureTemplateOptions options = input.getOptions().as(AzureTemplateOptions.class);

      AvailabilitySet as = null;
      String location = input.getLocation().getId();
      String resourceGroup = resourceGroupMap.getUnchecked(location).name();

      if (options.getAvailabilitySetName() != null) {
         as = api.getAvailabilitySetApi(resourceGroup).get(options.getAvailabilitySetName());

         checkArgument(as != null, "No availability set with name '%s' was found", options.getAvailabilitySetName());
         checkArgument(location.equals(as.location()), "The availability set %s does not belong to location %s",
               options.getAvailabilitySetName(), location);

      } else if (options.getAvailabilitySet() != null) {
         as = api.getAvailabilitySetApi(resourceGroup).get(options.getAvailabilitySet().name());

         if (as != null) {
            checkArgument(location.equals(as.location()), "The availability set %s does not belong to location %s",
                  options.getAvailabilitySetName(), location);
         } else {
            as = api.getAvailabilitySetApi(resourceGroup).createOrUpdate(options.getAvailabilitySet().name(), location,
                  options.getAvailabilitySet().tags(), options.getAvailabilitySet().properties());
            logger.debug(">> creating availability set [%s]", as.name());
         }
      }

      return as;
   }

}
