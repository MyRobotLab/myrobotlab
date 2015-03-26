package org.myrobotlab.codec;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.interfaces.LoggingSink;
import org.slf4j.Logger;

/**
 * a class which decodes bytes as an output stream, puts the decoded strings on
 * a queue and relays the stream of bytes to another output stream if available
 * 
 * another thread can wait on the blocking decode method for new decoded
 * messages
 * 
 * @author GroG
 *
 */
public class BlockingDecoderOutputStream extends OutputStream {

	public final static Logger log = LoggerFactory.getLogger(BlockingDecoderOutputStream.class);

	// Integer timeout = null;
	Integer timeout = 1000;
	int maxQueue = 1024;
	BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
	Codec codec;
	LoggingSink sink;
	OutputStream out;
	String prefix;

	public BlockingDecoderOutputStream(String prefix, LoggingSink sink) {
		this.sink = sink;
		codec = new DecimalCodec(sink);
		this.prefix = prefix;
	}

	public String decode() {
		try {
			if (timeout != null) {
				return queue.poll(timeout, TimeUnit.MILLISECONDS);
			}
			return queue.take();
		} catch (Exception e) {
			// don't care
		}
		return null;
	}

	public Codec getCodec() {
		return codec;
	}

	public String getCodecExt() {
		if (codec == null) {
			return null;
		}
		return codec.getCodecExt();
	}

	public String getKey() {
		if (codec == null) {
			return null;
		} else {
			return codec.getKey();
		}
	}

	public int getMaxQueue() {
		return maxQueue;
	}

	public OutputStream getOut() {
		return out;
	}

	public BlockingQueue<String> getQueue() {
		return queue;
	}

	public Integer getTimeout() {
		return timeout;
	}

	public boolean isRecording() {
		return out != null;
	}

	public void setCodec(Codec codec) {
		this.codec = codec;
	}

	public void setCodec(String key) {
		try {
			codec = Codec.getDecoder(key, sink);
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public void setMaxQueue(int maxQueue) {
		this.maxQueue = maxQueue;
	}

	public void setOut(OutputStream out) {
		this.out = out;
	}

	public void setQueue(BlockingQueue<String> queue) {
		this.queue = queue;
	}

	public Integer setTimeout(Integer timeout) {
		this.timeout = timeout;
		return timeout;
	}

	@Override
	public void write(int b) throws IOException {
		if (codec != null) {
			String decoded = codec.decode(b);
			if (decoded != null) {
				if (maxQueue > queue.size()) {
					queue.add(decoded);
				} else {
					/*
					 * meh - what do I care ? if (sink != null) {
					 * sink.info("%s buffer overrun", sink.getName()); }
					 */
				}
				if (out != null) {
					out.write(decoded.getBytes());
				}
			}
		} else {
			if (out != null) {
				out.write(b);
			}
		}
	}

	public void record(String filename) throws FileNotFoundException {
		log.info(String.format("record RX %s", filename));

		if (isRecording()) {
			log.info("already recording");
			return;
		}

		if (filename == null) {
			filename = String.format("%s.%s.%d.data", prefix, sink.getName(), System.currentTimeMillis());
		}

		// FIXME - allow setting of output stream ...
		out = new FileOutputStream(filename);
	}

}
