package org.myrobotlab.jme3;

import com.jme3.math.ColorRGBA;

public class Jme3Utils {

	static public ColorRGBA getColor(String c) {
		ColorRGBA color = ColorRGBA.Gray;

		switch (c) {
		case "gray":
			color = ColorRGBA.Gray;
			break;
		case "black":
			color = ColorRGBA.Black;
			break;
		case "white":
			color = ColorRGBA.White;
			break;
		case "darkgray":
			color = ColorRGBA.DarkGray;
			break;
		case "lightgray":
			color = ColorRGBA.LightGray;
			break;
		case "red":
			color = ColorRGBA.Red;
			break;
		case "green":
			color = ColorRGBA.Green;
			break;
		case "blue":
			color = ColorRGBA.Blue;
			break;
		case "magenta":
			color = ColorRGBA.Magenta;
			break;
		case "cyan":
			color = ColorRGBA.Cyan;
			break;
		case "orange":
			color = ColorRGBA.Orange;
			break;
		case "yellow":
			color = ColorRGBA.Yellow;
			break;
		case "brown":
			color = ColorRGBA.Brown;
			break;
		case "pink":
			color = ColorRGBA.Yellow;
			break;
		default:
			color = ColorRGBA.Gray;
		}

		return color;
	}

}
