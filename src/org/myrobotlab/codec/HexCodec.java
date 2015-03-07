package org.myrobotlab.codec;

import java.util.ArrayList;


public class HexCodec implements Codec {

	String displayDelimiter =  " ";

	@Override
	final public String decode(int data) {
		return String.format("%02x%s", data & 0xff, displayDelimiter);
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
	public String decode(int[] msgs) throws CodecException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getKey() {
		return "hex";
	}

}
