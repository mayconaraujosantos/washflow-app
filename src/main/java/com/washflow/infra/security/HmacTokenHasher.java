package com.washflow.infra.security;

import com.washflow.data.protocols.TokenHasher;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** HMAC-SHA256 keyed with the server secret, so a leaked table alone cannot be brute-forced. */
public class HmacTokenHasher implements TokenHasher {

  private final byte[] key;

  public HmacTokenHasher(String secret) {
    this.key = secret.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public String hash(String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("HmacSHA256 unavailable", e);
    }
  }
}
