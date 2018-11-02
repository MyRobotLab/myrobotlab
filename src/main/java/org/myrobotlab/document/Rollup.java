package org.myrobotlab.document;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Rollup {

  Map<String, Aggregate> aggregates = new TreeMap<>();

  public Set<String> getTypes() {
    return aggregates.keySet();
  }

  public void put(Classification object) {
    Aggregate aggregate = null;
    String label = object.getLabel();
    if (!aggregates.containsKey(label)) {
      aggregate = new Aggregate();
    } else {
      aggregate = aggregates.get(label);
    }
    aggregate.add(object);
    aggregates.put(label, aggregate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (String label : aggregates.keySet()) {
      Aggregate agg = aggregates.get(label);
      sb.append(label);
      sb.append(" ");
      sb.append(agg.totalCount);
      sb.append(" ");
      sb.append(agg.getTimeSinceMs());
      sb.append("\n");
    }
    return sb.toString();
  }

  public boolean contains(String label) {
    return aggregates.containsKey(label);
  }

}
