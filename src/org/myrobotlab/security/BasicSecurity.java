package org.myrobotlab.security;

import java.util.HashMap;

public class BasicSecurity {

	static private HashMap<String, String> userPasswords = new HashMap<String, String>();

	static public boolean addUser(String username, String password) {
		userPasswords.put(username, password);
		return true;
	}

	static public String authenticate(String username, String password) {
		if (username == null || password == null) {
			return null;
		}

		if (userPasswords.containsKey(username) && password.equals(userPasswords.get(username))) {
			return "TOKEN";
		}

		return null;
	}

	static public boolean load() {
		return false;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	static public boolean save() {
		return false;
	}

}
