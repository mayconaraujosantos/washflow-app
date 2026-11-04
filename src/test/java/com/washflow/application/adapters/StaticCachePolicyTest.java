package com.washflow.application.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StaticCachePolicyTest {

  @Test
  void theAppShellIsNeverCached() {
    for (String path : new String[] {"/", "/index.html", "/sw.js", "/manifest.webmanifest"}) {
      assertTrue(StaticCachePolicy.isAppShell(path), path);
      assertFalse(StaticCachePolicy.isOtherStatic(path), path);
    }
    assertEquals("no-store, max-age=0", StaticCachePolicy.APP_SHELL_HEADERS.get("Cache-Control"));
  }

  @Test
  void hashedBundlesAreImmutable() {
    assertTrue(StaticCachePolicy.isHashedAsset("/assets/index-CJA2D4zc.js"));
    assertFalse(StaticCachePolicy.isAppShell("/assets/index-CJA2D4zc.js"));
    assertTrue(StaticCachePolicy.HASHED_ASSET_HEADERS.get("Cache-Control").contains("immutable"));
  }

  @Test
  void everyOtherFileFallsInExactlyOnePass() {
    for (String path :
        new String[] {"/favicon.svg", "/icons/icon-192.png", "/workbox-9c191d2f.js"}) {
      assertTrue(StaticCachePolicy.isOtherStatic(path), path);
      assertFalse(StaticCachePolicy.isAppShell(path), path);
      assertFalse(StaticCachePolicy.isHashedAsset(path), path);
    }
  }
}
