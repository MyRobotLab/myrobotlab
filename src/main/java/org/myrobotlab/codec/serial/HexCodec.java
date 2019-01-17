package org.myrobotlab.codec.serial;

import org.myrobotlab.framework.interfaces.LoggingSink;

public class HexCodec extends Codec {

	String coloumnDelimiter = " ";
	String rowDelimiter = "\n";
	int byteCount = 0;
	int width = 16;

	public HexCodec(LoggingSink myService) {
		super(myService);
	}

	// FIXME - byte[] or int[] :P
	@Override
	final public String decodeImpl(int data) {
		++byteCount;
		return String.format("%02X%s%s", data & 0xff, coloumnDelimiter, ((byteCount % width == 0) ? rowDelimiter : ""));
	}

	@Override
	public String decode(int[] msgs) {
		return null;
	}

	@Override
	public int[] encode(String data) {
		return new int[0];
	}

	@Override
	public String getCodecExt() {
		return getKey();
	}

	@Override
	public String getKey() {
		return "hex";
	}

	public byte[] parse(byte[] data, String format) {
		// ArrayList<Byte> bytes = new ArrayList<Byte>();
		if ("hex".equals(format)) {
			// int charCount = 0;
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < data.length; ++i) {
				byte b = data[i];
				if (b == ' ') {
					continue;
				}

				sb.append((char) data[i]);
				++i;
				sb.append((char) data[i]);

				// Integer.parseInt(b.toString());

				sb.setLength(0);
			}
		}

		// return bytes.toArray(byte[]);
		return data;
	}

}
