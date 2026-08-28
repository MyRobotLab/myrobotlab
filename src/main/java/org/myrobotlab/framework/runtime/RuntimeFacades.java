package org.myrobotlab.framework.runtime;

/**
 * Catalog of existing collaborators that already act as facades around Runtime
 * concerns. Prefer extending these (or adding new types in this package) over
 * adding large new blocks to {@link org.myrobotlab.service.Runtime}.
 *
 * <ul>
 * <li>{@link org.myrobotlab.framework.repo.Repo} / IvyWrapper — dependency install</li>
 * <li>{@link org.myrobotlab.framework.Plan} — start/config plans</li>
 * <li>{@link org.myrobotlab.framework.MethodCache} — invoke resolution</li>
 * <li>{@link org.myrobotlab.framework.Registration} — registry records</li>
 * <li>{@link org.myrobotlab.codec.CodecUtils} — serialization</li>
 * </ul>
 *
 * See {@code doc/agent/hotspot-map.md}.
 */
public final class RuntimeFacades {

  private RuntimeFacades() {
  }
}
