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
package io.wcm.handler.url.suffix.impl;

import java.util.function.Predicate;

/**
 * Contains static methods that combine filters with logical operations
 */
public final class FilterOperators {

  private FilterOperators() {
    // utility methods only
  }

  /**
   * @return a filter that includes those elements that are included by *both* specified filters
   */
  public static <T> Predicate<T> and(final Predicate<T> filter1, final Predicate<T> filter2) {
    return element -> filter1.test(element) && filter2.test(element);
  }

  /**
   * @return a filter that includes those elements that are included by *one* of the specified filters
   */
  public static <T> Predicate<T> or(final Predicate<T> filter1, final Predicate<T> filter2) {
    return element -> filter1.test(element) || filter2.test(element);
  }

}
