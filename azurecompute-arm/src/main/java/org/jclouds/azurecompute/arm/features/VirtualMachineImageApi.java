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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.net.URI;
import java.util.List;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jclouds.Fallbacks;
import org.jclouds.Fallbacks.EmptyListOnNotFoundOr404;
import org.jclouds.Fallbacks.NullOnNotFoundOr404;
import org.jclouds.azurecompute.arm.domain.VirtualMachineImage;
import org.jclouds.azurecompute.arm.domain.VirtualMachineImageProperties;
import org.jclouds.azurecompute.arm.filters.ApiVersionFilter;
import org.jclouds.azurecompute.arm.functions.URIParser;
import org.jclouds.oauth.v2.filters.OAuthFilter;
import org.jclouds.rest.annotations.Fallback;
import org.jclouds.rest.annotations.MapBinder;
import org.jclouds.rest.annotations.PayloadParam;
import org.jclouds.rest.annotations.RequestFilters;
import org.jclouds.rest.annotations.ResponseParser;
import org.jclouds.rest.annotations.SelectJson;
import org.jclouds.rest.binders.BindToJsonPayload;

/**
 * The Azure Resource Management API includes operations for managing custom images in your subscription.
 */
@Path("/resourcegroups/{resourcegroup}/providers/Microsoft.Compute/images")
@RequestFilters({ OAuthFilter.class, ApiVersionFilter.class })
@Consumes(APPLICATION_JSON)
public interface VirtualMachineImageApi {

   /**
    * Lists the virtual machine images in the specified resource group.
    */
   @Named("virtualmachineimage:list")
   @GET
   @SelectJson("value")
   @Fallback(EmptyListOnNotFoundOr404.class)
   List<VirtualMachineImage> list();

   /**
    * Gets information about the specified virtual machine image.
    */
   @Named("virtualmachineimage:get")
   @Path("/{imagename}")
   @GET
   @Fallback(NullOnNotFoundOr404.class)
   VirtualMachineImage get(@PathParam("imagename") String imageName);

   /**
    * Creates a virtual machine image using the specified blob, snapshot, managed disk, or existing virtual machine.+
    */
   @Named("virtualmachineimage:create")
   @Path("/{imagename}")
   @PUT
   @MapBinder(BindToJsonPayload.class)
   @Produces(MediaType.APPLICATION_JSON)
   VirtualMachineImage create(@PathParam("imagename") String imageName, @PayloadParam("location") String location,
         @PayloadParam("properties") VirtualMachineImageProperties properties);

   /**
    * Deletes the specified virtual machine image.
    */
   @Named("virtualmachineimage:delete")
   @Path("/{imagename}")
   @DELETE
   @ResponseParser(URIParser.class)
   @Fallback(Fallbacks.NullOnNotFoundOr404.class)
   URI delete(@PathParam("imagename") String imageName);
}
