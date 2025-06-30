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
package io.wcm.handler.url.suffix;

import static io.wcm.handler.url.suffix.impl.UrlSuffixUtil.ESCAPE_DELIMITER;
import static io.wcm.handler.url.suffix.impl.UrlSuffixUtil.SUFFIX_PART_DELIMITER;
import static io.wcm.handler.url.suffix.impl.UrlSuffixUtil.hexCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Predicate;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.day.cq.wcm.api.Page;

import io.wcm.handler.url.testcontext.AppAemContext;
import io.wcm.sling.commons.resource.ImmutableValueMap;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith(AemContextExtension.class)
@SuppressWarnings("null")
class SuffixParserTest {

  private final AemContext context = AppAemContext.newAemContext();

  private static final String DEFAULT_RESOURCE_TYPE = "test/resourceType";

  private static final String ESCAPED_SLASH = ESCAPE_DELIMITER + hexCode('/');

  private SuffixParser getParserWithIncomingSuffix(final String urlEncodedSuffix) {
    return this.getParserWithIncommingSuffix(urlEncodedSuffix, null);
  }

  private SuffixParser getParserWithIncommingSuffix(final String urlEncodedSuffix, Page currentPage) {
    // simulate current page and suffix in this test's context context
    setContextAttributes(urlEncodedSuffix, currentPage);

    // create a UrlSuffixHelper that doesn't keep any state
    return new SuffixParser(context.request());
  }

  private void setContextAttributes(final String urlEncodedSuffix, Page currentPage) {
    String decodedSuffix = null;
    if (urlEncodedSuffix != null) {
      try {
        decodedSuffix = URLDecoder.decode(urlEncodedSuffix, StandardCharsets.UTF_8.name());
      }
      catch (UnsupportedEncodingException ex) {
        throw new RuntimeException("Unsupported encoding.", ex);
      }
    }
    context.requestPathInfo().setSuffix(decodedSuffix);

    if (currentPage != null) {
      context.currentPage(currentPage);
    }
  }

  private Resource createResource(String path) {
    return this.createResource(path, DEFAULT_RESOURCE_TYPE);
  }

  private Resource createResource(String path, String resourceType) {
    return context.create().resource(path, ImmutableValueMap.builder()
        .put(ResourceResolver.PROPERTY_RESOURCE_TYPE, resourceType)
        .build());
  }

  @Test
  void testGetLongIntBooleanStringString() {
    // create UrlSuffixHelper with single-part suffix in request
    String suffix = "/abc=def";
    SuffixParser parser = getParserWithIncomingSuffix(suffix);
    // reading existing key with and without default value should return the right value
    assertEquals("def", parser.get("abc", "default"));
    assertEquals("def", parser.get("abc", String.class));
    // reading a non-existing key should return the default value (which can be null!)
    assertEquals("default", parser.get("def", "default"));
    assertNull(parser.get("def", String.class));


    // create UrlSuffixHelper with null suffix in request
    parser = getParserWithIncomingSuffix(null);
    // reading any key should return the default value (which can be null!)
    assertEquals("default", parser.get("abc", "default"));
    assertNull(parser.get("abc", String.class));


    // create UrlSuffixHelper with empty suffix in request
    parser = getParserWithIncomingSuffix("");
    // reading any key should return the default value (which can be null!)
    assertEquals("default", parser.get("abc", "default"));
    assertNull(parser.get("abc", String.class));


    // create UrlSuffixHelper with empty *value* in request
    parser = getParserWithIncomingSuffix("/abc=");
    // reading the key should return the empty string
    assertEquals("", parser.get("abc", "default"));


    // create UrlSuffixHelper with additional extension in suffix
    parser = getParserWithIncomingSuffix("/abc=def.html");
    // reading the key should return the empty string
    assertEquals("def", parser.get("abc", "default"));
  }

  @Test
  void testGetLongIntBooleanStringBoolean() {
    // create UrlSuffixHelper with single-part suffix in request
    SuffixParser parser = getParserWithIncomingSuffix("/abc=true");
    // reading existing key with any default value should return the right value
    assertEquals(true, parser.get("abc", true));
    assertEquals(true, parser.get("abc", false));
    // reading a non-existing key should return the default value
    assertEquals(true, parser.get("def", true));
    assertEquals(false, parser.get("def", false));
    // test with type
    assertEquals(true, parser.get("abc", Boolean.class));
    assertEquals(false, parser.get("def", Boolean.class));


    // create UrlSuffixHelper with null suffix in request
    parser = getParserWithIncomingSuffix(null);
    // reading any key should return the default value
    assertEquals(true, parser.get("def", true));
    assertEquals(false, parser.get("def", false));


    // create UrlSuffixHelper with empty suffix in request
    parser = getParserWithIncomingSuffix("");
    // reading any key should return the default value
    assertEquals(true, parser.get("def", true));
    assertEquals(false, parser.get("def", false));


    // create UrlSuffixHelper with invalid boolean value
    parser = getParserWithIncomingSuffix("/abc=Ger");
    // reading the key should return *false*, as default value is only used if parameter is not set
    assertEquals(true, parser.get("abc", true));
    assertEquals(false, parser.get("abc", false));
  }

  @Test
  void testGetLongIntBooleanStringInt() {
    // create UrlSuffixHelper with single-part suffix in request
    SuffixParser parser = getParserWithIncomingSuffix("/abc=123");
    // reading existing key with any default value should return the right value
    assertEquals(123, (int)parser.get("abc", 123));
    // reading a non-existing key should return the default value
    assertEquals(456, (int)parser.get("def", 456));
    // test with type
    assertEquals(123, (int)parser.get("abc", Integer.class));
    assertEquals(0, (int)parser.get("def", Integer.class));


    // create UrlSuffixHelper with null suffix in request
    parser = getParserWithIncomingSuffix(null);
    // reading any key should return the default value
    assertEquals(123, (int)parser.get("def", 123));


    // create UrlSuffixHelper with empty suffix in request
    parser = getParserWithIncomingSuffix("");
    // reading any key should return the default value
    assertEquals(true, parser.get("def", true));
    assertEquals(false, parser.get("def", false));


    // create UrlSuffixHelper with invalid value in request
    parser = getParserWithIncomingSuffix("/abc=def");
    // reading any key should return the default value
    assertEquals(123, (int)parser.get("abc", 123));
    assertEquals(123, (int)parser.get("def", 123));


    // create UrlSuffixHelper with empty value in request
    parser = getParserWithIncomingSuffix("/abc=");
    // reading any key should return the default value
    assertEquals(123, (int)parser.get("abc", 123));
    assertEquals(123, (int)parser.get("def", 123));
  }

  @Test
  void testGetLongIntBooleanStringLong() {
    // create UrlSuffixHelper with single-part suffix in request
    SuffixParser parser = getParserWithIncomingSuffix("/abc=123");
    // reading existing key with any default value should return the right value
    assertEquals(123L, (long)parser.get("abc", 123L));
    // reading a non-existing key should return the default value
    assertEquals(456L, (long)parser.get("def", 456L));
    // test with type
    assertEquals(123L, (long)parser.get("abc", Long.class));
    assertEquals(0L, (long)parser.get("def", Long.class));


    // create UrlSuffixHelper with null suffix in request
    parser = getParserWithIncomingSuffix(null);
    // reading any key should return the default value
    assertEquals(123L, (long)parser.get("def", 123L));


    // create UrlSuffixHelper with empty suffix in request
    parser = getParserWithIncomingSuffix("");
    // reading any key should return the default value
    assertEquals(true, parser.get("def", true));
    assertEquals(false, parser.get("def", false));


    // create UrlSuffixHelper with invalid value in request
    parser = getParserWithIncomingSuffix("/abc=def");
    // reading any key should return the default value
    assertEquals(123L, (long)parser.get("abc", 123L));
    assertEquals(123L, (long)parser.get("def", 123L));


    // create UrlSuffixHelper with empty value in request
    parser = getParserWithIncomingSuffix("/abc=");
    // reading any key should return the default value
    assertEquals(123L, (long)parser.get("abc", 123L));
    assertEquals(123L, (long)parser.get("def", 123L));
  }

  @Test
  void testGetLongIntBooleanStringResource() {

    // create a page and a resource within the page
    Page currentPage = context.create().page("/content/a", "template", "title");
    Resource targetResource = createResource("/content/a/jcr:content/b/c");

    // get the existing page with relative path suffix
    SuffixParser parser = getParserWithIncommingSuffix(ESCAPED_SLASH + "b" + ESCAPED_SLASH + "c", currentPage);
    Resource suffixResource = parser.getResource();
    // check that the target resource is found
    assertNotNull(suffixResource);
    assertEquals(targetResource.getPath(), suffixResource.getPath());


    // non-existing resource in suffix
    parser = getParserWithIncommingSuffix(ESCAPED_SLASH + "b" + ESCAPED_SLASH + "d", currentPage);
    suffixResource = parser.getResource();
    // should return null
    assertNull(suffixResource);


    // don't crash with null-suffix
    parser = getParserWithIncommingSuffix(null, currentPage);
    assertNull(parser.getResource());
  }

  @Test
  void testGetLongIntBooleanStringResourceString() {

    // create a page that and a resource that within the page
    String resourceType = "theResourceType";
    Page basePage = context.create().page("/content/a", "template", "title");
    Resource targetResource = createResource("/content/a/jcr:content/b/c", resourceType);

    // get the resource by path (relative to the page) and resource type filter
    SuffixParser parser = getParserWithIncomingSuffix(ESCAPED_SLASH + "b" + ESCAPED_SLASH + "c");
    Resource suffixResource = parser.getResource(basePage.getContentResource());

    // check that the right target resource is found
    assertNotNull(suffixResource);
    assertEquals(targetResource.getPath(), suffixResource.getPath());


    // don't crash if a non-existing path specified in suffix
    parser = getParserWithIncomingSuffix(ESCAPED_SLASH + "c" + ESCAPED_SLASH + "d");
    assertNull(parser.getResource(basePage.getContentResource()));

    // don't crash with null suffix
    parser = getParserWithIncomingSuffix(null);
    assertNull(parser.getResource(basePage.getContentResource()));


    // don't crash if a null resource is specified as base path - use the current page as base
    parser = getParserWithIncommingSuffix("b" + ESCAPED_SLASH + "c", basePage);
    assertNotNull(parser.getResource((Resource)null));
  }


  @Test
  void testGetLongIntBooleanStringResourceFilterOfResource() {
    // create a page and a resource within the page
    String resourceType = "theResourceType";
    Predicate<Resource> filter = new ResourceTypeFilter(resourceType);
    Page currentPage = context.create().page("/content/a", "template", "title");
    Resource targetResource = createResource("/content/a/jcr:content/b/c", resourceType);

    // get the resource by path (relative to current page) and resource type filter
    SuffixParser parser = getParserWithIncommingSuffix(ESCAPED_SLASH + "b" + ESCAPED_SLASH + "c", currentPage);
    Resource suffixResource = parser.getResource(filter);
    // check that the right target resource is found
    assertNotNull(suffixResource);
    assertEquals(targetResource.getPath(), suffixResource.getPath());


    // get the suffix resource with the wrong resource type
    suffixResource = parser.getResource(new ResourceTypeFilter("wrong resourcetype"));
    // check that no resource is found, despite the path being correct
    assertNull(suffixResource);


    // don't crash if a non-existing path specified in suffix
    parser = getParserWithIncommingSuffix(ESCAPED_SLASH + "c" + ESCAPED_SLASH + "d", currentPage);
    assertNull(parser.getResource(filter));


    // don't crash with null suffix
    parser = getParserWithIncommingSuffix(null, currentPage);
    assertNull(parser.getResource(filter));
  }

  @Test
  void testGetLongIntBooleanStringResourceFilterOfResourceString() {
    // create a page that and a resource that within the page
    final String resourceType = "theResourceType";
    Page basePage = context.create().page("/content/a", "template", "title");
    Resource targetResource = createResource("/content/a/jcr:content/b/c", resourceType);

    // filter that only includes resources named "c"
    Predicate<Resource> cFilter = new Predicate<Resource>() {
      @Override
      public boolean test(Resource pResource) {
        return pResource.getPath().endsWith("/c");
      }
    };

    // get the resource by path (relative to the page) using the "c" filter
    SuffixParser parser = getParserWithIncomingSuffix(ESCAPED_SLASH + "b" + ESCAPED_SLASH + "c");
    Resource suffixResource = parser.getResource(cFilter, basePage.getContentResource());
    // check that the /content/a/jcr:content/b/c is found
    assertNotNull(suffixResource);
    assertEquals(targetResource.getPath(), suffixResource.getPath());


    // get the "b" resource (relative to the page) using the "c" filter
    parser = getParserWithIncomingSuffix(ESCAPED_SLASH + "b");
    suffixResource = parser.getResource(cFilter, basePage.getContentResource());
    // that resource doesn't match the filter
    assertNull(suffixResource);


    // get a non existing resource (relative to the page) using the "c" filter
    parser = getParserWithIncomingSuffix(ESCAPED_SLASH + "1" + ESCAPED_SLASH + "c");
    suffixResource = parser.getResource(cFilter, basePage.getContentResource());
    // that resource would match the filter but doesn't exist
    assertNull(suffixResource);
  }

  @Test
  void testGetLongIntBooleanStringResources() {
    // create a page with 4 resources
    final String resourceType = "theResourceType";
    Page basePage = context.create().page("/content/a", "template", "title");
    String basePath = basePage.getContentResource().getPath();
    Resource resourceBC = createResource(basePath + "/b/c", resourceType);
    Resource resourceBD = createResource(basePath + "/b/d", resourceType);
    Resource resourceCC = createResource(basePath + "/c/c", resourceType);
    Resource resourceCD = createResource(basePath + "/c/d", resourceType);

    // filter that only includes resources named "c"
    Predicate<Resource> cFilter = new Predicate<Resource>() {
      @Override
      public boolean test(Resource pResource) {
        return pResource.getPath().endsWith("/c");
      }
    };

    // get these resources from suffix
    SuffixParser parser = getParserWithIncomingSuffix(ESCAPED_SLASH + "b" + ESCAPED_SLASH + "c"
        + SUFFIX_PART_DELIMITER + "b" + ESCAPED_SLASH + "d"
        + SUFFIX_PART_DELIMITER + "c" + ESCAPED_SLASH + "c"
        + SUFFIX_PART_DELIMITER + "c" + ESCAPED_SLASH + "d");
    List<Resource> suffixResources = parser.getResources(cFilter, basePage.getContentResource());
    // check that the two resources named c are found
    assertNotNull(suffixResources);
    assertEquals(2, suffixResources.size());
    assertEquals(resourceBC.getPath(), suffixResources.get(0).getPath());
    assertEquals(resourceCC.getPath(), suffixResources.get(1).getPath());


    // test that all four resources are found if no filter is used
    parser = getParserWithIncomingSuffix(ESCAPED_SLASH + "b" + ESCAPED_SLASH + "c"
        + SUFFIX_PART_DELIMITER + "b" + ESCAPED_SLASH + "d"
        + SUFFIX_PART_DELIMITER + "c" + ESCAPED_SLASH + "c"
        + SUFFIX_PART_DELIMITER + "c" + ESCAPED_SLASH + "d");
    List<Resource> allResources = parser.getResources(null, basePage.getContentResource());
    // check that all resources are found
    assertNotNull(allResources);
    assertEquals(4, allResources.size());
    assertEquals(resourceBC.getPath(), allResources.get(0).getPath());
    assertEquals(resourceBD.getPath(), allResources.get(1).getPath());
    assertEquals(resourceCC.getPath(), allResources.get(2).getPath());
    assertEquals(resourceCD.getPath(), allResources.get(3).getPath());


    // test with non-existent resources in suffix
    parser = getParserWithIncomingSuffix(ESCAPED_SLASH + "b" + ESCAPED_SLASH + "c"
        + SUFFIX_PART_DELIMITER + "e" + ESCAPED_SLASH + "c"
        + SUFFIX_PART_DELIMITER + "e" + ESCAPED_SLASH + "d");
    suffixResources = parser.getResources(cFilter, basePage.getContentResource());
    // check that an only the existing resource b/c is found
    assertNotNull(suffixResources);
    assertEquals(1, suffixResources.size());
    assertEquals(resourceBC.getPath(), suffixResources.get(0).getPath());
  }

  @Test
  void testTag() {
    String tagId = "test:tag1/tag11";
    String suffix = new SuffixBuilder().put("tag", tagId).build();
    context.requestPathInfo().setSuffix(suffix);
    assertEquals(tagId, new SuffixParser(context.request()).get("tag", String.class));
  }

  @Test
  void testPage() {
    Page currentPage = context.create().page("/content/a");
    Page targetPage = context.create().page("/content/a/b/c");

    SuffixParser parser = getParserWithIncommingSuffix("b" + ESCAPED_SLASH + "c", currentPage);
    Page suffixPage = parser.getPage();

    assertNotNull(suffixPage);
    assertEquals(targetPage.getPath(), suffixPage.getPath());
  }

  @Test
  void testPageNonExistingPath() {
    Page currentPage = context.create().page("/content/a");

    SuffixParser parser = getParserWithIncommingSuffix("x" + ESCAPED_SLASH + "y", currentPage);
    Page suffixPage = parser.getPage();

    assertNull(suffixPage);
  }

  @Test
  void testPageNullSuffix() {
    Page currentPage = context.create().page("/content/a");

    SuffixParser parser = getParserWithIncommingSuffix(null, currentPage);
    Page suffixPage = parser.getPage();

    assertNull(suffixPage);
  }

  @Test
  void testPageWithFilter() {
    Page currentPage = context.create().page("/content/a");
    Page targetPage = context.create().page("/content/a/b/c", "/apps/app1/templates/template1");

    SuffixParser parser = getParserWithIncommingSuffix("b" + ESCAPED_SLASH + "c", currentPage);
    Page suffixPage = parser.getPage(new PageTemplateFilter("/apps/app1/templates/template1"));

    assertNotNull(suffixPage);
    assertEquals(targetPage.getPath(), suffixPage.getPath());

    suffixPage = parser.getPage(new PageTemplateFilter("/other/path"));
    assertNull(suffixPage);
  }

  @Test
  void testPages() {
    Page currentPage = context.create().page("/content/a");
    Page targetPage1 = context.create().page("/content/a/b/c");
    Page targetPage2 = context.create().page("/content/a/d/1");
    Page targetPage3 = context.create().page("/content/a/d/2");

    SuffixParser parser = getParserWithIncommingSuffix("b" + ESCAPED_SLASH + "c"
        + SUFFIX_PART_DELIMITER + "d" + ESCAPED_SLASH + "1"
        + SUFFIX_PART_DELIMITER + "d" + ESCAPED_SLASH + "2", currentPage);
    List<Page> suffixPages = parser.getPages();

    assertEquals(3, suffixPages.size());
    assertEquals(targetPage1.getPath(), suffixPages.get(0).getPath());
    assertEquals(targetPage2.getPath(), suffixPages.get(1).getPath());
    assertEquals(targetPage3.getPath(), suffixPages.get(2).getPath());
  }

  @Test
  void testPagesWithInvalidPath() {
    Page currentPage = context.create().page("/content/a");
    Page targetPage1 = context.create().page("/content/a/b/c");
    Page targetPage3 = context.create().page("/content/a/d/2");

    SuffixParser parser = getParserWithIncommingSuffix("b" + ESCAPED_SLASH + "c"
        + SUFFIX_PART_DELIMITER + "d" + ESCAPED_SLASH + "1"
        + SUFFIX_PART_DELIMITER + "d" + ESCAPED_SLASH + "2", currentPage);
    List<Page> suffixPages = parser.getPages();

    assertEquals(2, suffixPages.size());
    assertEquals(targetPage1.getPath(), suffixPages.get(0).getPath());
    assertEquals(targetPage3.getPath(), suffixPages.get(1).getPath());
  }

  @Test
  void testPagesWithFiltering() {
    Page currentPage = context.create().page("/content/a", "/apps/app1/templates/template1");
    context.create().page("/content/a/b/c", "/apps/app1/templates/template2");
    Page targetPage2 = context.create().page("/content/a/d/1", "/apps/app1/templates/template1");
    context.create().page("/content/a/d/2", "/apps/app1/templates/template2");

    SuffixParser parser = getParserWithIncommingSuffix("b" + ESCAPED_SLASH + "c"
        + SUFFIX_PART_DELIMITER + "d" + ESCAPED_SLASH + "1"
        + SUFFIX_PART_DELIMITER + "d" + ESCAPED_SLASH + "2", currentPage);
    List<Page> suffixPages = parser.getPages(new PageTemplateFilter("/apps/app1/templates/template1"));

    assertEquals(1, suffixPages.size());
    assertEquals(targetPage2.getPath(), suffixPages.get(0).getPath());
  }

  @Test
  void testPagesWithNonPageResourcesMixed() {
    Page currentPage = context.create().page("/content/a");
    Page targetPage1 = context.create().page("/content/a/b/c");
    context.create().page("/content/a/d/1");
    context.create().resource("/content/a/d/1/jcr:content/resource1");
    context.create().page("/content/a/d/2");

    SuffixParser parser = getParserWithIncommingSuffix("b" + ESCAPED_SLASH + "c"
        + SUFFIX_PART_DELIMITER + "d" + ESCAPED_SLASH + "1" + ESCAPED_SLASH + "jcr:content" + ESCAPED_SLASH + "resource1"
        + SUFFIX_PART_DELIMITER + "d" + ESCAPED_SLASH + "2" + ESCAPED_SLASH + "jcr:content", currentPage);
    List<Page> suffixPages = parser.getPages();

    assertEquals(1, suffixPages.size());
    assertEquals(targetPage1.getPath(), suffixPages.get(0).getPath());
  }

}
