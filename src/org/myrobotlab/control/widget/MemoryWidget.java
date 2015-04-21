package org.myrobotlab.control.widget;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.memory.Node;
import org.myrobotlab.service.interfaces.MemoryDisplay;
import org.slf4j.Logger;

public class MemoryWidget {

	public final static Logger log = LoggerFactory.getLogger(MemoryWidget.class.getCanonicalName());

	private JTree tree;
	private DefaultTreeModel model;
	private JPanel display = new JPanel(new BorderLayout());

	MemoryDisplay memoryDisplay;
	private NodeGUI root;

	public static void main(String args[]) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		JFrame frame = new JFrame();
		Container container = frame.getContentPane();
		final MemoryWidget nodeTree = new MemoryWidget(null);

		JButton addButton = new JButton("add node");
		addButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				nodeTree.putNode();
			}
		});
		JButton removeButton = new JButton("remove selected node");
		removeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				nodeTree.removeSelectedNode();
			}
		});
		JButton addNodeButton = new JButton("addNode node");
		addNodeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				nodeTree.put("root", new Node("fore"));
			}
		});
		JPanel inputPanel = new JPanel();
		inputPanel.add(addButton);
		inputPanel.add(removeButton);
		inputPanel.add(addNodeButton);

		container.add(inputPanel, BorderLayout.NORTH);
		container.add(nodeTree.getDisplay(), BorderLayout.CENTER);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(400, 300);
		frame.setVisible(true);

		nodeTree.put("root", new Node("foreground"));
		nodeTree.put("root", new Node("background"));
		nodeTree.put("root.foreground", new Node("objects"));
		nodeTree.put("root.foreground.objects", new Node("known"));
		nodeTree.put("root.foreground.objects", new Node("unknown"));

	}

	public MemoryWidget(final MemoryDisplay memDisplay) {
		this.memoryDisplay = memDisplay;

		root = new NodeGUI(new Node("/"));
		model = new DefaultTreeModel(root);
		tree = new JTree(model);

		display.add(new JScrollPane(tree));
		display.setPreferredSize(new Dimension(400, 500));

		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent me) {
				doMouseClicked(me);
			}
		});

		// ------ display different types through user selection begin ---------
		tree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				// DefaultMutableTreeNode node = (DefaultMutableTreeNode)
				// tree.getLastSelectedPathComponent();
				// TODO - put all user data into set/get UserData ???
				// MAKE NOTE - gui nodes can be many to one relation ship (or
				// one to many???) to actual memory nodes
				NodeGUI nodeGUI = (NodeGUI) tree.getLastSelectedPathComponent();

				/* if nothing is selected */
				if (nodeGUI == null) {
					log.info("nothing is selected");
					return;
				}

				TreeNode[] path = nodeGUI.getPath();
				TreePath tp = new TreePath(path);

				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < path.length; ++i) {
					sb.append(((NodeGUI) path[i]).getName());
					if (i != 0 && i != path.length - 1) {
						sb.append("/");
					}
				}

				// iterate through all data & display it
				memoryDisplay.clear();

				memoryDisplay.displayStatus(new Status(String.format("node %s", sb.toString())));
				memoryDisplay.display(nodeGUI.myNode);

				/* TODO ??? retrieve the node that was selected */
				Object nodeInfo = nodeGUI.getUserObject();
				log.info("{}", nodeInfo);

			}
		});
		// ------ display different types through user selection end ---------
	}

	void doMouseClicked(MouseEvent me) {
		TreePath tp = tree.getPathForLocation(me.getX(), me.getY());

		if (tp != null)
			log.info(tp.toString());// jtf.setText(tp.toString());
		else
			log.info("");// jtf.setText("");
	}

	public NodeGUI get(String path) {

		if (path == null) {
			return root;
		}

		return (NodeGUI) root.get(path);// root.getNode(path);
	}

	public JPanel getDisplay() {
		return display;
	}

	public NodeGUI getRoot() {
		return root;
	}

	private NodeGUI getSelectedNode() {
		return (NodeGUI) tree.getLastSelectedPathComponent();
	}

	public NodeGUI put(String parentPath, Node node) {
		// FIXME FIXME FIXME ???? - use JTree's index or NodeGUI's ?
		NodeGUI parent = (NodeGUI) root.get(parentPath);
		if (parent == null) {
			log.error("could not add gui node {} to path {}", node.getName(), parentPath);
			return null;
		}

		NodeGUI child = (NodeGUI) parent.get(node.getName());
		if (child == null) {
			child = new NodeGUI(node);
			// you must insertNodeInto only if child does not previously
			// exist - as
			model.insertNodeInto(child, parent, parent.getChildCount());
			parent.put(child);
		} else {
			child.refresh(node);
		}

		// FIXXED - NO LONGER CREATE BOGUS NODE GUIService'S --- YAY ! - JUST
		// SIMPLY DISPLAY A NODE
		// IMPORANT TO BE TRANSPARENT - MEMORY IS !

		// FIXME - for a pre-existing NodeGUI - regen ?? all children ??? or
		// refresh ???
		// FIXME - only NodeGUI's are in the (or should be in) the HashMap -
		// this should be strongly typed !
		// ----- add non-recursive nodes - begin ------------------
		/*
		 * for (Map.Entry<String,?> nodeData : node.getNodes().entrySet()) {
		 * String key = nodeData.getKey(); Object object = nodeData.getValue();
		 * log.info("{}{}", key, object);
		 * 
		 * // display based on type for all non-recursive memory Class<?> clazz
		 * = object.getClass(); if (clazz != Node.class) { if (clazz ==
		 * OpenCVData.class) { OpenCVData data = (OpenCVData)object;
		 * //log.info("{}",data); // adding a "gui" node - to have a place where
		 * a user can highlight // to "see" the opencv image data in the video
		 * widget // addLeaf(newChild, o.getKey(), o.getValue().toString()); //
		 * TODO - compartmentalize the following to methods // TODO - type info
		 * should be different field - your now converting typ info through
		 * name..
		 * 
		 * // this adds complexity - in that viewing its preferrable to be able
		 * to // select on different types, while "real" memory is much more
		 * compact // String imageKey = String.format("%s.%s", childKey,
		 * "image");
		 * 
		 * NodeGUI images = (NodeGUI)child.get("images"); log.info("{}",images);
		 * if (images == null){ images = new NodeGUI(child, "images");
		 * model.insertNodeInto(images, child, child.getChildCount()); // FIXME
		 * - nodeMap - probably not necessary if you learn to use // the JTree
		 * system child.put(images); }
		 * 
		 * 
		 * for (Map.Entry<String,?> img : data.getImages().entrySet()) { NodeGUI
		 * imgDisplay = (NodeGUI)images.get(img.getKey());
		 * 
		 * if (imgDisplay == null) { imgDisplay = new NodeGUI(images,
		 * img.getKey()); model.insertNodeInto(imgDisplay, images,
		 * imgDisplay.getChildCount()); images.put(imgDisplay);
		 * 
		 * if (memoryDisplay != null && img != null) {
		 * memoryDisplay.displayFrame((SerializableImage)img.getValue()); } }
		 * 
		 * } }
		 * 
		 * }
		 * 
		 * }
		 */

		return child;
	}

	// ----------------- user interface end -----------------------------

	// ----------------- user interface begin -----------------------------
	// user --> model --> add
	private void putNode() {
		NodeGUI parent = getSelectedNode();
		if (parent == null) {
			JOptionPane.showMessageDialog(display, "Select an era.", "Error", JOptionPane.ERROR_MESSAGE);

			return;
		}
		String name = JOptionPane.showInputDialog(MemoryWidget.this, "Enter Name:");

		// FIXME send to Cortex event - wait for publish
		put("/", new Node(name));

	}

	private void removeSelectedNode() {
		NodeGUI selectedNode = getSelectedNode();
		if (selectedNode != null)
			model.removeNodeFromParent(selectedNode);
	}

}
