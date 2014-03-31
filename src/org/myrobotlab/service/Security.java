package org.myrobotlab.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.AuthorizationProvider;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

// SINGLETON ???  similar to Runtime ???
// http://blog.palominolabs.com/2011/10/18/java-2-way-tlsssl-client-certificates-and-pkcs12-vs-jks-keystores/
// http://juliusdavies.ca/commons-ssl/ssl.html
// http://stackoverflow.com/questions/4319496/how-to-encrypt-and-decrypt-data-in-java

public class Security extends Service implements AuthorizationProvider {

	private static final long serialVersionUID = 1L;

	// TODO - concept (similar in Drupal) - anonymous, authenticated, admin ..
	// default groups ?
	transient private static final HashMap<String, Boolean> allowExportByName = new HashMap<String, Boolean>();
	transient private static final HashMap<String, Boolean> allowExportByType = new HashMap<String, Boolean>();

	public final static Logger log = LoggerFactory.getLogger(Security.class);

	// below is authorization
	transient private static final HashMap<String, Group> groups = new HashMap<String, Group>();
	// users only map to groups - groups have the only access rules
	transient private static final HashMap<String, User> users = new HashMap<String, User>();

	// many to 1 mapping - currently does not support many to many Yay !
	// transient private static final HashMap <String,String> userToGroup = new
	// HashMap <String,String>();

	// transient private boolean defaultAccess = true;

	static private Properties store = new Properties();
	static private String storeDirPath = String.format("%s%s.myrobotlab", System.getProperty("user.home"), File.separator);
	static private String keyFileName = "key";
	static private String storeFileName = "store";

	private static boolean isLoaded = false;

	transient private boolean defaultAllowExport = true;

	private String defaultNewGroupId = "anonymous";

	public static class User {
		// timestamp - single access login
		public String userId;
		public String password; // encrypt
		public String groupId; // support only 1 group now Yay !
	}

	public static class Group {
		// TODO - single access login
		// timestamp -
		public String groupId;
		public boolean defaultAccess = true;
		public HashMap<String, Boolean> accessRules = new HashMap<String, Boolean>();
	}

	public void createDefaultGroups() {
		Group g = new Group();
		g.groupId = "anonymous";
		g.defaultAccess = false;
		groups.put("anonymous", g);

		g = new Group();
		g.groupId = "authenticated";
		g.defaultAccess = true;
		groups.put("authenticated", g);
	}

	public Security(String n) {
		super(n);
		createDefaultGroups();

		allowExportByType.put("XMPP", false);
		allowExportByType.put("RemoteAdapter", false);
		allowExportByType.put("WebGUI", false);

		allowExportByType.put("Java", false);
		allowExportByType.put("Python", false);

		allowExportByType.put("Security", false);
		allowExportByType.put("Runtime", false);

		setSecurityProvider(this);
	}

	@Override
	public String getDescription() {
		return "security service";
	}

	// private HashMap<String, ByteArrayOutputStream> persistantStore = new
	// HashMap<String, ByteArrayOutputStream>();

	private HashMap<String, byte[]> keys = new HashMap<String, byte[]>();

	/*
	 * public boolean loadKeyStore(String location) {
	 * 
	 * }
	 */

	public boolean setDefaultNewGroupId(String userId, String groupId) {
		if (!users.containsKey(userId)) {
			error("user %s does not exist can not change groupId", userId);
			return false;
		}

		if (!groups.containsKey(groupId)) {
			error("group %s does not exist can not change groupId", groupId);
			return false;
		}

		users.get(userId).groupId = groupId;

		return false;

	}

	@Override
	public boolean allowExport(String serviceName) {

		if (allowExportByName.containsKey(serviceName)) {
			return allowExportByName.get(serviceName);
		}

		ServiceInterface si = Runtime.getService(serviceName);

		if (si == null) {
			error("%s could not be found for export", serviceName);
			return false;
		}

		String fullType = si.getClass().getCanonicalName();
		if (allowExportByType.containsKey(fullType)) {
			return allowExportByType.get(fullType);
		}
		return defaultAllowExport;
	}

	public Boolean allowExportByName(String name, Boolean access) {
		return allowExportByName.put(name, access);
	}

	public Boolean allowExportByType(String type, Boolean access) {
		return allowExportByType.put(Encoder.type(type), access);
	}

	@Override
	public boolean isAuthorized(Message msg) {
		return isAuthorized(msg.security, msg.name, msg.method);
	}

	@Override
	public boolean isAuthorized(HashMap<String, String> security, String serviceName, String method) {

		/*
		 * check not needed if (security == null) { // internal messaging return
		 * defaultAccess; }
		 */

		// TODO - super cache Radix Tree ??? super key -- uri
		// user:password@mrl://someService/someMethod - not found | ALLOWED ||
		// DENIED

		// user versus binary token
		if (security.containsKey("user")) // && password || token
		{
			String fromUser = security.get("user");

			// user scheme found - get the group
			if (!users.containsKey(fromUser)) {
				// invoke UserNotFound / throw
				return false;
			} else {

				User user = users.get(fromUser);
				// check MD5 hash of password
				// FIXME OPTIMIZE - GENERATE KEY user.group.accessRule - ALLOW ?
				// I'm looking for a specific object method - should have that
				// key
				if (!groups.containsKey(user.groupId)) // FIXME - optimize only
														// need a group look up
														// not a user l
				{
					// credentials supplied - no match
					// invoke Group for this user not found
					return false;
				} else {
					// credentials supplied - match - check access rules
					Group group = groups.get(user.groupId);
					// make message key
					// service level
					if (group.accessRules.containsKey(serviceName)) {
						return group.accessRules.get(serviceName);
					}

					// method level
					String methodLevel = String.format("%s.%s", serviceName, method);
					if (group.accessRules.containsKey(methodLevel)) {
						return group.accessRules.get(methodLevel);
					}

					return group.defaultAccess;
				}
			}

		} else {
			// invoke UnavailableSecurityScheme
			return false;
		}
	}

	// default group permissions - for new user/group
	// anonymous
	// authenticated

	public boolean setGroup(String userId, String groupId) {
		if (!users.containsKey(userId)) {
			error("user %s does not exist", userId);
			return false;
		}
		if (!groups.containsKey(groupId)) {
			error("group %s does not exist", groupId);
			return false;
		}

		User u = users.get(userId);
		u.groupId = groupId;
		return true;
	}

	public boolean addUser(String user) {
		return addUser(user, null, null);
	}

	public boolean addUser(String userId, String password, String groupId) {

		if (users.containsKey(userId)) {
			log.warn(String.format("user %s already exists", userId));
			return false;
		}
		User u = new User();
		u.userId = userId;
		u.password = password;
		if (groupId == null) {
			u.groupId = defaultNewGroupId;
		} else {
			u.groupId = groupId;
		}
		if (!groups.containsKey(u.groupId)) {
			error("could not add user %s groupId %s does not exist", userId, groupId);
			return false;
		}
		users.put(userId, u);
		return true;
	}

	public boolean addGroup(String groupId) {
		return addGroup(groupId, false);
	}

	public boolean addGroup(String groupId, boolean defaultAccess) {
		Group g = new Group();
		g.groupId = groupId;
		g.defaultAccess = defaultAccess;

		if (groups.containsKey(groupId)) {
			log.warn(String.format("group %s already exists", groupId));
			return false;
		}

		groups.put(groupId, g);
		return true;
	}

	/*
	 * public static void main(String[] args) {
	 * LoggingFactory.getInstance().configure();
	 * LoggingFactory.getInstance().setLevel(Level.INFO);
	 * 
	 * try {
	 * 
	 * //Serializable s = new SerializableImage(null, null); // passphrase - key
	 * // A better way to create a key is with a SecretKeyFactory using a salt:
	 * 
	 * String passphrase = "correct horse battery staple"; MessageDigest digest
	 * = MessageDigest.getInstance("SHA"); digest.update(passphrase.getBytes());
	 * SecretKeySpec key = new SecretKeySpec(digest.digest(), 0, 16, "AES");
	 * 
	 * Cipher aes = Cipher.getInstance("AES/ECB/PKCS5Padding");
	 * aes.init(Cipher.ENCRYPT_MODE, key); byte[] ciphertext =
	 * aes.doFinal("my cleartext".getBytes()); log.info(new String(ciphertext));
	 * 
	 * aes.init(Cipher.DECRYPT_MODE, key); String cleartext = new
	 * String(aes.doFinal(ciphertext));
	 * 
	 * log.info(cleartext);
	 * 
	 * } catch (Exception e) { Logging.logException(e); }
	 * 
	 * Security security = new Security("security"); security.startService();
	 * 
	 * Runtime.createAndStart("gui", "GUIService");
	 * 
	 * }
	 */

	public static final String AES = "AES";

	static public String getKeyFileName() {
		return String.format("%s%s%s", storeDirPath, File.separator, keyFileName);
	}

	// TODO - error if already exists !!
	// FIXME - errors if store has not been initalized
	public static void initializeStore(String passphrase) {
		try {
			String keyfile = getKeyFileName();
			log.info(String.format("initializing key file %s", keyfile));
			encrypt(passphrase, new File(keyfile));
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	public static void addSecret(String name, String secret) {
		store.put(name, secret);
		saveStore();
	}

	public static String getSecret(String name) {
		if (store.containsKey(name)) {
			return store.getProperty(name);
		}

		log.error(String.format("could not find %s in security store", name));
		return null;
	}

	static public void loadStore() {
		try {

			// FIXME - store not threadsafe
			Properties fileStore = new Properties();
			String storeFileContents = FileIO.fileToString(getStoreFileName());
			if (storeFileContents != null) {
				String properties = decrypt(storeFileContents, new File(getKeyFileName()));
				ByteArrayInputStream bis = new ByteArrayInputStream(properties.getBytes());
				fileStore.load(bis);
				// memory has precedence over file
				fileStore.putAll(store);
				store = fileStore;
				isLoaded = true;
			}

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	static public String getStoreFileName() {
		return String.format("%s%s%s", storeDirPath, File.separator, storeFileName);
	}

	static public void saveStore() {
		try {
			if (!isLoaded) {
				loadStore();
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			store.store(out, null);
			String encrypted = Security.encrypt(new String(out.toByteArray()), new File(getKeyFileName()));
			FileIO.stringToFile(getStoreFileName(), encrypted);
		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	/**
	 * encrypt a value and generate a keyfile if the keyfile is not found then a
	 * new one is created
	 * 
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */
	public static String encrypt(String passphrase, File keyFile) throws GeneralSecurityException, IOException {
		if (!keyFile.exists()) {

			new File(keyFile.getParent()).mkdirs();

			KeyGenerator keyGen = KeyGenerator.getInstance(Security.AES);
			keyGen.init(128);
			SecretKey sk = keyGen.generateKey();
			FileWriter fw = new FileWriter(keyFile);
			fw.write(byteArrayToHexString(sk.getEncoded()));
			fw.flush();
			fw.close();
		}

		SecretKeySpec sks = getSecretKeySpec(keyFile);
		Cipher cipher = Cipher.getInstance(Security.AES);
		cipher.init(Cipher.ENCRYPT_MODE, sks, cipher.getParameters());
		byte[] encrypted = cipher.doFinal(passphrase.getBytes());
		return byteArrayToHexString(encrypted);
	}

	/**
	 * decrypt a value
	 * 
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */
	public static String decrypt(String message, File keyFile) throws GeneralSecurityException, IOException {
		SecretKeySpec sks = getSecretKeySpec(keyFile);
		Cipher cipher = Cipher.getInstance(Security.AES);
		cipher.init(Cipher.DECRYPT_MODE, sks);
		byte[] decrypted = cipher.doFinal(hexStringToByteArray(message));
		return new String(decrypted);
	}

	private static SecretKeySpec getSecretKeySpec(File keyFile) throws NoSuchAlgorithmException, IOException {
		byte[] key = readKeyFile(keyFile);
		SecretKeySpec sks = new SecretKeySpec(key, Security.AES);
		return sks;
	}

	private static byte[] readKeyFile(File keyFile) throws FileNotFoundException {
		Scanner scanner = new Scanner(keyFile).useDelimiter("\\Z");
		String keyValue = scanner.next();
		scanner.close();
		return hexStringToByteArray(keyValue);
	}

	private static String byteArrayToHexString(byte[] b) {
		StringBuffer sb = new StringBuffer(b.length * 2);
		for (int i = 0; i < b.length; i++) {
			int v = b[i] & 0xff;
			if (v < 16) {
				sb.append('0');
			}
			sb.append(Integer.toHexString(v));
		}
		return sb.toString().toUpperCase();
	}

	private static byte[] hexStringToByteArray(String s) {
		byte[] b = new byte[s.length() / 2];
		for (int i = 0; i < b.length; i++) {
			int index = i * 2;
			int v = Integer.parseInt(s.substring(index, index + 2), 16);
			b[i] = (byte) v;
		}
		return b;
	}

	public static void main(String[] args) throws Exception {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		final String KEY_FILE = "c:/tempPass/howto.key";
		final String PWD_FILE = "c:/tempPass/howto.properties";

		// initializeStore("im a rockin rocker");
		loadStore();
		log.info(Security.getSecret("xmpp.user"));
		Security.addSecret("xmpp.user", "supertick@gmail.com");
		Security.addSecret("xmpp.pwd", "mrlRocks!");
		saveStore();

		String clearPwd = "mrlRocks!";

		Properties p1 = new Properties();

		p1.put("webgui.user", "supertick@gmail.com");
		p1.put("webgui.pwd", "zd7");
		p1.put("xmpp.user", "supertick@gmail.com");
		String encryptedPwd = Security.encrypt(clearPwd, new File(KEY_FILE));
		p1.put("xmpp.pwd", encryptedPwd);
		p1.store(new FileWriter(PWD_FILE), "");

		// ==================
		Properties p2 = new Properties();

		p2.load(new FileReader(PWD_FILE));
		encryptedPwd = p2.getProperty("xmpp.pwd");
		System.out.println(encryptedPwd);
		System.out.println(Security.decrypt(encryptedPwd, new File(KEY_FILE)));
	}

}
