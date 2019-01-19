package org.myrobotlab.jme3;

import java.util.List;
import java.util.concurrent.Callable;

import com.jme3.scene.Geometry;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;

public class Search implements Callable<List<Spatial>>, SceneGraphVisitor {

  @Override
  public void visit(Spatial spatial) {
    if (spatial instanceof Geometry) {
      // geometry
  } else {
      // node
  }
  }

  @Override
  public List<Spatial> call() throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

}
