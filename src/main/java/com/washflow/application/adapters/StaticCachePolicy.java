package com.washflow.application.adapters;

import java.util.Map;
import java.util.Set;

/**
 * Cache rules for the PWA files served from the classpath.
 *
 * <p>The app shell (index.html, the service worker and the manifest) must never be answered with a
 * 304 for an outdated copy: Jetty's ETag is derived from the file's timestamp and size, the jar
 * entries can share a fixed timestamp, and these files often keep the same size between releases
 * (only fixed-length hashes change). A 304 on sw.js makes the browser keep the previous service
 * worker - and its precached index.html - forever. So the shell is served with {@code no-store} and
 * always downloaded in full. Hashed bundles under /assets never change content for a given name and
 * can be cached for a year.
 */
public final class StaticCachePolicy {

  public static final Map<String, String> APP_SHELL_HEADERS =
      Map.of("Cache-Control", "no-store, max-age=0");
  public static final Map<String, String> HASHED_ASSET_HEADERS =
      Map.of("Cache-Control", "public, max-age=31536000, immutable");

  private static final Set<String> APP_SHELL =
      Set.of("/", "/index.html", "/sw.js", "/registerSW.js", "/manifest.webmanifest");

  private StaticCachePolicy() {}

  public static boolean isAppShell(String path) {
    return APP_SHELL.contains(path);
  }

  public static boolean isHashedAsset(String path) {
    return path != null && path.startsWith("/assets/");
  }

  /** Icons, favicon and the workbox runtime: revalidated on each use (Jetty's default). */
  public static boolean isOtherStatic(String path) {
    return !isAppShell(path) && !isHashedAsset(path);
  }
}
