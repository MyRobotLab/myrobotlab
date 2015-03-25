package org.myrobotlab.control.widget;

import java.util.HashMap;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.memory.Node;
import org.slf4j.Logger;

/**
 * 
 * GUIService representation of a Memory Node
 */

public class NodeGUI extends DefaultMutableTreeNode {
	public final static Logger log = LoggerFactory.getLogger(NodeGUI.class.getCanonicalName());

	private static final long serialVersionUID = 1L;
	private boolean areChildrenDefined = false;

	public boolean isLeaf = false;

	/**
	 * although a node contains the entire subtree stucture we will always treat
	 * it like an individual unit, because there is a high likely hood, it's
	 * children or grandchildren are stale
	 */
	Node myNode;
	private String name;
	private HashMap<String, NodeGUI> nodeMap = new HashMap<String, NodeGUI>();

	public NodeGUI(Node node) {
		this.name = node.getName();
		this.myNode = node;
	}

	/**
	 * this is a 'GUIService' node for subcomponent display of a "real" memory
	 * node it has the same "memory" node as its GUIService's parent
	 * 
	 * @param parent
	 * @param name
	 *            - display name of this node - often a sub-display component
	 */
	public NodeGUI(NodeGUI parent, String name) {
		this.name = name;
		this.myNode = parent.myNode;
	}

	private void defineChildNodes() {
		// You must set the flag before defining children if you
		// use "add" for the new children. Otherwise you get an infinite
		// recursive loop, since add results in a call to getChildCount.
		// However, you could use "insert" in such a case.
		areChildrenDefined = true;
		/*
		 * old -------------------- for (int i = 0; i <
		 * myNode.getNodes().size(); i++) { //add(new OutlineNode("new",
		 * numChildren)); Node child = add(new OutlineNode("new", numChildren));
		 * }
		 */

		/*
		 * wrong - its a count ! not a INSERTION !!! for
		 * (Map.Entry<String,Object> o : myNode.getNodes().entrySet()) {
		 * //Map.Entry<String,SerializableImage> pairs = o;
		 * //log.info(o.getKey()); //publish(o.getValue()); if
		 * (o.getValue().getClass() == Node.class) { // does this add to model
		 * ???? add(new NodeGUI(cortexGUI, model, (Node)o.getValue())); } }
		 */
	}

	/**
	 * the most important method get is the effective "search" method of memory.
	 * It has an XPath like syntax the "/" means "data of" a node, so when the
	 * path is /k1/k2/ - would mean get the hashmap of k2 /k1/k2 - means get the
	 * key value of k2 in the hashmap of k1
	 * 
	 * other examples /background /background/position/x /background/position/y
	 * /foreground /known/ball/red /known/ball/yellow /known/cup
	 * /unknown/object1 /positions/x/ <map> /positions/y/ <map> /positions/time/
	 * <map> /tracking
	 * 
	 * @param path
	 * @return
	 */
	public Object get(String path) {
		if (path == "") {
			return this;
		}

		if (path == "/") {
			return nodeMap;
		}

		int pos0 = path.indexOf('/');
		if (pos0 != -1) {
			int pos1 = path.indexOf('/', pos0 + 1);
			String subpath;
			String remaining;
			if (pos1 != -1) {
				subpath = path.substring(pos0 + 1, pos1);
				remaining = path.substring(pos0 + 1);
			} else {
				subpath = path.substring(pos0 + 1);
				remaining = subpath;
			}

			if (nodeMap.containsKey(subpath)) {
				Object o = nodeMap.get(subpath);
				Class<?> c = o.getClass();
				if (c == NodeGUI.class) {
					return ((NodeGUI) o).get(remaining);
				} else {
					return o;
				}
			}
		} else if (nodeMap.containsKey(path)) {
			return nodeMap.get(path);
		}

		if (path.equals(myNode.getName())) {
			return this;
		}

		return null;

	}

	@Override
	public int getChildCount() {
		if (!areChildrenDefined)
			defineChildNodes();
		return (super.getChildCount());
	}

	public String getName() {
		return name;
	}

	public HashMap<String, NodeGUI> getNodes() {
		return nodeMap;
	}

	@Override
	public boolean isLeaf() {
		return (false);
	}

	public NodeGUI put(NodeGUI newChild) {
		return nodeMap.put(newChild.getName(), newChild);
	}

	public void refresh(Node node) {
		this.myNode = node;
		// TODO - refresh gui data

	}

	@Override
	public String toString() {
		// return myNode.getName();
		return name;
	}

	public String toXPath() {
		/* HOW TO CLIMB UP !!!!! */
		TreeNode parent = getParent();

		if (parent == null)
			return (String.valueOf(myNode.getName()));
		else
			return (parent.toString() + "/" + myNode.getName());

	}

}