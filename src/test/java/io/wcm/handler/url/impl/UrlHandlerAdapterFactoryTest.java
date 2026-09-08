/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2026 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.handler.url.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.wcm.handler.url.SiteConfig;
import io.wcm.handler.url.testcontext.AppAemContext;
import io.wcm.sling.commons.caservice.ContextAwareServiceResolver;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
@SuppressWarnings("null")
class UrlHandlerAdapterFactoryTest {

  private final AemContext context = AppAemContext.newAemContext();

  @Test
  void testGetSiteConfig_FromResource() {
    Resource resource = context.create().resource("/content/unittest/de_test/brand/de/section/page/jcr:content/comp");

    SiteConfig siteConfig = resource.adaptTo(SiteConfig.class);
    assertNotNull(siteConfig);
    assertEquals("http://de.dummysite.org", siteConfig.siteUrl());
    assertEquals("https://de.dummysite.org", siteConfig.siteUrlSecure());
    assertEquals("https://author.dummysite.org", siteConfig.siteUrlAuthor());
  }

  @Test
  void testGetSiteConfig_FromRequest() {
    context.currentResource(context.create().resource("/content/unittest/de_test/brand/en/section/page/jcr:content/comp"));

    SiteConfig siteConfig = context.request().adaptTo(SiteConfig.class);
    assertNotNull(siteConfig);
    assertEquals("http://en.dummysite.org", siteConfig.siteUrl());
    assertEquals("https://en.dummysite.org", siteConfig.siteUrlSecure());
    assertEquals("https://author.dummysite.org", siteConfig.siteUrlAuthor());
  }

  @Test
  void testGetSiteConfig_CachedResult() {
    Resource resource1 = context.create().resource("/content/unittest/de_test/brand/de/section/page/jcr:content/comp1");
    Resource resource2 = context.create().resource("/content/unittest/de_test/brand/de/section2/page2/jcr:content/comp2");

    // both resources resolve to the same site root, second call is served from cache
    SiteConfig siteConfig1 = resource1.adaptTo(SiteConfig.class);
    SiteConfig siteConfig2 = resource2.adaptTo(SiteConfig.class);
    assertNotNull(siteConfig1);
    assertNotNull(siteConfig2);
    assertEquals("http://de.dummysite.org", siteConfig1.siteUrl());
    assertEquals("http://de.dummysite.org", siteConfig2.siteUrl());
  }

  @Test
  void testGetSiteConfig_NullResource() {
    SiteConfig siteConfig = context.resourceResolver().adaptTo(SiteConfig.class);
    assertNull(siteConfig);
  }

  @Test
  void testGetSiteConfig_UnsupportedAdaptable() {
    UrlHandlerAdapterFactory factory = new UrlHandlerAdapterFactory();
    assertNull(factory.getAdapter("a string", SiteConfig.class));
  }

  /**
   * Ensures the null-safety fix: if a site root path is detected but no resource exists at that path,
   * no NPE is thrown and null is returned.
   */
  @Test
  void testGetSiteConfig_SiteRootResourceNotResolvable() {
    AemContext mockContext = new AemContext();
    ConfigurationResourceResolver configurationResourceResolver = mock(ConfigurationResourceResolver.class);
    mockContext.registerService(ContextAwareServiceResolver.class, mock(ContextAwareServiceResolver.class));
    mockContext.registerService(ConfigurationResourceResolver.class, configurationResourceResolver);
    UrlHandlerAdapterFactory factory = mockContext.registerInjectActivateService(new UrlHandlerAdapterFactory());

    Resource contextResource = mock(Resource.class);
    ResourceResolver resourceResolver = mock(ResourceResolver.class);
    when(configurationResourceResolver.getContextPath(contextResource)).thenReturn("/content/not/existing");
    when(contextResource.getResourceResolver()).thenReturn(resourceResolver);
    when(resourceResolver.getResource("/content/not/existing")).thenReturn(null);

    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    when(request.getResource()).thenReturn(contextResource);

    assertNull(factory.getAdapter(request, SiteConfig.class));
  }

}
