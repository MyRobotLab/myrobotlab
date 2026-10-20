package org.myrobotlab.jme3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Box;

public class DepthPickTest {

  @Test
  public void overlayAndSkipNames() {
    assertTrue(DepthPick.isDepthOverlayName("chest.depthRgbMesh"));
    assertTrue(DepthPick.isDepthOverlayName("chest.depthCloud"));
    assertFalse(DepthPick.isDepthOverlayName("i01.leftArm.hand"));
    assertFalse(DepthPick.isDepthOverlayName("i01.chest.depthCamera"));
    assertTrue(DepthPick.isSkipName("_marker.ikLeftHand"));
    assertTrue(DepthPick.isSkipName(DepthPick.LEFT_HAND_REACH));
    assertTrue(DepthPick.isSkipName("i01.chest.depthCamera.frustum"));
    assertTrue(DepthPick.isSkipName("_mrl.depthMeter"));
    assertTrue(DepthPick.isSkipName("wireframe grid"));
    assertFalse(DepthPick.isSkipName("chest.depthRgbMesh"));
  }

  @Test
  public void rayHitsRgbMeshWorldPoint() {
    Geometry mesh = triangle(DepthPick.DEPTH_RGB_MESH, 1.5f);
    DepthPick.mark(mesh);
    Node root = new Node("root");
    root.attachChild(mesh);
    root.updateGeometricState();

    DepthPick.Hit hit = pickAlongZ(root);
    assertNotNull(hit);
    assertTrue(hit.depthOverlay);
    assertEquals(DepthPick.DEPTH_RGB_MESH, hit.geometry.getName());
    assertEquals(1.5f, hit.world.z, 0.02f);
    assertEquals(0f, hit.world.x, 0.05f);
    assertEquals(0f, hit.world.y, 0.05f);
  }

  @Test
  public void culledMeshIsSkippedSoRobotIsPicked() {
    Geometry mesh = triangle(DepthPick.DEPTH_RGB_MESH, 1.2f);
    DepthPick.mark(mesh);
    mesh.setCullHint(CullHint.Always);
    Geometry robot = new Geometry("i01.leftArm.hand", new Box(0.05f, 0.05f, 0.05f));
    robot.setLocalTranslation(0f, 0f, 0.8f);
    Node root = new Node("root");
    root.attachChild(mesh);
    root.attachChild(robot);
    root.updateGeometricState();

    DepthPick.Hit hit = pickAlongZ(root);
    assertNotNull(hit);
    assertFalse(hit.depthOverlay);
    assertEquals("i01.leftArm.hand", hit.geometry.getName());
  }

  @Test
  public void markerIsSkippedInFavorOfMesh() {
    Geometry marker = new Geometry("_marker.ikLeftHand", new Box(0.04f, 0.04f, 0.04f));
    marker.setLocalTranslation(0f, 0f, 0.5f);
    Geometry mesh = triangle(DepthPick.DEPTH_CLOUD, 1.4f);
    DepthPick.mark(mesh);
    Node root = new Node("root");
    root.attachChild(marker);
    root.attachChild(mesh);
    root.updateGeometricState();

    DepthPick.Hit hit = pickAlongZ(root);
    assertNotNull(hit);
    assertTrue(hit.depthOverlay);
    assertEquals(DepthPick.DEPTH_CLOUD, hit.geometry.getName());
  }

  private static Geometry triangle(String name, float z) {
    Mesh mesh = new Mesh();
    mesh.setMode(Mesh.Mode.Triangles);
    float[] pos = { -0.2f, -0.2f, z, 0.2f, -0.2f, z, 0f, 0.2f, z };
    mesh.setBuffer(VertexBuffer.Type.Position, 3, pos);
    mesh.setBuffer(VertexBuffer.Type.Index, 3, new int[] { 0, 1, 2 });
    mesh.updateBound();
    mesh.createCollisionData();
    return new Geometry(name, mesh);
  }

  private static DepthPick.Hit pickAlongZ(Node root) {
    CollisionResults results = new CollisionResults();
    root.collideWith(new Ray(new Vector3f(0f, 0f, 0f), Vector3f.UNIT_Z), results);
    return DepthPick.firstPick(results);
  }
}
