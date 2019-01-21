package org.myrobotlab.jme3;

import java.util.ArrayList;
import java.util.List;

import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;

public class Search implements SceneGraphVisitor {

  List<Spatial> results = new ArrayList<Spatial>();
  String searchText;
  boolean exactMatch;
  boolean includeGeometries;

  public Search(String searchText, boolean exactMatch, boolean includeGeometries) {
    this.searchText = searchText;
    this.exactMatch = exactMatch;
    this.includeGeometries = includeGeometries;
  }

  @Override
  public void visit(Spatial spatial) {
    String name = spatial.getName();
    if (name == null) {
      return;
    }
    
    if (!exactMatch) {
      if (name.toLowerCase().contains(searchText.toLowerCase())){
        if (spatial instanceof Node) {
          results.add(spatial);
        } else if (spatial instanceof Geometry && includeGeometries) {
          results.add(spatial);
        }
      }      
    }
  }
  
  public List<Spatial> getResults() {
    return results;
  }

}
