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
package io.wcm.handler.url.integrator.impl;

import java.util.Collection;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.day.cq.wcm.api.Page;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.wcm.handler.url.integrator.IntegratorHandler;
import io.wcm.handler.url.integrator.IntegratorMode;
import io.wcm.handler.url.integrator.IntegratorNameConstants;
import io.wcm.handler.url.integrator.IntegratorProtocol;
import io.wcm.handler.url.spi.UrlHandlerConfig;
import io.wcm.sling.commons.request.RequestPath;
import io.wcm.sling.models.annotations.AemObject;

/**
 * Default implementation of a {@link IntegratorHandler}
 */
@Model(adaptables = {
    SlingHttpServletRequest.class, Resource.class
}, adapters = IntegratorHandler.class)
public final class IntegratorHandlerImpl implements IntegratorHandler {

  @Self
  private UrlHandlerConfig urlHandlerConfig;

  // optional injections (only available if called inside a request)
  @SlingObject(injectionStrategy = InjectionStrategy.OPTIONAL)
  private SlingHttpServletRequest request;
  @AemObject(injectionStrategy = InjectionStrategy.OPTIONAL)
  private Page currentPage;

  private boolean integratorTemplateMode;
  private boolean integratorTemplateSecureMode;

  @PostConstruct
  private void postConstruct() {
    detectIntegratorTemplateModes();
  }

  /**
   * Detect integrator template modes - check selectors in current url.
   */
  private void detectIntegratorTemplateModes() {
    // integrator mode cannot be active if no modes defined
    if (urlHandlerConfig.getIntegratorModes().isEmpty()) {
      return;
    }
    if (request != null && RequestPath.hasSelector(request, SELECTOR_INTEGRATORTEMPLATE_SECURE)) {
      integratorTemplateSecureMode = true;
    }
    else if (request != null && RequestPath.hasSelector(request, SELECTOR_INTEGRATORTEMPLATE)) {
      integratorTemplateMode = true;
    }
  }

  /**
   * Checks if current request is in integrator template oder integrator template secure mode.
   * @return true if in integrator template or integrator template secure mode
   */
  @Override
  public boolean isIntegratorTemplateMode() {
    return integratorTemplateMode || integratorTemplateSecureMode;
  }

  /**
   * Checks if current request is in integrator template secure mode.
   * @return true if in integrator template secure mode
   */
  @Override
  public boolean isIntegratorTemplateSecureMode() {
    return integratorTemplateSecureMode;
  }

  /**
   * Returns selector for integrator template mode.
   * In HTTPS mode the secure selector is returned, otherwise the default selector.
   * HTTPS mode is active if the current page is an integrator page and has simple mode-HTTPs activated, or
   * the secure integrator mode selector is included in the current request.
   * @return Integrator template selector
   */
  @Override
  public @NotNull String getIntegratorTemplateSelector() {
    if (currentPage != null && urlHandlerConfig.isIntegrator(currentPage)) {
      if (isResourceUrlSecure(currentPage)) {
        return SELECTOR_INTEGRATORTEMPLATE_SECURE;
      }
      else {
        return SELECTOR_INTEGRATORTEMPLATE;
      }
    }
    if (integratorTemplateSecureMode) {
      return SELECTOR_INTEGRATORTEMPLATE_SECURE;
    }
    else {
      return SELECTOR_INTEGRATORTEMPLATE;
    }
  }

  /**
   * Get integrator mode configured for the current page.
   * @return Integrator mode (simple or extended)
   */
  @Override
  public @NotNull IntegratorMode getIntegratorMode() {
    return getIntegratorMode(currentPage);
  }

  @Override
  public @NotNull IntegratorMode getIntegratorMode(@Nullable Page page) {
    ValueMap props = getPagePropertiesNullSafe(page);
    return getIntegratorMode(props);
  }


  /**
   * Read integrator mode from content container. Defaults to first integrator mode defined.
   * @param properties Content container
   * @return Integrator mode
   */
  @SuppressWarnings({ "null", "java:S2637" })
  @SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION")
  private @NotNull IntegratorMode getIntegratorMode(ValueMap properties) {
    IntegratorMode mode = null;
    Collection<IntegratorMode> integratorModes = urlHandlerConfig.getIntegratorModes();
    String modeString = properties.get(IntegratorNameConstants.PN_INTEGRATOR_MODE, String.class);
    if (StringUtils.isNotEmpty(modeString)) {
      for (IntegratorMode candidate : integratorModes) {
        if (StringUtils.equals(modeString, candidate.getId())) {
          mode = candidate;
          break;
        }
      }
    }
    // fallback to first mode defined in configuration
    if (mode == null && !integratorModes.isEmpty()) {
      mode = integratorModes.iterator().next();
    }
    return mode;
  }

  /**
   * Read integrator protocol from content container. Default to AUTO.
   * @param properties Content container
   * @return Integrator protocol
   */
  @SuppressWarnings("null")
  private IntegratorProtocol getIntegratorProtocol(ValueMap properties) {
    IntegratorProtocol protocol = IntegratorProtocol.AUTO;
    try {
      String protocolString = properties.get(IntegratorNameConstants.PN_INTEGRATOR_PROTOCOL, String.class);
      if (StringUtils.isNotEmpty(protocolString)) {
        protocol = IntegratorProtocol.valueOf(protocolString.toUpperCase());
      }
    }
    catch (IllegalArgumentException ex) {
      // ignore
    }
    return protocol;
  }

  /**
   * Checks whether resource URLs should be rendered in secure mode or not.
   * @return true if resource URLs should be rendered in secure mode
   */
  private boolean isResourceUrlSecure(Page page) {
    ValueMap props = getPagePropertiesNullSafe(page);
    IntegratorMode mode = getIntegratorMode(props);
    if (mode.isDetectProtocol()) {
      IntegratorProtocol integratorProtocol = getIntegratorProtocol(props);
      if (integratorProtocol == IntegratorProtocol.HTTPS) {
        return true;
      }
      else if (integratorProtocol == IntegratorProtocol.AUTO) {
        return RequestPath.hasSelector(request, SELECTOR_INTEGRATORTEMPLATE_SECURE);
      }

    }
    return false;
  }

  /**
   * Get valuemap/papge properties of current page.
   * @param page Page
   * @return Value map of current page or empty map if current page is null.
   */
  private static ValueMap getPagePropertiesNullSafe(Page page) {
    if (page != null) {
      return page.getProperties();
    }
    else {
      return ValueMap.EMPTY;
    }
  }

}
