package org.myrobotlab.codec;

import java.util.ArrayList;

import org.myrobotlab.service.interfaces.LoggingSink;

public class HexCodec extends Codec {

	String coloumnDelimiter = " ";
	String rowDelimiter = "\n";
	int byteCount = 0;
	int width = 16;
	
	public HexCodec(LoggingSink myService) {
		super(myService);
	}

	@Override
	final public String decodeImpl(int data) {
		return String.format("%02x%s%s", data & 0xff, coloumnDelimiter, ((byteCount%width == 0)?rowDelimiter:""));
	}

	@Override
	public String decode(int[] msgs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] encode(String data) {
		// TODO Auto-generated method stub
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
		ArrayList<Byte> bytes = new ArrayList<Byte>();
		if ("hex".equals(format)) {
			int charCount = 0;
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
