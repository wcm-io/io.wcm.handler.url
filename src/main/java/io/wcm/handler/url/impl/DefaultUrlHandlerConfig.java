/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2014 wcm.io
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

import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.wcm.handler.url.SiteRootDetector;
import io.wcm.handler.url.spi.UrlHandlerConfig;
import io.wcm.sling.commons.caservice.ContextAwareService;

/**
 * Default implementation of configuration options of {@link UrlHandlerConfig} interface.
 */
@Component(service = UrlHandlerConfig.class, property = {
    Constants.SERVICE_RANKING + ":Integer=" + Integer.MIN_VALUE,
    ContextAwareService.PROPERTY_ACCEPTS_CONTEXT_PATH_EMPTY + ":Boolean=true"
})
public class DefaultUrlHandlerConfig extends UrlHandlerConfig {

  @Reference
  private SiteRootDetector siteRootDetector;

  @Override
  public int getSiteRootLevel(@Nullable Resource contextResource) {
    // default to detection via SiteRootDetector
    return siteRootDetector.getSiteRootLevel(contextResource);
  }

}
