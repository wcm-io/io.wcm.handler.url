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
package io.wcm.handler.url.impl.modes;

import java.util.Set;

import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.day.cq.wcm.api.Page;

import io.wcm.handler.url.integrator.IntegratorHandler;
import io.wcm.handler.url.integrator.IntegratorPlaceholder;
import io.wcm.sling.commons.adapter.AdaptTo;
import io.wcm.wcm.commons.util.RunMode;

/**
 * Enforce the generation of a full URL with protocol and hostname and non-secure mode.
 */
@SuppressWarnings("java:S2160") // equals is implemented via AbstractUrlMode
public final class FullUrlForceNonSecureUrlMode extends AbstractUrlMode {

  private final boolean forcePublish;

  /**
   * @param forcePublish Force to select publish URLs even on author instance
   */
  public FullUrlForceNonSecureUrlMode(boolean forcePublish) {
    this.forcePublish = forcePublish;
  }

  @Override
  public @NotNull String getId() {
    return "FULL_URL_FORCENONSECURE";
  }

  @SuppressWarnings("deprecation")
  @Override
  public String getLinkUrlPrefix(@NotNull Adaptable adaptable, @NotNull Set<String> runModes,
      @Nullable Page currentPage, @Nullable Page targetPage) {

    // if integrator template mode with placeholders is active return link url placeholder
    IntegratorHandler integratorHandler = AdaptTo.notNull(adaptable, IntegratorHandler.class);
    if (integratorHandler.isIntegratorTemplateMode()
        && integratorHandler.getIntegratorMode().isUseUrlPlaceholders()) {
      return IntegratorPlaceholder.URL_CONTENT;
    }

    UrlConfig config = getUrlConfigForTarget(adaptable, targetPage);

    // in author mode return author site url
    if (!forcePublish && RunMode.isAuthor(runModes) && config.hasSiteUrlAuthor()) {
      return config.getSiteUrlAuthor();
    }

    // return non-secure site url
    return config.getSiteUrl();
  }

  @SuppressWarnings("deprecation")
  @Override
  public String getResourceUrlPrefix(@NotNull Adaptable adaptable, @NotNull Set<String> runModes,
      @Nullable Page currentPage, @Nullable Resource targetResource) {

    // if integrator template mode with placeholders is active return resource url placeholder
    IntegratorHandler integratorHandler = AdaptTo.notNull(adaptable, IntegratorHandler.class);
    if (integratorHandler.isIntegratorTemplateMode()
        && integratorHandler.getIntegratorMode().isUseUrlPlaceholders()) {
      return IntegratorPlaceholder.URL_CONTENT_PROXY;
    }

    UrlConfig config = getUrlConfigForTarget(adaptable, targetResource);

    // in author mode return author site url
    if (!forcePublish && RunMode.isAuthor(runModes) && config.hasSiteUrlAuthor()) {
      return config.getSiteUrlAuthor();
    }

    // return non-secure site url
    return config.getSiteUrl();
  }

}
