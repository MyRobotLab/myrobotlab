package org.myrobotlab.codec;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Encoder;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Arduino2;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class ArduinoBindingsGenerator {

	public transient final static Logger log = LoggerFactory.getLogger(ArduinoBindingsGenerator.class);

	public static void generateDefinitions() throws IOException {

		HashMap<String,String> snr = new HashMap<String,String>();
		
		TreeMap<String, Method> sorted = new TreeMap<String, Method>();

		Arduino2 arduino = (Arduino2) Runtime.start("arduino", "Arduino2");
		Method[] m = arduino.getDeclaredMethods();
		for (int i = 0; i < m.length; ++i) {
			// String signature = Encoder.getMethodSignature(m[i]);
			Method method = m[i];
			StringBuilder msb = new StringBuilder(m[i].getName());
			Class<?>[] params = method.getParameterTypes();
			for (int j = 0; j < params.length; ++j) {
				msb.append(String.format(" %s", params[j].toString()));
			}

			// log.info(String.format("[%s]", msb.toString())); // hmmm
			// "class someclass" :(
			// get rid of overloads by taking the method with maximum
			// complexity(ish) :)
			if (sorted.containsKey(method.getName())) {
				// overloaded - who has more parameters
				Method complex = (sorted.get(method.getName()).getParameterTypes().length < method.getParameterTypes().length) ? method : sorted.get(method.getName());
				sorted.put(method.getName(), complex);
			} else {
				sorted.put(method.getName(), method);
			}
		}

		HashSet<String> except = new HashSet<String>();
		
		// filter out methods which do not get relayed
		// to the Arduino
		except.add("addCustomMsgListener");
		except.add("connect");
		except.add("disconnect");
		except.add("getDescription");
		except.add("createPinList");
		except.add("getBoardType");
		except.add("getCategories");
		except.add("getPeers");
		except.add("getPinList");
		except.add("getSerial");
		except.add("getStepperData");// WTF
		except.add("isConnected");
		except.add("main");
		except.add("onByte");
		except.add("onCustomMsg");
		except.add("releaseService");
		except.add("sendMsg");
		except.add("setBoardType");
		except.add("startService");
		except.add("test");

		// additionally force getversion and publishMRLCommError
		// so that mis-matches of version are quickly reported...
		except.add("getVersion");
		//except.add("E");
		
		// Arrays.sort(m);
		StringBuilder ino = new StringBuilder("///// INO GENERATED DEFINITION BEGIN //////\n");
		StringBuilder javaBindingsInit = new StringBuilder("///// java ByteToMethod generated definition - DO NOT MODIFY - Begin //////\n");

		int index = 2;
		
		for (String key : sorted.keySet()) {
			if (except.contains(key)){
				continue;
			}
			Method method = sorted.get(key);
			StringBuilder msb = new StringBuilder(method.getName());
			Class<?>[] params = method.getParameterTypes();
			for (int j = 0; j < params.length; ++j) {
				msb.append(String.format(" %s", params[j].toString()));
			}

			String methodSignatureComment = String.format("// {%s} \n", msb.toString());
			
			ino.append(methodSignatureComment);
			ino.append(String.format("#define %s\t\t%d\n\n", Encoder.toUnderScore(method.getName()), index));
			
			javaBindingsInit.append(String.format("\t%s", methodSignatureComment));
			javaBindingsInit.append(String.format("\tbyteToMethod.put(%d,\"%s\");\n", index, method.getName()));
			javaBindingsInit.append(String.format("\tmethodToByte.put(\"%s\",%d);\n\n", method.getName(), index));
			//javaByteToMethod.append(String.format("\tmethodToByte.put(%d,\"%s\");\n\n", index, method.getName()));			
			
			++index;
			// log.info(); // hmmm "class someclass" :(
		}
		
		ino.append("///// INO GENERATED DEFINITION END //////\n");
		javaBindingsInit.append("///// java ByteToMethod generated definition - DO NOT MODIFY - End //////\n");

		snr.put("<%=java.bindings.init%>", javaBindingsInit.toString());
		snr.put("<%=mrlcomm.defines%>", ino.toString());
		
		log.info(ino.toString());
		log.info(javaBindingsInit.toString());

		
		// file template filtering
		String ArduinoMsgCodec =  FileIO.fileToString("ArduinoMsgCodec.txt");
		String MRLComm = FileIO.fileToString("MRLComm.txt");
		
		for (String key : snr.keySet()){
			ArduinoMsgCodec = ArduinoMsgCodec.replaceAll(key, snr.get(key));
			MRLComm = MRLComm.replaceAll(key, snr.get(key));
		}
		
		
		FileIO.stringToFile("ArduinoMsgCodec.out.txt", ArduinoMsgCodec);
		FileIO.stringToFile("MRLComm.out.txt", MRLComm);
		
		//String ret = String.format("%s\n\n%s", ino.toString(), java.toString());
		//log.info(ret);
		// to define (upper with underscore)
		
		Runtime.exit();
	}

	public static void main(String[] args) {
		try {

			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel(Level.INFO);

			String t = "testMethod";
			// camelback to underscore
			/*
			 * String regex = "[A-Z\\d]"; String replacement = "$1_";
			 * log.info(t.replaceAll(regex, replacement)); log.info(t);
			 */

			log.info(Encoder.toUnderScore(t));
			log.info(Encoder.toCamelCase(Encoder.toUnderScore(t)));

			ArduinoBindingsGenerator.generateDefinitions();

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}
