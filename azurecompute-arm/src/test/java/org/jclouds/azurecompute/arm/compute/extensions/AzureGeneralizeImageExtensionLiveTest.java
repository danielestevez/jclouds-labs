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
package org.jclouds.azurecompute.arm.compute.extensions;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jclouds.compute.options.RunScriptOptions.Builder.overrideAuthenticateSudo;
import static org.jclouds.util.Predicates2.retry;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import javax.annotation.Resource;
import javax.inject.Named;

import org.jclouds.azurecompute.arm.AzureComputeProviderMetadata;
import org.jclouds.azurecompute.arm.domain.ResourceGroup;
import org.jclouds.azurecompute.arm.internal.AzureLiveTestUtils;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.ImageTemplate;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.extensions.ImageExtension;
import org.jclouds.compute.internal.BaseComputeServiceContextLiveTest;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.logging.Logger;
import org.jclouds.providers.ProviderMetadata;
import org.jclouds.ssh.SshClient;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;

/**
 * Live tests for the {@link org.jclouds.compute.extensions.ImageExtension}
 * integration.
 */
@Test(groups = "live", singleThreaded = true, testName = "AzureGeneralizeImageExtensionLiveTest")
public class AzureGeneralizeImageExtensionLiveTest extends BaseComputeServiceContextLiveTest {

   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;

   public static final String NAME_PREFIX = "%s";

   private String preparedNodeId = "eastus/vm2delete";

   //   private String imageId = "eastus/jcloudstest-eastus/jcloudstesteastus756/custom/imagefromportal";

   private String imageId = "eastus/imagecreatedgroup";

   private String imageGroup = "imagecreatedgroup";

   private LoadingCache<String, ResourceGroup> resourceGroupMap;

   private NodeMetadata node;

   public AzureGeneralizeImageExtensionLiveTest() {
      provider = "azurecompute-arm";
   }

   public void initializeContext() {
      super.initializeContext();
      resourceGroupMap = context.utils().injector()
            .getInstance(Key.get(new TypeLiteral<LoadingCache<String, ResourceGroup>>() {
            }));
   }

   @Test(groups = { "integration", "live" }, enabled = false, singleThreaded = true)
   public void testCreateImage() throws RunNodesException, InterruptedException, ExecutionException {
      ComputeService computeService = view.getComputeService();
      Optional<ImageExtension> imageExtension = computeService.getImageExtension();
      assertTrue(imageExtension.isPresent(), "image extension was not present");

      //TODO create new VM with script
      //      view.getComputeService().createNodesInGroup(group, count, availabilitySet(as).runScript("el comando"))
      //      LoginCredentials creds = view.getComputeService().getNodeMetadata
      // ("eastus/virtualmachineimageapilivetest-97c")
      //            .getCredentials();

      //      Template template = getNodeTemplate().forceCacheReload().build();
      //      NodeMetadata node = Iterables.getOnlyElement(computeService.createNodesInGroup(imageGroup, 1, template));

      //      String nodeId = "eastus/vm2image";
      //      NodeMetadata node = computeService.getNodeMetadata(nodeId);
      //      checkReachable(node);
      //
      //      prepareNodeForImageCreation(node);
      //      checkNotReachable(node);

      logger.info("Creating image from node %s, started with template: %s", node, "template");
      //      ImageTemplate newImageTemplate = imageExtension.get().buildImageTemplateFromNode(imageGroup, node.getId
      // ());
      ImageTemplate newImageTemplate = imageExtension.get().buildImageTemplateFromNode(imageGroup, preparedNodeId);

      Image image = imageExtension.get().createImage(newImageTemplate).get();
      logger.info("Image created: %s", image);

      assertEquals(imageGroup, image.getName());

      imageId = image.getId();
      //      computeService.destroyNode(node.getId());

      Optional<? extends Image> optImage = getImage();
      assertTrue(optImage.isPresent());
   }

   protected void prepareNodeForImageCreation(NodeMetadata node) {
      logger.info("prepareNodeForImageCreation script init...");
      view.getComputeService().runScriptOnNode(node.getId(), "waagent -deprovision+user -force",
            overrideAuthenticateSudo(true).overrideLoginUser("azureabiquo").overrideLoginPassword("azureabiqu0!"));
      logger.info("prepareNodeForImageCreation script init...");
   }

   @Test(groups = { "integration", "live" }, /*dependsOnMethods = "testCreateImage",*/ singleThreaded = true)
   public void testSpawnNodeFromImage() throws RunNodesException {
      ComputeService computeService = view.getComputeService();
      Optional<? extends Image> optImage = getImage();
      assertTrue(optImage.isPresent());

      NodeMetadata node = Iterables
            .getOnlyElement(computeService.createNodesInGroup("grouptestfakegeneralize", 1, getNodeTemplate()
                  // fromImage does not use the arg image's id (but we do need to set location)
                  .imageId(optImage.get().getId()).fromImage(optImage.get()).build()));

      checkReachable(node);
      view.getComputeService().destroyNode(node.getId());
   }

   @AfterClass(groups = "live", alwaysRun = false)
   protected void tearDownContext() {
      //      try {
      //         Location location = getNodeTemplate().build().getLocation();
      //         ResourceGroup rg = resourceGroupMap.getIfPresent(location.getId());
      //         if (rg != null) {
      //            AzureComputeApi api = view.unwrapApi(AzureComputeApi.class);
      //            api.getResourceGroupApi().delete(rg.name());
      //         }

      //      } finally {
      //         super.tearDownContext();
      //      }
   }

   protected Module getSshModule() {
      return new SshjSshClientModule();
   }

   protected Properties setupProperties() {
      Properties properties = super.setupProperties();
      AzureLiveTestUtils.defaultProperties(properties);
      setIfTestSystemPropertyPresent(properties, "oauth.endpoint");
      return properties;
   }

   protected ProviderMetadata createProviderMetadata() {
      return AzureComputeProviderMetadata.builder().build();
   }

   //   public TemplateBuilder getNodeTemplate() {
   //      Map<String, String> keyPair = ComputeTestUtils.setupKeyPair();
   //      return super.getNodeTemplate()
   //            .options(authorizePublicKey(keyPair.get("public")).overrideLoginPrivateKey(keyPair.get("private")));
   //   }

   public TemplateBuilder getNodeTemplate() {
      TemplateBuilder templateBuilder = view.getComputeService().templateBuilder();
      if (templateBuilderSpec != null) {
         templateBuilder = templateBuilder.from(templateBuilderSpec);
      }
      return templateBuilder;
   }

   private void checkReachable(NodeMetadata node) {
      SshClient client = view.utils().sshForNode().apply(node);
      assertTrue(retry(new Predicate<SshClient>() {
         public boolean apply(SshClient input) {
            input.connect();
            if (input.exec("id").getExitStatus() == 0) {
               return true;
            }
            return false;
         }
      }, getSpawnNodeMaxWait(), 1L, SECONDS).apply(client));
   }

   private void checkNotReachable(NodeMetadata node) {
      SshClient client = view.utils().sshForNode().apply(node);
      assertFalse(retry(new Predicate<SshClient>() {
         public boolean apply(SshClient input) {
            input.connect();
            if (input.exec("id").getExitStatus() == 0) {
               return true;
            }
            return false;
         }
      }, getSpawnNodeMaxWait(), 1L, SECONDS).apply(client));
   }

   /**
    * Returns the maximum amount of time (in seconds) to wait for a node spawned from the new image
    * to become available, override to increase this time.
    *
    * @return
    */
   public long getSpawnNodeMaxWait() {
      return 600L;
   }

   private Optional<? extends Image> getImage() {
      return Optional.fromNullable(view.getComputeService().getImage(imageId));
   }

}
