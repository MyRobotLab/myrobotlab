/**
 * 
 */
package org.myrobotlab.IntegratedMovement;

import java.util.ArrayList;
import java.util.List;

/**
 * @author calamity
 *
 */
public class Node<T> {
	transient private T data = null;
	transient private List<Node<T>> children = new ArrayList<>();
	transient private Node<T> parent = null;
	public Node(T data){
		this.data = data;
	}
	public Node<T> addchild(Node<T> child){
		child.setParent(this);
		this.children.add(child);
		return child;
	}
	
	public List<Node<T>> getChildren(){
		return children;
	}
	
	public T getData(){
		return data;
	}
	
	public void setData(T data){
		this.data = data;
	}
	
	public void setParent(Node<T> parent){
		this.parent = parent;
	}
	public Node<T> getParent(){
		return parent;
	}
	public Node<T> find(T toFind) {
		Node<T> retVal = null;
		for(Node<T> child : getChildren()){
			if (child.data == toFind){
				return child;
			}
			else{
				retVal = child.find(toFind);
				if (retVal!=null){
					return retVal;
				}
			}
		}
		return retVal;
	}
}
