package test;

import java.util.ArrayList;

//This statement means that class "Bookstore.java" is the root-element of our example
public class Bookstore {

	// XmLElementWrapper generates a wrapper element around XML representation
	// XmlElement sets the name of the entities
	private String name;
	private String location;
	private ArrayList<Book> bookList;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getBork() {
		return name;
	}

	public void setBork(String name) {
		this.name = name;
	}

	public void setZorks(ArrayList<Book> bookList) {
		this.bookList = bookList;
	}

	public ArrayList<Book> getZorks() {
		return bookList;
	}
}