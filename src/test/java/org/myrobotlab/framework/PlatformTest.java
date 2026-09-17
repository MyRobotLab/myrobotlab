package org.myrobotlab.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class PlatformTest {

  @Test
  public void testGetTreatsLiteralNullAsAbsent() {
    Map<String, String> manifest = new HashMap<>();
    manifest.put("GitCommitIdAbbrev", "null");
    manifest.put("GitBranch", "null");
    assertEquals("unknownCommit", Platform.get(manifest, "GitCommitIdAbbrev", "unknownCommit"));
    assertEquals("unknownBranch", Platform.get(manifest, "GitBranch", "unknownBranch"));
  }

  @Test
  public void testGetReturnsRealValues() {
    Map<String, String> manifest = new HashMap<>();
    manifest.put("GitCommitIdAbbrev", "abcdef1");
    assertEquals("abcdef1", Platform.get(manifest, "GitCommitIdAbbrev", "unknownCommit"));
  }

  @Test
  public void testToShortCommitHandlesShortAndNull() {
    assertNull(Platform.toShortCommit(null));
    assertNull(Platform.toShortCommit("null"));
    assertEquals("abcd", Platform.toShortCommit("abcd"));
    assertEquals("abcdef1", Platform.toShortCommit("abcdef1"));
    assertEquals("abcdef1", Platform.toShortCommit("abcdef12extra"));
  }
}
