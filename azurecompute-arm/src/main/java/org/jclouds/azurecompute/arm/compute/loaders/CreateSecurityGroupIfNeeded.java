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
package org.jclouds.azurecompute.arm.compute.loaders;

import static org.jclouds.compute.util.ComputeServiceUtils.getPortRangesFromList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jclouds.azurecompute.arm.AzureComputeApi;
import org.jclouds.azurecompute.arm.compute.domain.RegionScopeIdAndIngressRules;
import org.jclouds.azurecompute.arm.domain.NetworkSecurityGroup;
import org.jclouds.azurecompute.arm.domain.NetworkSecurityGroupProperties;
import org.jclouds.azurecompute.arm.domain.NetworkSecurityRule;
import org.jclouds.azurecompute.arm.domain.NetworkSecurityRuleProperties;
import org.jclouds.azurecompute.arm.domain.NetworkSecurityRuleProperties.Access;
import org.jclouds.azurecompute.arm.domain.NetworkSecurityRuleProperties.Direction;
import org.jclouds.azurecompute.arm.domain.NetworkSecurityRuleProperties.Protocol;
import org.jclouds.azurecompute.arm.domain.ResourceGroup;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.logging.Logger;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Singleton
public class CreateSecurityGroupIfNeeded extends CacheLoader<RegionScopeIdAndIngressRules, String> {
   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;

   private final AzureComputeApi api;
   private final LoadingCache<String, ResourceGroup> resourceGroupMap;

   @Inject
   CreateSecurityGroupIfNeeded(AzureComputeApi api, LoadingCache<String, ResourceGroup> resourceGroupMap) {
      this.api = api;
      this.resourceGroupMap = resourceGroupMap;
   }

   @Override
   public String load(RegionScopeIdAndIngressRules key) throws Exception {
      ResourceGroup resourceGroup = resourceGroupMap.getUnchecked(key.region());
      return createSecurityGroup(key.region(), resourceGroup.name(), key.id(), key.inboundPorts());
   }

   private String createSecurityGroup(String location, String resourceGroup, String name, int[] inboundPorts) {
      logger.debug(">> creating security group %s in %s...", name, location);

      Map<Integer, Integer> portRanges = getPortRangesFromList(inboundPorts);

      List<NetworkSecurityRule> rules = new ArrayList<NetworkSecurityRule>();

      int startPriority = 100;
      for (Map.Entry<Integer, Integer> portRange : portRanges.entrySet()) {
         String range = portRange.getKey() + "-" + portRange.getValue();
         String ruleName = "tcp-" + range;

         NetworkSecurityRuleProperties properties = NetworkSecurityRuleProperties.builder().protocol(Protocol.Tcp) //
               .sourceAddressPrefix("*") //
               .sourcePortRange("*") //
               .destinationAddressPrefix("*") //
               .destinationPortRange(range) //
               .direction(Direction.Inbound) //
               .access(Access.Allow) //
               .priority(startPriority++) //
               .build();

         rules.add(NetworkSecurityRule.create(ruleName, null, null, properties));
      }

      NetworkSecurityGroup securityGroup = api.getNetworkSecurityGroupApi(resourceGroup).createOrUpdate(name, location,
            null, NetworkSecurityGroupProperties.builder().securityRules(rules).build());

      return securityGroup.id();
   }

}
