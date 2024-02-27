package io.wcm.handler.url;

/**
 * Vanity mode to use when building URLs
 */
public enum VanityMode {
    /**
     * Never take vanity paths into account
     */
    NEVER,

    /**
     * Only use vanity paths when externalizing URLs
     */
    EXTERNALIZE,

    /**
     * Always use vanity paths
     */
    ALWAYS
}
