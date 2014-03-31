package org.myrobotlab.string;

public class Util {

	public static String removeChar(String s, char c) {
		StringBuffer r = new StringBuffer(s.length());
		r.setLength(s.length());
		int current = 0;
		for (int i = 0; i < s.length(); i++) {
			char cur = s.charAt(i);
			if (cur != c)
				r.setCharAt(current++, cur);
		}
		return r.toString();
	}

	public static String removeCharAt(String s, int pos) {
		StringBuffer buf = new StringBuffer(s.length() - 1);
		buf.append(s.substring(0, pos)).append(s.substring(pos + 1));
		return buf.toString();
	}

	public static String replaceCharAt(String s, int pos, char c) {
		StringBuffer buf = new StringBuffer(s);
		buf.setCharAt(pos, c);
		return buf.toString();
	}

	public static String StringToMethodName(String english) {
		StringBuffer methodName = new StringBuffer();
		boolean afterWhitespace = false;
		for (int i = 0; i <= english.length() - 1; i++) {
			Character temp = english.charAt(i);
			if (temp != ' ') {
				if (i == 0) {
					temp = Character.toLowerCase(temp);
				} else if (afterWhitespace) {
					temp = Character.toUpperCase(temp);
				}

				methodName.append(temp);
				afterWhitespace = false;
			} else {
				afterWhitespace = true;
			}
		}

		return methodName.toString();
	}

	public static void main(String[] args) throws ClassNotFoundException {

		String methodName = Util.StringToMethodName("hello all you freaks");
		methodName = Util.StringToMethodName("thisIsATest over here");
		methodName = Util.StringToMethodName("This would be a nifty method name");
		methodName = Util.StringToMethodName("I have whitespace");

		methodName.toString();
	}

}
