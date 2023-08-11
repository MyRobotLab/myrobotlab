package org.myrobotlab.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
// SINGLETON ???  similar to Runtime ???
// http://blog.palominolabs.com/2011/10/18/java-2-way-tlsssl-client-certificates-and-pkcs12-vs-jks-keystores/
// http://juliusdavies.ca/commons-ssl/ssl.html
// http://stackoverflow.com/questions/4319496/how-to-encrypt-and-decrypt-data-in-java

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.interfaces.AuthorizationProvider;
import org.myrobotlab.service.interfaces.KeyConsumer;
import org.slf4j.Logger;

// controlling export is "nice" but its control messages are the most important to mediate

public class Security extends Service<ServiceConfig> implements AuthorizationProvider {

  protected Set<String> serviceKeyNames = new HashSet<>();

  public static class Group {
    public HashMap<String, Boolean> accessRules = new HashMap<String, Boolean>();
    public boolean defaultAccess = true;
    // TODO - single access login
    // timestamp -
    public String groupId;
  }

  private class SavingTrustManager implements X509TrustManager {

    private X509Certificate[] chain;
    private final X509TrustManager tm;

    SavingTrustManager(X509TrustManager tm) {
      this.tm = tm;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
      throw new UnsupportedOperationException();
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
      this.chain = chain;
      tm.checkServerTrusted(chain, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {

      /**
       * This change has been done due to the following resolution advised for
       * Java 1.7+
       * http://infposs.blogspot.kr/2013/06/installcert-and-java-7.html
       **/

      return new X509Certificate[0];
      // throw new UnsupportedOperationException();
    }
  }

  public static class User {
    public String groupId; // support only 1 group now Yay !
    public String password; // encrypt
    // timestamp - single access login
    public String userId;
  }

  public static final String AES = "AES";

  transient private static final HashMap<String, Boolean> allowExportByName = new HashMap<String, Boolean>();

  transient private static final HashMap<String, Boolean> allowExportByType = new HashMap<String, Boolean>();

  // below is authorization
  transient private static final HashMap<String, Group> groups = new HashMap<String, Group>();

  private static final char[] HEXDIGITS = "0123456789abcdef".toCharArray();

  public final static Logger log = LoggerFactory.getLogger(Security.class);

  private static final long serialVersionUID = 1L;

  static private Properties store = new Properties();

  // users only map to groups - groups have the only access rules
  transient private static final HashMap<String, User> users = new HashMap<String, User>();

  /**
   * I think it might be easier concept to use a singleton for this service ...
   * Almost "always" better to have a singleton instance vs static methods !!!
   * 
   * @return the security service (singleton)
   * 
   */
  public static Security getInstance() {
    return (Security) Runtime.start("security", "Security");
  }

  public static void main(String[] args) throws Exception {
    // LoggingFactory.init(Level.INFO);

    Runtime.getInstance(args);

    Runtime.start("gui", "SwingGui");
    // Security security = Security.getInstance();
    // initializeStore("im a rockin rocker");
    Runtime.getSecurity().setKey("myKeyName", "XXDDLKERIOEJKLJ##$KJKJ#LJ@@");
    String key = Runtime.getSecurity().getKey("myKeyName");
    log.info("key is {}", key);

    Security security = Runtime.getSecurity();

    security.setKey("amazon.polly.user.key", "XXXXXXXXXXXXXX");
    security.setKey("amazon.polly.user.secret", "XXXXXXXXXXXXXXXXXXXXXXXXXXXX");
    security.setKey("xmpp.user", "user@gmail.com");
    security.setKey("xmpp.pwd", "xxxxxxxx");
    security.saveStore();
    security.getKey("amazon.polly.user.key");
    security.loadStore();
    log.info(security.getKey("xmpp.user")); // FIXME - report stor is has not
    // be loaded !!!
    log.info(security.getKey("amazon.polly.user.key"));
    log.info(security.getKey("amazon.polly.user.secret"));

    /*
     * String clearPwd = "mrlRocks!";
     * 
     * Properties p1 = new Properties();
     * 
     * p1.put("webgui.user", "supertick@gmail.com"); p1.put("webgui.pwd",
     * "zd7"); p1.put("xmpp.user", "supertick@gmail.com"); String encryptedPwd =
     * Security.encrypt(clearPwd, new File(KEY_FILE)); p1.put("xmpp.pwd",
     * encryptedPwd); p1.store(new FileWriter(PWD_FILE), "");
     * 
     * // ================== Properties p2 = new Properties();
     * 
     * p2.load(new FileReader(PWD_FILE)); encryptedPwd =
     * p2.getProperty("xmpp.pwd"); System.out.println(encryptedPwd);
     * System.out.println(Security.decrypt(encryptedPwd, new File(KEY_FILE)));
     */

  }

  transient private boolean defaultAllowExport = true;

  private String defaultNewGroupId = "anonymous";

  String keyFileName = null;

  String storeDirPathx = null;

  String storeFileName = "store";

  public Security(String n, String id) {
    super(n, id);
    keyFileName = String.format("%s%skey", FileIO.getCfgDir(), File.separator);
    storeFileName = String.format("%s%sstore", getDataDir(), File.separator);
    loadStore();
    createDefaultGroups();

    /*
     * FIXME - set predefined levels - high security medium low
     * allowExportByType.put("Xmpp", false);
     * 
     * allowExportByType.put("WebGui", false); allowExportByType.put("SwingGui",
     * false);
     * 
     * allowExportByType.put("Java", false); allowExportByType.put("Python",
     * false);
     * 
     * allowExportByType.put("Security", false);
     * allowExportByType.put("Runtime", false);
     */

    allowExportByType.put("Security", false);
    setSecurityProvider(this);
  }

  public boolean addGroup(String groupId) {
    return addGroup(groupId, false);
  }

  public boolean addGroup(String groupId, boolean defaultAccess) {
    Group g = new Group();
    g.groupId = groupId;
    g.defaultAccess = defaultAccess;

    if (groups.containsKey(groupId)) {
      warn("group %s already exists", groupId);
      return false;
    }

    info("added group %s", groupId);
    groups.put(groupId, g);
    return true;
  }

  @Deprecated // use setKey - name seems more appropriate
  public void addSecret(String name, String keyValue) {
    setKey(name, keyValue);
  }

  public boolean addUser(String user) {
    return addUser(user, null, null);
  }

  public boolean addUser(String userId, String password, String groupId) {

    if (users.containsKey(userId)) {
      log.warn("user {} already exists", userId);
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
    info("added user %s to group %s", userId, groupId);
    return true;
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

    String fullType = si.getClass().getSimpleName();
    if (allowExportByType.containsKey(fullType)) {
      return allowExportByType.get(fullType);
    }
    return defaultAllowExport;
  }

  public Boolean allowExportByName(String name, Boolean access) {
    return allowExportByName.put(name, access);
  }

  public Boolean allowExportByType(String type, Boolean access) {
    return allowExportByType.put(CodecUtils.type(type), access);
  }

  // default group permissions - for new user/group
  // anonymous
  // authenticated

  private String byteArrayToHexString(byte[] b) {
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

  /**
   * decrypt a value
   * 
   * @param message
   *          m
   * @param keyFile
   *          k
   * @return string
   * @throws GeneralSecurityException
   *           e
   * @throws IOException
   *           e
   */
  public String decrypt(String message, File keyFile) throws GeneralSecurityException, IOException {
    SecretKeySpec sks = getSecretKeySpec(keyFile);
    Cipher cipher = Cipher.getInstance(Security.AES);
    cipher.init(Cipher.DECRYPT_MODE, sks);
    byte[] decrypted = cipher.doFinal(hexStringToByteArray(message));
    return new String(decrypted);
  }

  /**
   * 
   * 
   * @param keyName
   *          remove a key from the keystore
   */
  public void deleteKey(String keyName) {
    if (store.containsKey(keyName)) {
      store.remove(keyName);
      saveStore();
      info("removed key %s", keyName);
      invoke("getKeyNames");
      return;
    }
    warn("could not remove key %s - does not exist", keyName);
  }

  /**
   * encrypt a value and generate a keyfile if the keyfile is not found then a
   * new one is created
   * 
   * @param passphrase
   *          p
   * @param keyFile
   *          k
   * @return string
   * @throws GeneralSecurityException
   *           e
   * @throws IOException
   *           e
   * 
   */
  public String encrypt(String passphrase, File keyFile) throws GeneralSecurityException, IOException {
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
   * get a key/secret from the secure store
   * 
   * @param name
   *          - the name of the security key
   * @return the property for a given key
   */
  public String getKey(String name) {
    if (store.containsKey(name)) {
      return store.getProperty(name);
    }

    log.error("could not find {} in security store", name);
    return null;
  }

  public String getKeyFileName() {
    return keyFileName;
  }

  /**
   * @return the set of key names currently stored in the key store
   * 
   * 
   */
  public Set<String> getKeyNames() {
    Set<String> ret = new TreeSet<String>();
    for (Object o : store.keySet()) {
      ret.add(o.toString());
    }
    return ret;
  }

  @Deprecated // use getKey
  public String getSecret(String name) {
    return getKey(name);
  }

  private SecretKeySpec getSecretKeySpec(File keyFile) throws NoSuchAlgorithmException, IOException {
    byte[] key = readKeyFile(keyFile);
    SecretKeySpec sks = new SecretKeySpec(key, Security.AES);
    return sks;
  }

  public String getStoreFileName() {
    return storeFileName;
  }

  private byte[] hexStringToByteArray(String s) {
    byte[] b = new byte[s.length() / 2];
    for (int i = 0; i < b.length; i++) {
      int index = i * 2;
      int v = Integer.parseInt(s.substring(index, index + 2), 16);
      b[i] = (byte) v;
    }
    return b;
  }

  // TODO - error if already exists !!
  // FIXME - errors if store has not been initalized
  public void initializeStore(String passphrase) {
    try {
      String keyfile = getKeyFileName();
      log.info("initializing key file {}", keyfile);
      encrypt(passphrase, new File(keyfile));
    } catch (Exception e) {
      log.error("initializeStore threw", e);
    }
  }

  public void installCert(String[] args) throws Exception {
    String host;
    int port;
    char[] passphrase;
    if ((args.length == 1) || (args.length == 2)) {
      String[] c = args[0].split(":");
      host = c[0];
      port = (c.length == 1) ? 443 : Integer.parseInt(c[1]);
      String p = (args.length == 1) ? "changeit" : args[1];
      passphrase = p.toCharArray();
    } else {
      System.out.println("Usage: java InstallCert <host>[:port] [passphrase]");
      return;
    }

    File file = new File("jssecacerts");
    File dir = null;
    if (file.isFile() == false) {
      char SEP = File.separatorChar;
      dir = new File(System.getProperty("java.home") + SEP + "lib" + SEP + "security");
      file = new File(dir, "jssecacerts");
      if (file.isFile() == false) {
        file = new File(dir, "cacerts");
      }
    }
    System.out.println("Loading KeyStore " + file + "...");
    InputStream in = new FileInputStream(file);
    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
    ks.load(in, passphrase);
    in.close();

    SSLContext context = SSLContext.getInstance("TLS");
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(ks);
    X509TrustManager defaultTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];
    SavingTrustManager tm = new SavingTrustManager(defaultTrustManager);
    context.init(null, new TrustManager[] { tm }, null);
    SSLSocketFactory factory = context.getSocketFactory();

    System.out.println("Opening connection to " + host + ":" + port + "...");
    SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
    socket.setSoTimeout(10000);
    try {
      System.out.println("Starting SSL handshake...");
      socket.startHandshake();
      socket.close();
      System.out.println();
      System.out.println("No errors, certificate is already trusted");
    } catch (SSLException e) {
      System.out.println();
      e.printStackTrace(System.out);
    }

    X509Certificate[] chain = tm.chain;
    if (chain == null) {
      System.out.println("Could not obtain server certificate chain");
      return;
    }

    System.out.println();
    System.out.println("Server sent " + chain.length + " certificate(s):");
    System.out.println();
    MessageDigest sha1 = MessageDigest.getInstance("SHA1");
    MessageDigest md5 = MessageDigest.getInstance("MD5");
    for (int i = 0; i < chain.length; i++) {
      X509Certificate cert = chain[i];
      System.out.println(" " + (i + 1) + " Subject " + cert.getSubjectDN());
      System.out.println("   Issuer  " + cert.getIssuerDN());
      sha1.update(cert.getEncoded());
      System.out.println("   sha1    " + toHexString(sha1.digest()));
      md5.update(cert.getEncoded());
      System.out.println("   md5     " + toHexString(md5.digest()));
      System.out.println();
    }

    /*
     * System.out.
     * println("Enter certificate to add to trusted keystore or 'q' to quit: [1]"
     * ); String line = reader.readLine().trim();
     */
    int k = 1;
    /*
     * try { k = (line.length() == 0) ? 0 : Integer.parseInt(line) - 1; } catch
     * (NumberFormatException e) { System.out.println("KeyStore not changed");
     * return; }
     */

    X509Certificate cert = chain[1];
    String alias = host + "-" + (k + 1);
    ks.setCertificateEntry(alias, cert);

    OutputStream out = new FileOutputStream("jssecacerts");
    ks.store(out, passphrase);
    out.close();
    // FileUtils.copyFileToDirectory(file, dir);
    System.out.println();
    System.out.println(cert);
    System.out.println();
    System.out.println("Added certificate to keystore 'jssecacerts' using alias '" + alias + "'");
  }

  @Override
  public boolean isAuthorized(Map<String, Object> annotation, String serviceName, String method) {

    /*
     * check not needed if (security == null) { // internal messaging return
     * defaultAccess; }
     */

    // TODO - super cache Radix Tree ??? super key -- uri
    // user:password@mrl://someService/someMethod - not found | ALLOWED ||
    // DENIED

    // user versus binary token
    if (annotation.containsKey("user")) // && password || token
    {
      String fromUser = (String) annotation.get("user");

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

  @Override
  public boolean isAuthorized(Message msg) {
    return isAuthorized(msg.getProperties(), msg.getName(), msg.method);
  }

  synchronized public void loadStore() {
    try {

      Properties fileStore = new Properties();
      log.info("security loading secure store file {}", getStoreFileName());
      String storeFileContents = FileIO.toString(getStoreFileName());
      if (storeFileContents != null) {
        String properties = decrypt(storeFileContents, new File(getKeyFileName()));
        ByteArrayInputStream bis = new ByteArrayInputStream(properties.getBytes());
        fileStore.load(bis);
        // memory has precedence over file
        fileStore.putAll(store);
        store = fileStore;
        // isLoaded = true;
      }
    } catch (FileNotFoundException e) {
      log.info("Security.loadStore file not found {}", getStoreFileName());
    } catch (Exception e2) {
      log.error("loadStore threw", e2);
    }
  }

  private byte[] readKeyFile(File keyFile) throws FileNotFoundException {
    Scanner scanner = new Scanner(keyFile);
    scanner.useDelimiter("\\Z");
    String keyValue = scanner.next();
    scanner.close();
    return hexStringToByteArray(keyValue);
  }

  public void saveStore() {
    try {
      /*
       * - loading in constructor now if (!isLoaded) { loadStore(); }
       */
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      store.store(out, null);
      String encrypted = encrypt(new String(out.toByteArray()), new File(getKeyFileName()));
      FileIO.toFile(getStoreFileName(), encrypted);
      log.info("aes secure file saved");
    } catch (Exception e) {
      log.error("saveStore threw", e);
    }
  }

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

  /**
   * Set a key with a keyname .. e.g. AWS_SECRET with a value e.g.
   * ERM23!933-df3j2l4kjfu Once a key is set its in an encrypted store and the
   * code which sets the key can be removed
   * 
   * @param keyName
   *          name
   * @param keyValue
   *          value
   * @return the name of the key stored
   * 
   */
  public String setKey(String keyName, String keyValue) {
    store.put(keyName, keyValue);
    saveStore();
    invoke("getKeyNames");
    info("added key %s", keyName);
    return keyName;
  }

  @Deprecated /* replace with StringUtil.bytesToHex */
  private String toHexString(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 3);
    for (int b : bytes) {
      b &= 0xff;
      sb.append(HEXDIGITS[b >> 4]);
      sb.append(HEXDIGITS[b & 15]);
      sb.append(' ');
    }
    return sb.toString();
  }

  public Set<String> getServiceKeyNames() {
    List<String> servicesNeedingKeys = Runtime.getServiceNamesFromInterface(KeyConsumer.class);
    for (String serviceName : servicesNeedingKeys) {
      KeyConsumer s = (KeyConsumer) Runtime.getService(serviceName);
      addServiceKeyNames(s.getKeyNames());
    }
    broadcastState();
    return serviceKeyNames;
  }

  // WTH? this never gets called
  public void onRegistered(String name) {
    log.info("onRegistered({})", name);
  }

  public void onStarted(String name) {
    ServiceInterface si = Runtime.getService(name);
    if (si instanceof KeyConsumer) {
      addServiceKeyNames(((KeyConsumer) si).getKeyNames());
    }
  }

  public void addServiceKeyNames(String[] keyNamesIn) {
    for (String keyName : keyNamesIn) {
      serviceKeyNames.add(keyName);
    }
  }

  @Override
  public ServiceConfig apply(ServiceConfig c) {
    super.apply(c);
    config = c;
    return config;
  }

  @Override
  public ServiceConfig getConfig() {
    return config;
  }

}
