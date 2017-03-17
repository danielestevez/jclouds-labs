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
package org.jclouds.azurecompute.arm.features;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.any;
import static org.jclouds.azurecompute.arm.config.AzureComputeProperties.TIMEOUT_RESOURCE_DELETED;
import static org.jclouds.compute.predicates.NodePredicates.inGroup;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jclouds.azurecompute.arm.AzureComputeApi;
import org.jclouds.azurecompute.arm.domain.IdReference;
import org.jclouds.azurecompute.arm.domain.Provisionable;
import org.jclouds.azurecompute.arm.domain.ResourceGroup;
import org.jclouds.azurecompute.arm.domain.VirtualMachineImage;
import org.jclouds.azurecompute.arm.domain.VirtualMachineImageProperties;
import org.jclouds.azurecompute.arm.internal.AzureLiveTestUtils;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.internal.BaseComputeServiceContextLiveTest;
import org.jclouds.domain.Location;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.cache.LoadingCache;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

// We extend the BaseComputeServiceContextLiveTest to create nodes using the abstraction, which is much easier
@Test(groups = "live", singleThreaded = true)
public class VirtualMachineImageApiLiveTest extends BaseComputeServiceContextLiveTest {

   //   private static final String imageName = String
   //         .format("image-%s-%s", VirtualMachineImageApiLiveTest.class.getSimpleName().toLowerCase(),
   //               System.getProperty("user.name"));
   //
   private static final String imageName = "imageFromRest";

   private LoadingCache<String, ResourceGroup> resourceGroupMap;
   private Predicate<URI> resourceDeleted;
   private Predicate<Supplier<Provisionable>> resourceAvailable;
   private AzureComputeApi api;

   private String resourceGroupName;
   private String location;
   private VirtualMachineImageApi imageApi;
   private VirtualMachineImage image;

   private String group;

   public VirtualMachineImageApiLiveTest() {
      provider = "azurecompute-arm";
      group = getClass().getSimpleName().toLowerCase();
   }

   @Override
   protected Properties setupProperties() {
      Properties properties = super.setupProperties();
      AzureLiveTestUtils.defaultProperties(properties);
      checkNotNull(setIfTestSystemPropertyPresent(properties, "oauth.endpoint"), "test.oauth.endpoint");
      return properties;
   }

   @Override
   protected void initializeContext() {
      super.initializeContext();
      resourceDeleted = context.utils().injector().getInstance(Key.get(new TypeLiteral<Predicate<URI>>() {
      }, Names.named(TIMEOUT_RESOURCE_DELETED)));
      resourceGroupMap = context.utils().injector()
            .getInstance(Key.get(new TypeLiteral<LoadingCache<String, ResourceGroup>>() {
            }));
      resourceAvailable = context.utils().injector()
            .getInstance(Key.get(new TypeLiteral<Predicate<Supplier<Provisionable>>>() {
            }));
      api = view.unwrapApi(AzureComputeApi.class);
   }

   @Override
   @BeforeClass
   public void setupContext() {
      super.setupContext();
      // Use the resource name conventions used in the abstraction so the nodes
      // can see the load balancer
      ResourceGroup resourceGroup = createResourceGroup();
      resourceGroupName = resourceGroup.name();
      location = resourceGroup.location();
      imageApi = api.getVirtualMachineImageApi(resourceGroupName);
   }

   @Override
   @AfterClass(alwaysRun = true)
   protected void tearDownContext() {
      try {
         view.getComputeService().destroyNodesMatching(inGroup(group));
      } finally {
         try {
            URI uri = api.getResourceGroupApi().delete(resourceGroupName);
            assertResourceDeleted(uri);
         } finally {
            super.tearDownContext();
         }
      }
   }

   @Test
   public void testDeleteImageDoesNotExist() {
      URI uri = imageApi.delete("notAnImage");
      assertNull(uri);
   }

   @Test(dependsOnMethods = "testDeleteImageDoesNotExist")
   public void testCreateImage() throws RunNodesException {

      Set<? extends NodeMetadata> nodes = view.getComputeService().createNodesInGroup(group, 1);

      NodeMetadata node = nodes.iterator().next();
      IdReference vmIdRef = IdReference.create(node.getProviderId());
      view.getComputeService().suspendNode(node.getId());

      VirtualMachineApi vmApi = view.unwrapApi(AzureComputeApi.class).getVirtualMachineApi(resourceGroupName);
      vmApi.generalize(node.getName());

      image = imageApi
            .create(imageName, location, VirtualMachineImageProperties.builder().sourceVirtualMachine(vmIdRef).build());
      assertNotNull(image);
   }

   @Test(dependsOnMethods = "testCreateImage")
   public void testListImages() {
      List<VirtualMachineImage> result = imageApi.list();

      // Verify we have something
      assertNotNull(result);
      assertTrue(result.size() > 0);

      // Check that the load balancer matches the one we originally passed in
      assertTrue(any(result, new Predicate<VirtualMachineImage>() {
         @Override
         public boolean apply(VirtualMachineImage input) {
            return image.name().equals(input.name());
         }
      }));
   }

   @Test(dependsOnMethods = "testCreateImage")
   public void testGetImage() {
      image = imageApi.get(imageName);
      assertNotNull(image);
   }

   @Test(dependsOnMethods = { "testCreateImage", "testListImages", "testGetImage" }, enabled = false, alwaysRun = true)
   public void deleteImage() {
      URI uri = imageApi.delete(imageName);
      assertResourceDeleted(uri);
   }


   private void assertResourceDeleted(final URI uri) {
      if (uri != null) {
         assertTrue(resourceDeleted.apply(uri),
               String.format("Resource %s was not terminated in the configured timeout", uri));
      }
   }


   private ResourceGroup createResourceGroup() {
      Location location = view.getComputeService().templateBuilder().build().getLocation();
      return resourceGroupMap.getUnchecked(location.getId());
   }

}
