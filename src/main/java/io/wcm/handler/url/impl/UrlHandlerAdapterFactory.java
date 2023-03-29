/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2017 wcm.io
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

import java.lang.annotation.Annotation;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.wcm.handler.url.SiteConfig;
import io.wcm.handler.url.spi.UrlHandlerConfig;
import io.wcm.sling.commons.caservice.ContextAwareServiceResolver;

/**
 * Adapts resources or requests to {@link UrlHandlerConfig} and {@link SiteConfig}.
 */
@Component(service = AdapterFactory.class,
    property = {
        AdapterFactory.ADAPTABLE_CLASSES + "=org.apache.sling.api.resource.Resource",
        AdapterFactory.ADAPTABLE_CLASSES + "=org.apache.sling.api.SlingHttpServletRequest",
        AdapterFactory.ADAPTER_CLASSES + "=io.wcm.handler.url.spi.UrlHandlerConfig",
        AdapterFactory.ADAPTER_CLASSES + "=io.wcm.handler.url.SiteConfig"
    })
public class UrlHandlerAdapterFactory implements AdapterFactory {

  @Reference
  private ContextAwareServiceResolver serviceResolver;
  @Reference
  private ConfigurationResourceResolver configurationResourceResolver;

  // cache resolving of site root level per resource path
  private final Cache<String, SiteConfig> siteConfigCache = Caffeine.newBuilder()
      .expireAfterWrite(5, TimeUnit.SECONDS)
      .maximumSize(10000)
      .build();

  @SuppressWarnings({ "unchecked", "null" })
  @Override
  public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
    if (type == UrlHandlerConfig.class) {
      return (AdapterType)serviceResolver.resolve(UrlHandlerConfig.class, (Adaptable)adaptable);
    }
    if (type == SiteConfig.class) {
      return (AdapterType)getSiteConfigForSiteRoot(getContextResource(adaptable));
    }
    return null;
  }

  private Resource getContextResource(Object adaptable) {
    if (adaptable instanceof Resource) {
      return (Resource)adaptable;
    }
    else if (adaptable instanceof SlingHttpServletRequest) {
      return ((SlingHttpServletRequest)adaptable).getResource();
    }
    return null;
  }

  private SiteConfig getSiteConfigForSiteRoot(Resource contextResource) {
    if (contextResource == null) {
      return null;
    }
    String contextRootPath = configurationResourceResolver.getContextPath(contextResource);

    // site root cannot be detected? then get SiteConfig directly from resource without any caching
    if (StringUtils.isBlank(contextRootPath)) {
      return getSiteConfigForResource(contextResource);
    }

    // get site config for site root resource and cache the result (for a short time)
    return siteConfigCache.get(contextRootPath, path -> {
      Resource siteRootResource = contextResource.getResourceResolver().getResource(contextRootPath);
      return getSiteConfigForResourceCacheable(siteRootResource);
    });
  }

  /**
   * Converts the SiteConfig instance to a newly created instance, because the original implementation
   * implements lazy property reading and is connected to the original resource resolver implementation.
   * @param contextResource Context resource
   * @return Cacheable site configuration
   */
  private SiteConfig getSiteConfigForResourceCacheable(Resource contextResource) {
    final SiteConfig siteConfig = getSiteConfigForResource(contextResource);
    return new SiteConfig() {
      @Override
      public Class<? extends Annotation> annotationType() {
        return SiteConfig.class;
      }
      @Override
      public String siteUrl() {
        return siteConfig.siteUrl();
      }
      @Override
      public String siteUrlSecure() {
        return siteConfig.siteUrlSecure();
      }
      @Override
      public String siteUrlAuthor() {
        return siteConfig.siteUrlAuthor();
      }

    };
  }

  private SiteConfig getSiteConfigForResource(Resource contextResource) {
    ConfigurationBuilder configurationBuilder = contextResource.adaptTo(ConfigurationBuilder.class);
    if (configurationBuilder == null) {
      throw new RuntimeException("No configuration builder.");
    }
    return configurationBuilder.as(SiteConfig.class);
  }

}
