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

import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.day.cq.wcm.api.Page;

import io.wcm.handler.url.UrlBuilder;
import io.wcm.handler.url.UrlMode;
import io.wcm.handler.url.VanityMode;

/**
 * Default implementation or {@link UrlBuilder}.
 */
final class UrlBuilderImpl implements UrlBuilder {

  private final UrlHandlerImpl urlHandler;
  private final String path;
  private final Resource resource;
  private final Page page;

  private String selectors;
  private String extension;
  private String suffix;
  private String queryString;
  private Set<String> inheritableParameterNames;
  private String fragment;
  private UrlMode urlMode;
  private VanityMode vanityMode;
  private boolean disableSuffixSelector;

  /**
   * @param path Path for URL (without any hostname, scheme, extension, suffix etc.)
   * @param urlHandler Url handler instance
   */
  UrlBuilderImpl(String path, UrlHandlerImpl urlHandler) {
    this.path = path;
    this.resource = null;
    this.page = null;
    this.urlHandler = urlHandler;
  }

  /**
   * @param resource Resource
   * @param urlHandler Url handler instance
   */
  UrlBuilderImpl(Resource resource, UrlHandlerImpl urlHandler) {
    this.path = resource != null ? resource.getPath() : null;
    this.resource = resource;
    this.page = null;
    this.urlHandler = urlHandler;
  }

  /**
   * @param page Page
   * @param urlHandler Url handler instance
   */
  UrlBuilderImpl(Page page, UrlHandlerImpl urlHandler) {
    this.path = page != null ? page.getPath() : null;
    this.resource = null;
    this.page = page;
    this.urlHandler = urlHandler;
  }

  @Override
  public @NotNull UrlBuilder selectors(@Nullable String value) {
    this.selectors = value;
    return this;
  }

  @Override
  public @NotNull UrlBuilder extension(@Nullable String value) {
    this.extension = value;
    return this;
  }

  @Override
  public @NotNull UrlBuilder suffix(@Nullable String value) {
    this.suffix = value;
    return this;
  }

  @Override
  public @NotNull UrlBuilder queryString(@Nullable String value) {
    this.queryString = value;
    this.inheritableParameterNames = null;
    return this;
  }

  @Override
  public @NotNull UrlBuilder queryString(@Nullable String value, @NotNull Set<String> inheritableParamNames) {
    this.queryString = value;
    this.inheritableParameterNames = inheritableParamNames;
    return this;
  }

  @Override
  public @NotNull UrlBuilder fragment(@Nullable String value) {
    this.fragment = value;
    return this;
  }

  @Override
  public @NotNull UrlBuilder urlMode(@Nullable UrlMode value) {
    this.urlMode = value;
    return this;
  }

  @Override
  public @NotNull UrlBuilder vanityMode(@Nullable VanityMode value) {
    this.vanityMode = value;
    return this;
  }


  @Override
  public @NotNull UrlBuilder disableSuffixSelector(boolean value) {
    this.disableSuffixSelector = value;
    return this;
  }

  private String build(boolean externalize) {
    String pathToUse = path;
    VanityMode vanityModeToUse = ObjectUtils.defaultIfNull(vanityMode, urlHandler.getDefaultVanityMode());
    if (page != null && (vanityModeToUse == VanityMode.ALWAYS || (externalize && vanityModeToUse == VanityMode.EXTERNALIZE))) {
      pathToUse = StringUtils.defaultString(page.getVanityUrl(), path);
    }

    String url = urlHandler.buildUrl(pathToUse, selectors, extension, suffix, disableSuffixSelector);
    if (StringUtils.isNotEmpty(queryString) || inheritableParameterNames != null) {
      url = urlHandler.appendQueryString(url, queryString, inheritableParameterNames);
    }
    if (StringUtils.isNotEmpty(fragment)) {
      url = urlHandler.setFragment(url, fragment);
    }
    return url;
  }

  @Override
  public String build() {
    return build(false);
  }

  @Override
  public String buildExternalLinkUrl() {
    return buildExternalLinkUrl(null);
  }

  @Override
  public String buildExternalLinkUrl(@Nullable Page targetPage) {
    Page targetPageToUse = targetPage;
    if (targetPageToUse == null) {
      targetPageToUse = page;
    }
    if (targetPageToUse == null && resource != null) {
      targetPageToUse = resource.adaptTo(Page.class);
    }
    String url = build(true);
    return urlHandler.externalizeLinkUrl(url, targetPageToUse, urlMode);
  }

  @Override
  public String buildExternalResourceUrl() {
    return buildExternalResourceUrl(null);
  }

  @Override
  public String buildExternalResourceUrl(@Nullable Resource targetResource) {
    Resource targetResourceToUse = targetResource;
    if (targetResourceToUse == null) {
      targetResourceToUse = resource;
    }
    if (targetResourceToUse == null && page != null) {
      targetResourceToUse = page.adaptTo(Resource.class);
    }
    String url = build(true);
    return urlHandler.externalizeResourceUrl(url, targetResourceToUse, urlMode);
  }

}
