/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2019 wcm.io
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
package io.wcm.handler.url.impl.clientlib;

import org.apache.sling.api.resource.ResourceResolverFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rewrites resource links to client libraries that are in "allowProxy" mode to /etc.clientlibs.
 */
@Component(service = ClientlibProxyRewriter.class, immediate = true)
public class ClientlibProxyRewriterImpl implements ClientlibProxyRewriter {

  private static final String RESOURCES_PATH_SEGMENT = "/resources/";

  private static final Logger log = LoggerFactory.getLogger(ClientlibProxyRewriterImpl.class);

  @Reference
  private ResourceResolverFactory resourceResolverFactory;

  @SuppressWarnings("java:S3077") // volatile is ok here
  private volatile ClientlibPathCache clientlibPathCache;

  @Deactivate
  private void deactivate() {
    if (clientlibPathCache != null) {
      this.clientlibPathCache.close();
    }
    this.clientlibPathCache = null;
  }

  private ClientlibPathCache getClientlibPathCache() {
    if (this.clientlibPathCache == null) {
      // lazy initialization
      synchronized (this) {
        if (this.clientlibPathCache == null) {
          this.clientlibPathCache = new ClientlibPathCache(resourceResolverFactory);
        }
      }
    }
    return this.clientlibPathCache;
  }

  @Override
  public @NotNull String rewriteStaticResourcePath(@NotNull String path) {
    String clientlibPath = getClientlibPath(path);
    if (clientlibPath != null && getClientlibPathCache().isClientlibWithAllowProxy(clientlibPath)) {
      return rewriteClientlibProxyPath(path);
    }
    return path;
  }

  /**
   * Extracts the client library path from a static resource path located below a client library's
   * "resources" folder. Example: for "/apps/myapp/clientlibs/clientlib1/resources/images/img.png"
   * the returned client library path is "/apps/myapp/clientlibs/clientlib1".
   * @param path Static resource path
   * @return Client library path, or null if the path is not a valid /apps or /libs resource path
   */
  private static @Nullable String getClientlibPath(@NotNull String path) {
    if (path.startsWith("/apps/") || path.startsWith("/libs/")) {
      int resourcesIndex = path.lastIndexOf(RESOURCES_PATH_SEGMENT);
      // require at least the "/apps/" or "/libs/" prefix before the "/resources/" segment
      if (resourcesIndex >= "/apps/".length()) {
        return path.substring(0, resourcesIndex);
      }
    }
    return null;
  }

  private String rewriteClientlibProxyPath(String path) {
    // replace /apps or /libs with /etc.clientlibs
    String rewrittenPath = "/etc.clientlibs" + path.substring(5);
    log.debug("Rewrite {} to {}", path, rewrittenPath);
    return rewrittenPath;
  }

}
