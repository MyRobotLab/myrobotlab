package org.myrobotlab.framework;

/*
 import javax.xml.bind.annotation.XmlAttribute;
 import javax.xml.bind.annotation.XmlElement;
 import javax.xml.bind.annotation.XmlRootElement;
 */

public class MessageFormatter {

	// TODO ???? default return type String? OutputStream?
	public final static String TYPE_XML = "TYPE_XML";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public String format(Message msg) {
		return format(msg, TYPE_XML);
	}

	// public String format

	public String format(Message msg, String type) {
		StringBuffer sb = new StringBuffer();

		sb.append("<Message>");

		sb.append("<msgID>").append(msg.msgID).append("</msgID>");
		sb.append("<msgID>").append(msg.msgID).append("</msgID>");
		sb.append("<msgID>").append(msg.msgID).append("</msgID>");
		sb.append("<msgID>").append(msg.msgID).append("</msgID>");
		sb.append("<msgID>").append(msg.msgID).append("</msgID>");
		sb.append("<msgID>").append(msg.msgID).append("</msgID>");

		sb.append("</Message>");

		return sb.toString();

	}

}
