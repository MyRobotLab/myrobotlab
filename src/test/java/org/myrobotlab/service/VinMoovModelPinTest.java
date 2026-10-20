package org.myrobotlab.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The VinMoov mesh is loaded by JME's BinaryImporter, which instantiates the
 * classes named in the file. These checks lock the download to one commit,
 * one length, and one SHA-256. No service startup and no network.
 */
public class VinMoovModelPinTest {

  @Test
  public void urlIsPinnedToCommit() {
    assertTrue(JMonkeyEngine.VINMOOV_MODEL_URL.contains(JMonkeyEngine.VINMOOV_MODEL_COMMIT));
    assertFalse(JMonkeyEngine.VINMOOV_MODEL_URL.contains("/master/"));
    assertEquals(64, JMonkeyEngine.VINMOOV_MODEL_SHA256.length());
    assertEquals(67_449_927L, JMonkeyEngine.VINMOOV_MODEL_BYTES);
  }

  @Test
  public void payloadMustMatchPinnedBytesAndHash() {
    assertTrue(JMonkeyEngine.vinMoovPayloadOk(JMonkeyEngine.VINMOOV_MODEL_BYTES, JMonkeyEngine.VINMOOV_MODEL_BYTES,
        JMonkeyEngine.VINMOOV_MODEL_SHA256));
    // A body past the old 1 MB floor is still a truncated transfer when
    // Content-Length is the full file.
    assertFalse(JMonkeyEngine.vinMoovPayloadOk(JMonkeyEngine.VINMOOV_MODEL_BYTES, 2_000_000L, JMonkeyEngine.VINMOOV_MODEL_SHA256));
    assertFalse(JMonkeyEngine.vinMoovPayloadOk(JMonkeyEngine.VINMOOV_MODEL_BYTES, JMonkeyEngine.VINMOOV_MODEL_BYTES,
        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));
    // Unknown Content-Length is accepted only when the bytes and hash match.
    assertTrue(JMonkeyEngine.vinMoovPayloadOk(-1L, JMonkeyEngine.VINMOOV_MODEL_BYTES, JMonkeyEngine.VINMOOV_MODEL_SHA256));
    assertFalse(JMonkeyEngine.vinMoovPayloadOk(-1L, JMonkeyEngine.VINMOOV_MODEL_BYTES, null));
  }
}
