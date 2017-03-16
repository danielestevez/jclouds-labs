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

import static com.google.common.collect.Iterables.isEmpty;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.net.URI;
import java.util.List;

import org.jclouds.azurecompute.arm.domain.DataDisk;
import org.jclouds.azurecompute.arm.domain.IdReference;
import org.jclouds.azurecompute.arm.domain.OSDisk;
import org.jclouds.azurecompute.arm.domain.StorageProfile;
import org.jclouds.azurecompute.arm.domain.VirtualMachineImage;
import org.jclouds.azurecompute.arm.domain.VirtualMachineImageProperties;
import org.jclouds.azurecompute.arm.internal.BaseAzureComputeApiMockTest;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

@Test(groups = "unit", testName = "VirtualMachineImageApiMockTest", singleThreaded = true)
public class VirtualMachineImageApiMockTest extends BaseAzureComputeApiMockTest {
   private final String subscriptionid = "SUBSCRIPTIONID";
   private final String resourcegroup = "myresourcegroup";
   private final String apiVersion = "api-version=2016-04-30-preview";
   private final String imageName = "testVirtualMachineImage";
   private final String location = "canadaeast";

   public void createVirtualMachineImage() throws InterruptedException {
      VirtualMachineImage newImage = newVirtualMachineImage();

      server.enqueue(jsonResponse("/virtualmachineimagecreate.json").setResponseCode(200));
      final VirtualMachineImageApi machineImageApi = api.getVirtualMachineImageApi(resourcegroup);

      String path = String
            .format("/subscriptions/%s/resourcegroups/%s/providers/Microsoft.Compute/images/%s?%s", subscriptionid,
                  resourcegroup, imageName, apiVersion);

      //      [{"location":"canadaeast","properties":{"sourceVirtualMachine":{"id":"vmId"},
      // "storageProfile":{"osDisk":{"osType":"Linux","name":"Ubuntu"},"dataDisks":[]},
      // "provisioningState":"Succeeded"}}]

      String json =
            "{\"location\":\"" + location + "\"," + "\"properties\":{\"sourceVirtualMachine\":{\"id\":\"vmId\"},"
                  + "\"storageProfile\":{\"osDisk\":{\"osType\":\"Linux\",\"name\":\"Ubuntu\"},\"dataDisks\":[]},"
                  + "\"provisioningState\":\"Succeeded\"}}";

      System.out.println("JSON: " + json);

      VirtualMachineImage result = machineImageApi.create(imageName, location, newImage.properties());
      assertSent(server, "PUT", path, json);

      assertEquals(result.name(), imageName);
      assertEquals(result.location(), location);
   }

   public void getVirtualMachineImage() throws InterruptedException {
      server.enqueue(jsonResponse("/virtualmachineimageget.json").setResponseCode(200));

      final VirtualMachineImageApi imageApi = api.getVirtualMachineImageApi(resourcegroup);
      VirtualMachineImage result = imageApi.get(imageName);

      String path = String
            .format("/subscriptions/%s/resourcegroups/%s/providers/Microsoft.Compute/images/%s?%s", subscriptionid,
                  resourcegroup, imageName, apiVersion);
      assertSent(server, "GET", path);

      assertEquals(result.name(), imageName);
      assertEquals(result.location(), location);
      assertNotNull(result.properties().sourceVirtualMachine());
      assertNotNull(result.properties().storageProfile());
   }

   public void getVirtualMachineImageReturns404() throws InterruptedException {
      server.enqueue(response404());

      final VirtualMachineImageApi imageApi = api.getVirtualMachineImageApi(resourcegroup);
      VirtualMachineImage result = imageApi.get(imageName);

      String path = String
            .format("/subscriptions/%s/resourcegroups/%s/providers/Microsoft.Compute/images/%s?%s", subscriptionid,
                  resourcegroup, imageName, apiVersion);
      assertSent(server, "GET", path);

      assertNull(result);
   }

   public void listVirtualMachineImages() throws InterruptedException {
      server.enqueue(jsonResponse("/virtualmachineimagelist.json").setResponseCode(200));

      final VirtualMachineImageApi imageApi = api.getVirtualMachineImageApi(resourcegroup);
      List<VirtualMachineImage> result = imageApi.list();

      String path = String
            .format("/subscriptions/%s/resourcegroups/%s/providers/Microsoft.Compute/images?%s", subscriptionid,
                  resourcegroup, apiVersion);
      assertSent(server, "GET", path);

      assertNotNull(result);
      assertTrue(result.size() > 0);
   }

   public void listVirtualMachineImagesReturns404() throws InterruptedException {
      server.enqueue(response404());

      final VirtualMachineImageApi imageApi = api.getVirtualMachineImageApi(resourcegroup);
      List<VirtualMachineImage> result = imageApi.list();

      String path = String
            .format("/subscriptions/%s/resourcegroups/%s/providers/Microsoft.Compute/images?%s", subscriptionid,
                  resourcegroup, apiVersion);
      assertSent(server, "GET", path);

      assertTrue(isEmpty(result));
   }

   public void deleteVirtualMachineImage() throws InterruptedException {
      server.enqueue(response202WithHeader());

      final VirtualMachineImageApi imageApi = api.getVirtualMachineImageApi(resourcegroup);
      URI uri = imageApi.delete(imageName);

      assertEquals(server.getRequestCount(), 1);
      assertNotNull(uri);

      String path = String
            .format("/subscriptions/%s/resourcegroups/%s/providers/Microsoft.Compute/images/%s?%s", subscriptionid,
                  resourcegroup, imageName, apiVersion);
      assertSent(server, "DELETE", path);

      assertTrue(uri.toString().contains("api-version"));
      assertTrue(uri.toString().contains("operationresults"));
   }

   public void deleteVirtualMachineImageDoesNotExist() throws InterruptedException {
      server.enqueue(response404());

      final VirtualMachineImageApi imageApi = api.getVirtualMachineImageApi(resourcegroup);
      URI uri = imageApi.delete(imageName);
      assertNull(uri);

      String path = String
            .format("/subscriptions/%s/resourcegroups/%s/providers/Microsoft.Compute/images/%s?%s", subscriptionid,
                  resourcegroup, imageName, apiVersion);
      assertSent(server, "DELETE", path);
   }

   private VirtualMachineImage newVirtualMachineImage() {
      return VirtualMachineImage.create(imageName, location,
            VirtualMachineImageProperties.builder().sourceVirtualMachine(IdReference.create("vmId")).storageProfile(
                  StorageProfile.create(null, OSDisk.create("Linux", "Ubuntu", null, null, null, null),
                        ImmutableList.<DataDisk> of())).provisioningState("Succeeded").build());
   }
}
