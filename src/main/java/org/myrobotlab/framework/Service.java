/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.framework;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.Timer;
import java.util.TreeMap;
import java.util.TreeSet;

import org.myrobotlab.cache.LRUMethodCache;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.codec.Recorder;
import org.myrobotlab.framework.interfaces.Attachable;
import org.myrobotlab.framework.interfaces.Invoker;
import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.net.CommunicationManager;
import org.myrobotlab.net.Heartbeat;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.AuthorizationProvider;
import org.myrobotlab.service.interfaces.CommunicationInterface;
import org.myrobotlab.service.interfaces.QueueReporter;
import org.slf4j.Logger;

/**
 * 
 * Service is the base of the MyRobotLab Service Oriented Architecture. All
 * meaningful Services derive from the Service class. There is a
 * _TemplateService.java in the org.myrobotlab.service package. This can be used
 * as a very fast template for creating new Services. Each Service begins with
 * two threads One is for the "OutBox" this delivers messages out of the
 * Service. The other is the "InBox" thread which processes all incoming
 * messages.
 * 
 */
public abstract class Service extends MessageService implements Runnable, Serializable, ServiceInterface, Invoker, QueueReporter {

  // FIXME upgrade to ScheduledExecutorService
  // http://howtodoinjava.com/2015/03/25/task-scheduling-with-executors-scheduledthreadpoolexecutor-example/
  
  /**
   * contains all the meta data about the service - pulled from the static
   * method getMetaData() each instance will call the method and populate the
   * data for an instance
   * 
   */
  ServiceType serviceType;

  /**
   * a radix-tree of data -"DNA" Description of Neighboring Automata ;) this is
   * a 'master build plan' for the service
   * 
   * TODO - when a service is created - a copy of this (RNA) is made and the
   * instance of the service creates and starts its peers according to its
   * definition
   * 
   * For mutations - the master build plan is changed - then a copy is made
   * 
   * Each Service instance contains its own (possibly mutated) version
   * 
   * Peer references should probably always be transient - as the
   * cross-reference of names from remotes will get the wrong name
   * 
   * You call this peer "Bob" .. but in Chicago there is more than one Bob - and
   * your "Bob" needs to be referenced as "Cincinnati Bob" - if your remote
   * instance is in Chicago and you just say "Bob" I will think your talking
   * about "Chicago Bob" :)
   */
  transient static public final TreeMap<String, ServiceReservation> dnaPool = new TreeMap<String, ServiceReservation>();

  private static final long serialVersionUID = 1L;

  transient public final static Logger log = LoggerFactory.getLogger(Service.class);

  /**
   * key into Runtime's hosts of ServiceEnvironments mrlscheme://[gateway
   * name]/scheme://key for gateway mrl://gateway/xmpp://incubator incubator if
   * host == null the service is local
   */
  private URI instanceId = null;

  private String name;

  private String simpleName; // used in gson encoding for getSimpleName()

  private String serviceClass;

  private boolean isRunning = false;

  transient protected Thread thisThread = null;

  transient protected Inbox inbox = null;

  transient Timer timer = null;

  /**
   * a more capable task handler
   */
  transient HashMap<String, Timer> tasks = new HashMap<String, Timer>();

  public final static String cfgDir = FileIO.getCfgDir();

  // no longer transient - getMethodMap is not really needed
  protected Set<String> methodSet;

  // :P - gson will default convert a HashSet into an Array :(
  // So we need to make it a HashMap in order for gson to convert to an object
  protected Map<String, String> interfaceSet;

  transient protected SimpleDateFormat tsFormatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");

  transient protected Calendar cal = Calendar.getInstance(new SimpleTimeZone(0, "GMT"));

  // recordings
  // static private boolean isRecording = false;
  static private Recorder recorder = null;

  transient public final String MESSAGE_RECORDING_FORMAT_XML = "MESSAGE_RECORDING_FORMAT_XML";

  transient public final String MESSAGE_RECORDING_FORMAT_BINARY = "MESSAGE_RECORDING_FORMAT_BINARY";

  // FIXME SecurityProvider
  protected AuthorizationProvider authProvider = null;

  private Status lastError = null;

  /**
   * variable for services to virtualize some of their dependencies
   */
  protected boolean isVirtual = false;

  /**
   * Recursively builds Peer type information - which is not instance specific.
   * Which means it will not prefix any of the branches with a instance name
   * @param myKey m
   * @param serviceClass class 
   * @return a map of string to service reservation
   * 
   */
  static public TreeMap<String, ServiceReservation> buildDna(String myKey, String serviceClass) {
    TreeMap<String, ServiceReservation> ret = new TreeMap<String, ServiceReservation>();
    buildDna(ret, myKey, serviceClass, null);
    log.info("{}", ret);
    return ret;
  }

  public Set<String> buildDnaKeys(String myKey, String serviceClass) {
    TreeMap<String, ServiceReservation> dna = buildDna(myKey, serviceClass);
    return dna.keySet();
  }

  public Set<String> buildDnaNames(String myKey, String serviceClass) {
    TreeMap<String, ServiceReservation> dna = buildDna(myKey, serviceClass);
    TreeSet<String> set = new TreeSet<String>();
    for (ServiceReservation sr : dna.values()) {
      set.add(sr.actualName);
    }
    return set;
  }

  /*
   * public static Set<String> getPeerNames (String myKey){ // goes to dnaPool
   * TreeSet<String> set = new TreeSet<String>(); return set; }
   * 
   * public static Set<String> getPeerKeys (String myKey){ // goes to template
   * TreeSet<String> set = new TreeSet<String>(); return set; }
   */

  public String getPeerName(String fullKey) {
    // String fullKey = String.format("%s.%s", getName(), peerKey);
    // below is correct - all 'reads' should be against the dnaPool (i think)
    if (dnaPool.containsKey(fullKey)) {
      // easy case - info already exists ...
      return dnaPool.get(fullKey).actualName;
    }
    // --------- begin - is this necessary or correct ? -------------
    // look at the build plan
    TreeMap<String, ServiceReservation> srs = buildDna(getName(), getClass().getCanonicalName());
    if (srs == null) {
      return null;
    }

    if (srs != null) {
      ServiceReservation sr = srs.get(fullKey);
      if (sr != null) {
        return sr.actualName;
      }
    }
    // --------- begin - is this necessary or correct ? -------------

    return null;
  }

  public static TreeMap<String, ServiceReservation> mergeDna(String myKey, String serviceClass) {
    TreeMap<String, ServiceReservation> rna = buildDna(myKey, serviceClass);
    mergeDna(dnaPool, rna);
    return dnaPool;
  }

  public static TreeMap<String, ServiceReservation> mergeDna(TreeMap<String, ServiceReservation> dna, TreeMap<String, ServiceReservation> rna) {
    for (String key : rna.keySet()) {
      if (!dna.containsKey(key)) {
        // easy - doesnt exist in dna add it
        dna.put(key, rna.get(key));
      } else {
        // replace any null parts
        ServiceReservation node = dna.get(key);
        ServiceReservation rnaNode = dna.get(key);
        node.actualName = (node.actualName != null) ? node.actualName : rnaNode.actualName;
        node.comment = (node.comment != null) ? node.comment : rnaNode.comment;
        node.fullTypeName = (node.fullTypeName != null) ? node.fullTypeName : rnaNode.fullTypeName;
      }
    }

    return dna;
  }

  /**
   * this method returns the current build strucutre for which name &amp; type is
   * specified
   * 
   * @param dna - a.k.a myDna which information will be added to
   * @param myKey
   *          - key (name) instance of the class currently under construction
   * @param serviceClass
   *          - type of class being constructed
   * @param comment
   *          - added comment
   * @return a map
   */
  static public TreeMap<String, ServiceReservation> buildDna(TreeMap<String, ServiceReservation> dna, String myKey, String serviceClass, String comment) {

    String fullClassName = CodecUtils.getServiceType(serviceClass);

    try {

      /// PUSH PEER KEYS IN - IF SOMETHING ALREADY EXISTS LEAVE IT

      //// ------- this is static data which will never change
      //// ----------------------
      // - the 'key' structure will never change - however the service
      //// reservations within
      // - the dna CAN change - so the order of operations
      // get the static keys
      // query on keys
      // if reservations exist then merge in data
      Class<?> theClass = Class.forName(fullClassName);

      // getPeers
      Method method = theClass.getMethod("getMetaData");
      ServiceType st = (ServiceType) method.invoke(null);
      Map<String, ServiceReservation> peers = st.getPeers();

      log.info("processing {}.getPeers({}) will process {} peers", serviceClass, myKey, peers.size());

      // Breadth first recursion
      // Two loops are necessary - because recursion should not start
      // until the entire level
      // of peers has been entered into the tree - this will build the
      // index level by level
      // versus depth first - necessary because the "upper" levels need to
      // process first
      // to influence the lower levels

      for (ServiceReservation templatePeer : peers.values()) {

        String peerKey = templatePeer.key;

        String fullKey = String.format("%s.%s", myKey, peerKey);
        ServiceReservation rna = dnaPool.get(fullKey);

        log.info("({}) - [{}]", fullKey, templatePeer.actualName);

        if (rna == null) {
          // there is no reservation for this in the dnaPool (no
          // mutant) :)
          // so as long as its not a root then we add our prefix to
          // actual name
          if (!templatePeer.isRoot) {
            templatePeer.actualName = String.format("%s.%s", myKey, templatePeer.actualName);
          }
          log.info("dna adding new key {} {} {} {}", fullKey, templatePeer.actualName, templatePeer.fullTypeName, comment);
          dna.put(fullKey, templatePeer);
        } else {
          log.info("dna collision - replacing null values !!! {}", fullKey);
          StringBuffer sb = new StringBuffer();
          if (rna.actualName == null) {
            sb.append(String.format(" updating actualName to %s ", templatePeer.actualName));
            rna.actualName = templatePeer.actualName;
          }

          if (rna.fullTypeName == null) {
            // FIXME check for dot ?
            sb.append(String.format("updating peerType to %s ", templatePeer.fullTypeName));
            rna.fullTypeName = templatePeer.fullTypeName;
          }

          if (rna.comment == null) {
            sb.append(String.format(" updating comment to %s ", comment));
            rna.comment = templatePeer.comment;
          }

          log.info(sb.toString());

          buildDna(dna, Peers.getPeerKey(myKey, templatePeer.key), templatePeer.fullTypeName, templatePeer.comment);
        }

      } // for each peer

    } catch (Exception e) {
      log.error("{} does not have a getMetaData ", fullClassName);
    }

    return dna;
  }

  static public String getDnaString() {
    StringBuffer sb = new StringBuffer();
    for (Map.Entry<String, ServiceReservation> entry : dnaPool.entrySet()) {
      String key = entry.getKey();
      ServiceReservation value = entry.getValue();
      sb.append(String.format("%s=%s", key, value.toString()));
    }
    return sb.toString();
  }

  /**
   * copyShallowFrom is used to help maintain state information with
   * @param target t
   * @param source s
   * @return o
   */
  public static Object copyShallowFrom(Object target, Object source) {
    if (target == source) { // data is myself - operating on local copy
      return target;
    }
    Set<Class<?>> ancestry = new HashSet<Class<?>>();
    Class<?> targetClass = source.getClass();
    
    ancestry.add(targetClass);
    
    // if we are a org.myrobotlab object climb up the ancestry to
    // copy all super-type fields ... 
    // GroG says: I wasn't comfortable copying of "Service" - because its never been tested before - so we copy all definitions from
    // other superclasses e.g. - org.myrobotlab.service.abstracts
    // it might be safe in the future to copy all the way up without stopping...
   while(targetClass.getCanonicalName().startsWith("org.myrobotlab") && !targetClass.getCanonicalName().startsWith("org.myrobotlab.framework")){
     ancestry.add(targetClass);
     targetClass = targetClass.getSuperclass();
   }
   
   for(Class<?> sourceClass : ancestry) {
    
    Field fields[] = sourceClass.getDeclaredFields();
    for (int j = 0, m = fields.length; j < m; j++) {
      try {
        Field f = fields[j];

        int modifiers = f.getModifiers();

        // if (Modifier.isPublic(mod)
        // !(Modifier.isPublic(f.getModifiers())
        // Hmmm JSON mappers do hacks to get by
        // IllegalAccessExceptions.... Hmmmmm

        // GROG - recent change from this
        // if ((!Modifier.isPublic(modifiers)
        // to this
        String fname = f.getName();
        /*
         * if (fname.equals("desktops") || fname.equals("useLocalResources") ){
         * log.info("here"); }
         */

        if (Modifier.isPrivate(modifiers) || fname.equals("log") || Modifier.isTransient(modifiers) || Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
          log.debug("skipping {}", f.getName());
          continue;
        } else {
          log.debug("copying {}", f.getName());
        }
        Type t = f.getType();

        // log.info(String.format("setting %s", f.getName()));
        /*
         * if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
         * continue; }
         */

        // GroG - this is new 1/26/2017 - needed to get webgui data to
        // load
        f.setAccessible(true);
        Field targetField = sourceClass.getDeclaredField(f.getName());
        targetField.setAccessible(true);

        if (t.equals(java.lang.Boolean.TYPE)) {
          targetField.setBoolean(target, f.getBoolean(source));
        } else if (t.equals(java.lang.Character.TYPE)) {
          targetField.setChar(target, f.getChar(source));
        } else if (t.equals(java.lang.Byte.TYPE)) {
          targetField.setByte(target, f.getByte(source));
        } else if (t.equals(java.lang.Short.TYPE)) {
          targetField.setShort(target, f.getShort(source));
        } else if (t.equals(java.lang.Integer.TYPE)) {
          targetField.setInt(target, f.getInt(source));
        } else if (t.equals(java.lang.Long.TYPE)) {
          targetField.setLong(target, f.getLong(source));
        } else if (t.equals(java.lang.Float.TYPE)) {
          targetField.setFloat(target, f.getFloat(source));
        } else if (t.equals(java.lang.Double.TYPE)) {
          targetField.setDouble(target, f.getDouble(source));
        } else {
          // log.debug(String.format("setting reference to remote
          // object %s", f.getName()));
          targetField.set(target, f.get(source));
        }
      } catch (Exception e) {
        log.error("copy failed", e);
      }
    } // for each field in class
   } // for each in ancestry
    return target;
  }

  /**
   * Create the reserved peer service if it has not already been created
   * 
   * @param key
   *          unique identification of the peer service used by the composite
   * @return true if successfully created
   */
  static public ServiceInterface createRootReserved(String key) {
    log.info("createReserved {}", key);
    ServiceReservation node = dnaPool.get(key);
    if (node != null) {
      ServiceReservation r = dnaPool.get(key);
      return Runtime.create(r.actualName, r.fullTypeName);
    }

    log.error("createRootReserved can not create %s", key);
    return null;
  }

  public static String getCfgDir() {
    return cfgDir;
  }

  static public TreeMap<String, ServiceReservation> getDna() {
    return dnaPool;
  }

  public static String getHostName(final String inHost) {
    if (inHost != null)
      return inHost;

    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      log.error("could not find host, host is null or empty !");
    }

    return "localhost"; // no network - still can't be null // chumby
  }

  public static String getMethodToolTip(String className, String methodName, Class<?>[] params) {
    Class<?> c;
    Method m;
    ToolTip tip = null;
    try {
      c = Class.forName(className);

      m = c.getMethod(methodName, params);

      tip = m.getAnnotation(ToolTip.class);
    } catch (Exception e) {
      log.error("getMethodToolTip failed", e);
    }

    if (tip == null) {
      return null;
    }
    return tip.value();
  }

  static public void logTimeEnable(Boolean b) {
    Logging.logTimeEnable(b);
  }

  /**
   * This method will merge in the requested peer dna into the final global dna
   * - from which it will be accessible for create methods
   * 
   * template merge with existing dna
   * @param myKey the key
   * @param className the class name
   */
  public void mergePeerDna(String myKey, String className) {
    if (serviceType != null) {
      // serviceType starts as static type information from getMetaData
      // here we have to replace instance differences
      Map<String, ServiceReservation> peers = serviceType.getPeers();
      for (Entry<String, ServiceReservation> entry : peers.entrySet()) {
        String templateKey = entry.getKey();
        ServiceReservation template = entry.getValue();
        // build full key with our instance key + the peer template
        // defined in getMetaData

        String fullKey = String.format("%s.%s", myKey, templateKey);

        // test dna - if something already exists then LEAVE IT !!!
        // if it does not exist then inject it
        // do we prefix the actual name !?!?!?!?!?
        ServiceReservation sr = null;
        if (!dnaPool.containsKey(fullKey)) {
          // full key does not exist - so we put this reservation in
          // for further definition
          // since there was no previous definition of this service -
          // we will modify
          // the actual name so it is correct with the fullKey (prefix
          // of the context)

          // this is a template being merged in
          // if actualName == key then there is no re-mapping and both
          // get prefixed !
          // if actualName != key then there is a re-map

          // create new service reservation with fullkey to put into
          // dna9

          if (template.key.equals(template.actualName) && !template.isRoot) {
            sr = new ServiceReservation(fullKey, template.fullTypeName, template.comment);
          } else {
            // COLLISION WITH CUSTOM KEY - WE ARE MOVING DNA !!!
            String actualName = null;
            if (template.isRoot) {
              // moving to root
              actualName = template.actualName;
            } else {
              // We Prefix it if its not a root !
              actualName = String.format("%s.%s", myKey, template.actualName);
            }

            sr = new ServiceReservation(fullKey, actualName, template.fullTypeName, template.comment, template.isRoot);

            // we have to recursively move things if we moved a root
            // of some complex peer (the root and all its branches)
            movePeerDna(fullKey, actualName, template.fullTypeName, sr.comment);
          }

          dnaPool.put(fullKey, sr);
        } else {
          log.info("found reservation name [{}] is replaced with {}", fullKey, entry.getValue());
          sr = dnaPool.get(fullKey);
          if (sr.fullTypeName == null) {
            log.info("no type name in reservation, replacing with standard type - {}", template.fullTypeName);
            sr.fullTypeName = template.fullTypeName;
          }
        }

        // for each peer put in the processed peer
        // serviceType.peers.put(templateKey, sr);
        // sumthin's not right

      } // for each peer
    } // else no class meta - no peers
      // buildDNA(myKey, className, "merged dna");
    log.debug("merged dna \n{}", dnaPool);
  }

  /**
   * a method to recursively move all peer children of this server
   * @param myKey key
   * @param actualName name 
   * @param fullTypeName  full 
   * @param comment a comment
   */
  public void movePeerDna(String myKey, String actualName, String fullTypeName, String comment) {
    ServiceType meta = getMetaData(fullTypeName);
    if (meta != null) {
      Map<String, ServiceReservation> peers = meta.getPeers();

      for (Entry<String, ServiceReservation> reservation : peers.entrySet()) {
        String templateKey = reservation.getKey();
        // build full key with our instance key + the peer template
        // defined in getMetaData
        String fullKey = String.format("%s.%s", myKey, templateKey);
        String movedActual = String.format("%s.%s", actualName, templateKey);
        ServiceReservation templateSr = reservation.getValue();
        ServiceReservation sr = new ServiceReservation(movedActual, movedActual, templateSr.fullTypeName, templateSr.comment);
        dnaPool.put(movedActual, sr);
        // recurse to process children
        movePeerDna(fullKey, movedActual, templateSr.fullTypeName, templateSr.comment);
      }

    }
  }

  /**
   * Reserves a name for a root level Service. allows modifications to the
   * reservation map at the highest level
   * @param key the key
   * @param simpleTypeName the type 
   * @param comment a comment
   */
  static public void reserveRoot(String key, String simpleTypeName, String comment) {
    // strip delimeter out if put in by key
    // String actualName = key.replace(".", "");
    reserveRoot(key, key, simpleTypeName, comment);
  }

  static public void reserveRoot(String key, String actualName, String simpleTypeName, String comment) {
    log.info("reserved key {} -> {} {} {}", key, actualName, simpleTypeName, comment);
    dnaPool.put(key, new ServiceReservation(key, actualName, simpleTypeName, comment));
  }

  /**
   * basic useful reset of a peer before service is created
   * @param peerName name
   * @param peerType type
   */
  public void setPeer(String peerName, String peerType) {
    String fullKey = String.format("%s.%s", getName(), peerName);
    ServiceReservation sr = new ServiceReservation(fullKey, peerName, peerType, null);
    dnaPool.put(fullKey, sr);
  }

  /**
   * This method re-binds the key to another name. An example of where this
   * would be used is within Tracking there is an Servo service named "x",
   * however it may be desired to bind this to an already existing service named
   * "pan" in a pan/tilt system
   * 
   * @param key
   *          key internal name
   * @param newName
   *          new name of bound peer service
   * @return true if re-binding took place
   */
  static public boolean reserveRootAs(String key, String newName) {

    ServiceReservation genome = dnaPool.get(key);
    if (genome == null) {
      // FIXME - this is a BAD KEY !!! into the ServiceReservation (I
      // think :P) - another
      // reason to get rid of it !!
      dnaPool.put(key, new ServiceReservation(key, newName, null, null));
    } else {
      genome.actualName = newName;
    }
    return true;
  }

  public boolean setSecurityProvider(AuthorizationProvider provider) {
    if (authProvider != null) {
      log.error("security provider is already set - it can not be unset .. THAT IS THE LAW !!!");
      return false;
    }

    authProvider = provider;
    return true;
  }

  /**
   * sleep without the throw
   * @param millis the time in milliseconds
   * 
   */
  public static void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
    }
  }

  public final static String stackToString(final Throwable e) {
    StringWriter sw;
    try {
      sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
    } catch (Exception e2) {
      return "bad stackToString";
    }
    return "------\r\n" + sw.toString() + "------\r\n";
  }

  // FIXME - make a static initialization part !!!

  public Service(String reservedKey) {
    super(reservedKey);

    serviceClass = this.getClass().getCanonicalName();
    simpleName = this.getClass().getSimpleName();

    // xxx
    try {// FIXME !!! AFTER MERGE !!!
      serviceType = getMetaData(this.getClass().getCanonicalName());
    } catch (Exception e) {
      Logging.logError(e);
    }

    // FIXME - this is 'sort-of' static :P
    if (methodSet == null) {
      methodSet = getMessageSet();
    }

    if (interfaceSet == null) {
      interfaceSet = getInterfaceSet();
    }

    // a "safety" if Service was created by new Service(name)
    // we still want the local Runtime running
    if (!Runtime.isRuntime(this)) {
      Runtime.getInstance();
    }

    // merge all our peer keys into the dna
    // so that reservations are set with actual names if
    // necessary
    mergePeerDna(reservedKey, serviceClass);
    // xxx

    // see if incoming key is my "actual" name
    ServiceReservation sr = dnaPool.get(reservedKey);
    if (sr != null) {
      log.info("found reservation exchanging reservedKey {} for actual name {}", reservedKey, sr.actualName);
      name = sr.actualName;
    } else {
      name = reservedKey;
    }
    // keep MessageService name in sync

    // this.timer = new Timer(String.format("%s_timer", name)); FIXME -
    // re-implement but only create if there is a task!!
    this.inbox = new Inbox(name);
    this.outbox = new Outbox(this);
    cm = new CommunicationManager(name);
    this.outbox.setCommunicationManager(cm);

    tsFormatter.setCalendar(cal);
    // load(); removed by GroG
    Runtime.register(this, null);
  }

  public void addListener(MRLListener listener) {
    addListener(listener.topicMethod, listener.callbackName, listener.callbackMethod);
  }

  /**
   * adds a MRL message listener to this service this is the result of a
   * "subscribe" from a different service FIXME !! - implement with HashMap or
   * HashSet .. WHY ArrayList ???
   * 
   * @param topicMethod
   *          - method when called, it's return will be sent to the
   *          callbackName/calbackMethod
   * @param callbackName
   *          - name of the service to send return message to
   * @param callbackMethod
   *          - name of the method to send return data to
   */
  public void addListener(String topicMethod, String callbackName, String callbackMethod) {
    MRLListener listener = new MRLListener(topicMethod, callbackName, callbackMethod);
    if (outbox.notifyList.containsKey(listener.topicMethod.toString())) {
      // iterate through all looking for duplicate
      boolean found = false;
      ArrayList<MRLListener> nes = outbox.notifyList.get(listener.topicMethod.toString());
      for (int i = 0; i < nes.size(); ++i) {
        MRLListener entry = nes.get(i);
        if (entry.equals(listener)) {
          log.debug("attempting to add duplicate MRLListener {}", listener);
          found = true;
          break;
        }
      }
      if (!found) {
        log.debug("adding addListener from {}.{} to {}.{}", this.getName(), listener.topicMethod, listener.callbackName, listener.callbackMethod);
        nes.add(listener);
      }
    } else {
      ArrayList<MRLListener> notifyList = new ArrayList<MRLListener>();
      notifyList.add(listener);
      log.debug("adding addListener from {}.{} to {}.{}", this.getName(), listener.topicMethod, listener.callbackName, listener.callbackMethod);
      outbox.notifyList.put(listener.topicMethod.toString(), notifyList);
    }
  }

  public void addTask(int intervalMs, String method) {
    addTask(intervalMs, method, new Object[] {});
  }

  public void addTask(int intervalMs, String method, Object... params) {
    addTask(method, intervalMs, 0, method, params);
  }

  public void addTaskOneShot(int delay, String method, Object... params) {
    addTask(method, 0, delay, method, params);
  }

  /**
   * a stronger bigger better task handler !
   * @param taskName task name
   * @param intervalMs how frequent in milliseconds
   * @param delay the delay 
   * @param method the method
   * @param params the params to pass
   */
  public void addTask(String taskName, int intervalMs, int delay, String method, Object... params) {
    if (tasks.containsKey(taskName)) {
      log.warn("already have active task \"{}\"", taskName);
      return;
    }
    Timer timer = new Timer(String.format("%s.timer", String.format("%s.%s", getName(), taskName)));
    Message msg = Message.createMessage(this, getName(), method, params);
    Task task = new Task(this, taskName, intervalMs, msg);
    timer.schedule(task, delay);
    tasks.put(taskName, timer);
  }

  public HashMap<String, Timer> getTasks() {
    return tasks;
  }

  public boolean containsTask(String taskName) {
    return tasks.containsKey(taskName);
  }

  public void purgeTask(String taskName) {
    if (tasks.containsKey(taskName)) {
      log.info("remove task {}", taskName);
      Timer timer = tasks.get(taskName);
      if (timer != null) {
        try {
          timer.cancel();
          timer.purge();
          timer = null;
          tasks.remove(taskName);
        } catch (Exception e) {
          log.info(e.getMessage());
        }
      }
    } else {
      log.warn("purgeTask - task {} does not exist", taskName);
    }
  }

  public void purgeTasks() {
    for (String taskName : tasks.keySet()) {
      Timer timer = tasks.get(taskName);
      if (timer != null) {
        try {
          timer.cancel();
          timer.purge();
          timer = null;
          tasks.remove(taskName);
        } catch (Exception e) {
          log.info(e.getMessage());
        }
      }
    }
    tasks.clear();
  }

  @Override
  public void broadcastState() {
    invoke("publishState");
  }

  @Override
  public void broadcastStatus(Status status) {
    invoke("publishStatus", status);
  }

  @Override
  public String clearLastError() {
    String le = lastError.toString();
    lastError = null;
    return le;
  }

  public void close(Writer w) {
    if (w == null) {
      return;
    }
    try {
      w.flush();
    } catch (Exception e) {
      Logging.logError(e);
    } finally {
      try {
        w.close();
      } catch (Exception e) {
        // don't really care
      }
    }
  }

  /**
   * method for getting actual name from a service of its peer based on a 'key'
   * - the return value would change depending on if the service is local or
   * not.
   * 
   * FIXME - if not local - it needs to be prefixed by the gateway e.g.
   * {remote}.arduino.serial
   * @param reservedKey r
   * @return service interface
   */
  /*
   * public String getPeerName(String key){ if
   * (!serviceType.peers.containsKey(key)){ return null; } else {
   * ServiceReservation sr = serviceType.peers.get(key); // TODO !isLocal(){
   * return gw.getPrefix() + actualName return sr.actualName; } }
   */

  public synchronized ServiceInterface createPeer(String reservedKey) {
    String fullkey = Peers.getPeerKey(getName(), reservedKey);

    ServiceReservation sr = dnaPool.get(fullkey);
    if (sr == null) {
      error("can not create peer from reservedkey %s - no type definition !", fullkey);
      return null;
    }

    // WOW THIS WAS A NASTY BUG !!!
    // return Runtime.create(fullkey, sr.fullTypeName);
    return Runtime.create(sr.actualName, sr.fullTypeName);
  }

  public synchronized ServiceInterface createPeer(String reservedKey, String defaultType) {
    return Runtime.create(Peers.getPeerKey(getName(), reservedKey), defaultType);
  }

  /**
   * called typically from a remote system When 2 MRL instances are connected
   * they contain serialized non running Service in a registry, which is
   * maintained by the Runtime. The data can be stale.
   * 
   * Messages are sometimes sent (often in the gui) which prompt the remote
   * service to "broadcastState" a new serialized snapshot is broadcast to all
   * subscribed methods, but there is no guarantee that the registry is updated
   * 
   * This method will update the registry, additionally it will block until the
   * refresh response comes back
   * @param pulse p
   * @return a heartbeat
   */

  public Heartbeat echoHeartbeat(Heartbeat pulse) {
    return pulse;
  }

  public CommunicationInterface getComm() {
    return cm;
  }

  @Override
  public String[] getDeclaredMethodNames() {
    Method[] methods = getDeclaredMethods();
    String[] ret = new String[methods.length];

    log.info("getDeclaredMethodNames loading {} non-sub-routable methods", methods.length);
    for (int i = 0; i < methods.length; ++i) {
      ret[i] = methods[i].getName();
    }
    Arrays.sort(ret);
    return ret;
  }

  @Override
  public Method[] getDeclaredMethods() {
    return this.getClass().getDeclaredMethods();
  }

  public Inbox getInbox() {
    return inbox;
  }

  @Override
  public URI getInstanceId() {
    return instanceId;
  }

  public String getIntanceName() {
    return name;
  }

  @Override
  public Status getLastError() {
    return lastError;
  }

  // FIXME - use the method cache
  public Set<String> getMessageSet() {
    Set<String> ret = new TreeSet<String>();
    Method[] methods = getMethods();
    log.info("getMessageSet loading {} non-sub-routable methods", methods.length);
    for (int i = 0; i < methods.length; ++i) {
      ret.add(methods[i].getName());
    }
    return ret;
  }

  // FIXME - should be a "Set" not an array !
  @Override
  public String[] getMethodNames() {
    Method[] methods = getMethods();
    /*
     * Set<String> m = new TreeSet<String>(); m.addAll(methods);
     */
    String[] ret = new String[methods.length];

    log.info("getMethodNames loading {} non-sub-routable methods", methods.length);
    for (int i = 0; i < methods.length; ++i) {
      ret[i] = methods[i].getName();
    }

    Arrays.sort(ret);

    return ret;
  }

  @Override
  public Method[] getMethods() {
    return this.getClass().getMethods();
  }

  public Map<String, String> getInterfaceSet() {
    Map<String, String> ret = new TreeMap<String, String>();
    Class<?>[] interfaces = this.getClass().getInterfaces();
    for (int i = 0; i < interfaces.length; ++i) {
      Class<?> interfaze = interfaces[i];
      // ya silly :P - but gson's default conversion of a HashSet is an
      // array
      ret.put(interfaze.getName(), interfaze.getName());
    }
    return ret;
  }

  public Message getMsg() throws InterruptedException {
    return inbox.getMsg();
  }

  /**
   * 
   */
  @Override
  public ArrayList<MRLListener> getNotifyList(String key) {
    if (getOutbox() == null) {
      // this is remote system - it has a null outbox, because its
      // been serialized with a transient outbox
      // and your in a skeleton
      // use the runtime to send a message
      @SuppressWarnings("unchecked")
      // FIXME - parameters !
      ArrayList<MRLListener> remote = (ArrayList<MRLListener>) Runtime.getInstance().sendBlocking(getName(), "getNotifyList", new Object[] { key });
      return remote;

    } else {
      return getOutbox().notifyList.get(key);
    }
  }

  @Override
  public ArrayList<String> getNotifyListKeySet() {
    ArrayList<String> ret = new ArrayList<String>();
    if (getOutbox() == null) {
      // this is remote system - it has a null outbox, because its
      // been serialized with a transient outbox
      // and your in a skeleton
      // use the runtime to send a message
      @SuppressWarnings("unchecked")
      ArrayList<String> remote = (ArrayList<String>) Runtime.getInstance().sendBlocking(getName(), "getNotifyListKeySet");
      return remote;
    } else {
      ret.addAll(getOutbox().notifyList.keySet());
    }
    return ret;
  }

  public Outbox getOutbox() {
    return outbox;
  }

  public String getPeerKey(String key) {
    return Peers.getPeerKey(getName(), key);
  }

  /**
   * a default way to attach Services to other Services An example would be
   * attaching a Motor to a MotorControl or a Speaking service (TTS) to a
   * Listening service (STT) such that when the system is speaking it does not
   * try to listen &amp; act on its own speech (feedback loop)
   * 
   * FIXME - the SwingGui currently has attachGUI() and detachGUI() - these are
   * to bind Services with their swing views/tab panels. It should be
   * generalized to this attach method
   * @param subpath s
   * 
   * @return if successful
   * 
   */

  public String getServiceResourceFile(String subpath) {
    return FileIO.resourceToString(String.format("%s/%s", this.getSimpleName(), subpath));
  }

  @Override
  public String getSimpleName() {
    return simpleName;
  }

  public Thread getThisThread() {
    return thisThread;
  }

  @Override
  public String getType() {
    return getClass().getCanonicalName();
  }

  @Override
  public boolean hasError() {
    return lastError != null;
  }

  // TODO Clock example - roles
  // no - security (internal) Role - default access - ALLOW
  // WebGui - public - no security header - default access DISALLOW +
  // exception
  // WebGui (remote in genera) - user / group ALLOW

  /*
   * private boolean hasAccess(Message msg) { // turn into single key ??? //
   * type.name.method
   * 
   * // check this type <-- not sure i want to support this
   * 
   * // check this name &amp; method // if any access limitations exist which might
   * be applicable if (accessRules.containsKey(msg.name) ||
   * accessRules.containsKey(String.format("%s.%s", msg.name, msg.method))) { //
   * restricted service - check for authorization // Security service only
   * provides authorization ? if (security == null) { return false; } else {
   * return security.isAuthorized(msg); }
   * 
   * }
   * 
   * // invoke - SecurityException - log error return false; }
   */

  @Override
  public boolean hasPeers() {
    try {
      Class<?> theClass = Class.forName(serviceClass);
      Method method = theClass.getMethod("getPeers", String.class);
    } catch (Exception e) {
      log.debug("{} does not have a getPeers", serviceClass);
      return false;
    }
    return true;
  }

  public String help() {
    return help("url", "declared");
  }

  public String help(String format, String level) {
    StringBuffer sb = new StringBuffer();
    Method[] methods = this.getClass().getDeclaredMethods();
    TreeMap<String, Method> sorted = new TreeMap<String, Method>();

    for (int i = 0; i < methods.length; ++i) {
      Method m = methods[i];
      sorted.put(m.getName(), m);
    }
    for (String key : sorted.keySet()) {
      Method m = sorted.get(key);
      sb.append("/").append(getName()).append("/").append(m.getName());
      Class<?>[] types = m.getParameterTypes();
      if (types != null) {
        for (int j = 0; j < types.length; ++j) {
          Class<?> c = types[j];
          sb.append("/").append(c.getSimpleName());
        }
      }
      sb.append("\n");
    }

    sb.append("\n");
    return sb.toString();
  }

  @Override
  public void in(Message msg) {
    inbox.add(msg);
  }

  // BOXING - BEGIN --------------------------------------

  /**
   * This is where all messages are routed to and processed
   */
  @Override
  final public Object invoke(Message msg) {
    Object retobj = null;

    if (log.isDebugEnabled()) {
      log.debug("--invoking {}.{}({}) {} --", name, msg.method, CodecUtils.getParameterSignature(msg.data), msg.msgId);
    }

    // recently added - to support "nameless" messages - concept you may get
    // a message at this point
    // which does not belong to you - but is for a service in the same
    // Process
    // this is to support nameless Runtime messages but theoretically it
    // could
    // happen in other situations...
    if (!name.equals(msg.name)) {
      // wrong Service - get the correct one
      return Runtime.getService(msg.name).invoke(msg);
    }

    // SECURITY -
    // 0. allowing export - whether or not we'll allow services to be
    // exported - based on Type or Name
    // 1. we have firewall like rules where we can add inclusion and
    // exclusion rules - based on Type or Name - Service Level - Method
    // Level
    // 2. authentication & authorization
    // 3. transport mechanism (needs implementation on each type of remote
    // Communicator e.g. Xmpp RemoteAdapter WebGui etc...)

    // check for access
    // if access FAILS ! - check for authenticated access
    // not needed "centrally" - instead will impement in Communicators
    // which hand foriegn connections
    // if (security == null || security.isAuthorized(msg)) {

    // "local" invoke - you have a "real" reference
    retobj = invokeOn(this, msg.method, msg.data);
    // }

    // retobject will be returned as another
    // message
    return retobj;
  }

  @Override
  final public Object invoke(String method) {
    return invokeOn(this, method, (Object[]) null);
  }

  @Override
  final public Object invoke(String method, Object... params) {
    return invokeOn(this, method, params);
  }

  /**
   * the core working invoke method
   * 
   * @param obj - the object
   * @param method - the method to invoke on that object
   * @param params - the list of args to pass to the method
   * @return return object
   */
  @Override
  final public Object invokeOn(Object obj, String method, Object... params) {

    if (obj == null) {
      log.error("invokeOn object is null");
      return null;
    }

    Object retobj = null;
    Class<?> c = null;
    Class<?>[] paramTypes = null;

    try {
      c = obj.getClass();

      if (params != null) {
        paramTypes = new Class[params.length];
        for (int i = 0; i < params.length; ++i) {
          if (params[i] != null) {
            paramTypes[i] = params[i].getClass();
          } else {
            paramTypes[i] = null;
          }
        }
      }
      Method meth = null;

      // TODO - method cache map
      // can not auto-box or downcast with this method - getMethod will
      // return a "specific & exact" match based
      // on parameter types - the thing is we may have a typed signature
      // which will allow execution - but
      // if so we need to search

      // FIXME - WHY ISN'T METHOD CACHING USED HERE !!!

      // SECURITY - ??? can't be implemented here - need a full message
      meth = c.getMethod(method, paramTypes); // getDeclaredMethod zod !!!
      retobj = meth.invoke(obj, params);

      // put return object onEvent
      out(method, retobj);
    } catch (NoSuchMethodException e) {

      // cache key compute

      // TODO: validate what "params.toString()" returns.
      StringBuilder keyBuilder = new StringBuilder();
      if (paramTypes != null) {
        for (Object o : paramTypes) {
          keyBuilder.append(o);
        }
      }

      Method mC = LRUMethodCache.getInstance().getCacheEntry(obj, method, paramTypes);
      if (mC != null) {
        // We found a cached hit! lets invoke on that.
        try {
          retobj = mC.invoke(obj, params);
          // put return object onEvent
          out(method, retobj);
          // return
          return retobj;
        } catch (Exception e1) {
          log.error("boom goes method {}", mC.getName());
          Logging.logError(e1);
        }

      }

      // TODO - build method cache map from errors
      log.info("no such method {}.{} - attempting upcasting", c.getSimpleName(), MethodEntry.getPrettySignature(method, paramTypes, null));

      // TODO - optimize with a paramter TypeConverter & Map
      // c.getMethod - returns on EXACT match - not "Working" match
      Method[] allMethods = c.getMethods(); // ouch
      log.info("searching through {} methods", allMethods.length);

      for (Method m : allMethods) {
        String mname = m.getName();
        if (!mname.equals(method)) {
          continue;
        }

        Type[] pType = m.getGenericParameterTypes();
        // checking parameter lengths
        if (params == null && pType.length != 0 || pType.length != params.length) {
          continue;
        }
        try {
          log.debug("found appropriate method");
          retobj = m.invoke(obj, params);
          // put return object onEvent
          out(method, retobj);
          // we've found a match. put that in the cache.
          log.debug("caching method cache key");
          LRUMethodCache.getInstance().addCacheEntry(obj, method, paramTypes, m);
          return retobj;
        } catch (Exception e1) {
          log.error("boom goes method {}", m.getName());
          Logging.logError(e1);
        }
      }

      log.error("did not find method - {}.{}({})", obj, method, CodecUtils.getParameterSignature(params));
    } catch (Exception e) {
      log.error("{}", e.getClass().getSimpleName(), e);
    }

    return retobj;
  }

  @Override
  public boolean isLocal() {
    return instanceId == null;
  }

  @Override
  public boolean isRuntime() {
    return Runtime.class == this.getClass();
  }

  @Override
  public boolean isReady() {
    return true;
  }

  @Override
  public boolean isRunning() {
    return isRunning;
  }

  /**
   * method of de-serializing default will to load simple xml from name file
   */
  @Override
  public boolean load() {
    return load(null, null);
  }

  public boolean load(Object o, String inCfgFileName) {
    String filename = null;
    if (inCfgFileName == null) {
      filename = String.format("%s%s%s.json", cfgDir, File.separator, String.format("%s-%s", getClass().getSimpleName(), getName()));
    } else {
      filename = inCfgFileName;
    }
    if (o == null) {
      o = this;
    }

    try {
      File cfg = new File(filename);
      if (cfg.exists()) {
        // serializer.read(o, cfg);
        String json = FileIO.toString(filename);
        Object saved = CodecUtils.fromJson(json, o.getClass()); 
        
        copyShallowFrom(o, saved);
        return true;
      }
      log.info("cfg file {} does not exist", filename);
    } catch (Exception e) {
      Logging.logError(e);
    }
    return false;
  }

  public void out(Message msg) {
    outbox.add(msg);
  }

  /**
   * Creating a message function call - without specifying the recipients -
   * static routes will be applied this is good for Motor drivers - you can swap
   * motor drivers by creating a different static route The motor is not "Aware"
   * of the driver - only that it wants to method="write" data to the driver
   */
  public void out(String method, Object o) {
    Message m = Message.createMessage(this, null, method, o); // create a
                                                              // un-named
                                                              // message
                                                              // as output

    if (m.sender.length() == 0) {
      m.sender = this.getName();
    }
    if (m.sendingMethod.length() == 0) {
      m.sendingMethod = method;
    }
    if (outbox == null) {
      log.info("******************OUTBOX IS NULL*************************");
      return;
    }
    outbox.add(m);
  }

  // override for extended functionality
  public boolean preProcessHook(Message m) {
    return true;
  }

  // override for extended functionality
  public boolean preRoutingHook(Message m) {
    return true;
  }

  /**
   * framework diagnostic publishing method for examining load, capacity, and
   * throughput of Inbox &amp; Outbox queues
   * @param stats s
   * @return the stats
   */
  public QueueStats publishQueueStats(QueueStats stats) {
    return stats;
  }

  /**
   * publishing point for the whole service the entire Service is published
   * @return the service
   */
  public Service publishState() {
    return this;
  }

  /**
   * FIXME - implement This SHOULD NOT be called by the framework - since - the
   * framework does not know about dna mutation - or customizations which have
   * been applied such that Arduinos are shared between services or peers of
   * services
   * 
   * It SHOULD shutdown all the peers of a service - but it SHOULD NOT be
   * automatically called by the framework. If the 'user' wants to release all
   * peers - it should fufill the request
   */
  @Override
  public void releasePeers() {
    log.info("dna - {}", dnaPool.toString());
    String myKey = getName();
    log.info("releasePeers ({}, {})", myKey, serviceClass);
    try {
      // TODO: what the heck does this thing do?
      Class<?> theClass = Class.forName(serviceClass);
      Method method = theClass.getMethod("getMetaData");
      ServiceType serviceType = (ServiceType) method.invoke(null);
      Map<String, ServiceReservation> peers = serviceType.getPeers();
      // FIXME - recursively release peers

    } catch (Exception e) {
      log.debug("{} does not have a getPeers", serviceClass);
    }
  }

  /**
   * Releases resources, and unregisters service from the runtime
   */
  @Override
  public void releaseService() {

    // recently added - preference over detach(Runtime.getService(getName()));
    // since this service is releasing - it should be detached from all existing services
    detach();

    // note - if stopService is overwritten with extra
    // threads - releaseService will need to be overwritten too
    stopService();

    // TODO ? detach all other services currently attached
    // detach();
    // @grog is it ok for now ?

    // GroG says, I don't think so - this is releasing itself from itself 
    // detach(Runtime.getService(getName()));

    // FIXME - deprecate - peers are no longer used ...
    releasePeers();

    purgeTasks();

    Runtime.release(getName());
  }

  /**
   * 
   */
  public void removeAllListeners() {
    outbox.notifyList.clear();
  }

  @Override
  public void removeListener(String outMethod, String serviceName, String inMethod) {
    if (outbox.notifyList.containsKey(outMethod)) {
      ArrayList<MRLListener> nel = outbox.notifyList.get(outMethod);
      for (int i = 0; i < nel.size(); ++i) {
        MRLListener target = nel.get(i);
        if (target.callbackName.compareTo(serviceName) == 0) {
          nel.remove(i);
          log.info("removeListener requested {}.{} to be removed", serviceName, outMethod);
        }
      }
    } else {
      log.error("removeListener requested {}.{} to be removed - but does not exist", serviceName, outMethod);
    }
  }

  // ---------------- logging end ---------------------------

  @Override
  public boolean requiresSecurity() {
    return authProvider != null;
  }

  /**
   * Reserves a name for a Peer Service. This is important for services which
   * control other services. Internally composite services will use a key so the
   * name of the peer service can change, effectively binding a new peer to the
   * composite
   * 
   * @param key
   *          internal key name of peer service
   * @param simpleTypeName
   *          type of service
   * @param comment
   *          comment detailing the use of the peer service within the composite
   */
  public void reserve(String key, String simpleTypeName, String comment) {
    // creating
    String peerKey = getPeerKey(key);
    reserveRoot(peerKey, simpleTypeName, comment);
  }

  public void reserve(String key, String actualName, String simpleTypeName, String comment) {
    // creating
    String peerKey = getPeerKey(key);
    reserveRoot(peerKey, actualName, simpleTypeName, comment);
  }

  @Override
  final public void run() {
    isRunning = true;

    try {
      while (isRunning) {
        // TODO should this declaration be outside the while loop? if
        // so, make sure to release prior to continue
        Message m = getMsg();

        if (!preRoutingHook(m)) {
          continue;
        }

        // nameless Runtime messages
        if (m.name == null) {
          // don't know if this is "correct"
          // but we are substituting the Runtime name as soon as we
          // see that its a null
          // name message
          m.name = Runtime.getInstance().getName();
        }

        // route if necessary
        if (!m.getName().equals(this.getName())) // && RELAY
        {
          outbox.add(m); // RELAYING
          continue; // sweet - that was a long time coming fix !
        }

        if (!preProcessHook(m)) {
          // if preProcessHook returns false
          // the message does not need to continue
          // processing
          continue;
        }
        // TODO should this declaration be outside the while loop?
        Object ret = invoke(m);
        if (Message.BLOCKING.equals(m.status)) {
          // TODO should this declaration be outside the while loop?
          // create new message reverse sender and name set to same
          // msg id
          Message msg = Message.createMessage(this, m.sender, m.method, ret);
          msg.sender = this.getName();
          msg.msgId = m.msgId;
          // msg.status = Message.BLOCKING;
          msg.status = Message.RETURN;

          outbox.add(msg);
        }
      }
    } catch (InterruptedException edown) {
      info("shutting down");
    } catch (Exception e) {
      error(e);
    }
  }

  /**
   * method of serializing default will be simple xml to name file
   */
  @Override
  public boolean save() {

    try {
      File cfg = new File(String.format("%s%s%s.json", cfgDir, File.separator, String.format("%s-%s", getClass().getSimpleName(), getName())));
      // serializer.write(this, cfg);
      info("saving %s", cfg.getName());

      if (this instanceof Runtime) {
        info("we cant serialize runtime yet");
        return false;
      }

      String s = CodecUtils.toJson(this);
      FileOutputStream out = new FileOutputStream(cfg);
      out.write(s.getBytes());
      out.close();
    } catch (Exception e) {
      Logging.logError(e);
      return false;
    }
    return true;
  }

  public boolean save(Object o, String cfgFileName) {

    try {
      File cfg = new File(String.format("%s%s%s", cfgDir, File.separator, cfgFileName));
      String s = CodecUtils.toJson(o);
      FileOutputStream out = new FileOutputStream(cfg);
      out.write(s.getBytes());
      out.close();
    } catch (Exception e) {
      Logging.logError(e);
      return false;
    }
    return true;
  }

  public boolean save(String cfgFileName, String data) {
    // saves user data in the .myrobotlab directory
    // with the file naming convention of name.<cfgFileName>
    try {
      FileIO.toFile(String.format("%s%s%s.%s", cfgDir, File.separator, this.getName(), cfgFileName), data);
    } catch (Exception e) {
      Logging.logError(e);
      return false;
    }
    return true;
  }

  /**
   * 0?
   */
  public void send(String name, String method) {
    send(name, method, (Object[]) null);
  }

  public void send(String name, String method, Object... data) {
    Message msg = Message.createMessage(this, name, method, data);
    msg.sender = this.getName();
    // All methods which are invoked will
    // get the correct sendingMethod
    // here its hardcoded
    msg.sendingMethod = "send";
    send(msg);
  }

  public void send(Message msg) {
    if (recorder != null) {
      try {
        recorder.write(msg);
      } catch (IOException e) {
        log.error("recording failed", e);
      }
    }
    outbox.add(msg);
  }

  /**
   * this send forces remote connect - for registering services
   * @param url u
   * @param method m 
   * @param param1 the param
   */
  public void send(URI url, String method, Object param1) {
    Object[] params = new Object[1];
    params[0] = param1;
    Message msg = Message.createMessage(this, name, method, params);
    outbox.getCommunicationManager().send(url, msg);
  }

  public Object sendBlocking(String name, Integer timeout, String method, Object... data) {
    Message msg = Message.createMessage(this, name, method, data);
    msg.sender = this.getName();
    msg.status = Message.BLOCKING;
    msg.msgId = Runtime.getUniqueID();

    return sendBlocking(msg, timeout);
  }

  public Object sendBlocking(Message msg, Integer timeout) {
    Object[] returnContainer = new Object[1];
    /*
     * if (inbox.blockingList.contains(msg.msgID)) { log.error("DUPLICATE"); }
     */
    inbox.blockingList.put(msg.msgId, returnContainer);

    try {
      // block until message comes back
      synchronized (returnContainer) {
        outbox.add(msg);
        returnContainer.wait(timeout); // NEW !!! TIMEOUT !!!!
      }
    } catch (InterruptedException e) {
      log.error("interrupted", e);
    }

    return returnContainer[0];
  }

  // BOXING - End --------------------------------------
  public Object sendBlocking(String name, String method) {
    return sendBlocking(name, method, (Object[]) null);
  }

  public Object sendBlocking(String name, String method, Object... data) {
    // default 1 second timeout - FIXME CONFIGURABLE
    return sendBlocking(name, 1000, method, data);
  }

  @Override
  public void setInstanceId(URI uri) {
    instanceId = uri;
  }

  /**
   * rarely should this be used. Gateways use it to provide x-route natting
   * services by re-writing names with prefixes
   */

  @Override
  public void setName(String name) {
    // this.name = String.format("%s%s", prefix, name);
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  public Service setState(Service s) {
    return (Service) copyShallowFrom(this, s);
  }

  public void setThisThread(Thread thisThread) {
    this.thisThread = thisThread;
  }

  public void startHeartbeat() {
    // getComm().
  }

  public ServiceInterface startPeer(String reservedKey) {
    ServiceInterface si = null;
    try {
      si = createPeer(reservedKey);
      if (si == null) {
        error("could not create service from key %s", reservedKey);
        return null;
      }

      si.startService();
    } catch (Exception e) {
      error(e.getMessage());
      Logging.logError(e);
    }
    return si;
  }

  public ServiceInterface startPeer(String reservedKey, String defaultType) throws Exception {
    ServiceInterface si = createPeer(reservedKey, defaultType);
    if (si == null) {
      error("could not create service from key %s", reservedKey);
    }

    si.startService();
    return si;
  }

  public void startRecording() {
    invoke("startRecording", new Object[] { null });
  }

  @Override
  public void startService() {
    ServiceInterface si = Runtime.getService(name);
    if (si == null) {
      Runtime.create(name, getSimpleName());
    }
    if (!isRunning()) {
      outbox.start();
      if (thisThread == null) {
        thisThread = new Thread(this, name);
      }
      thisThread.start();
      isRunning = true;
    } else {
      log.debug("startService request: service {} is already running", name);
    }
  }

  public void stopHeartbeat() {
  }

  public void stopMsgRecording() {
    log.info("stopped recording");
    if (recorder != null) {
      try {
        recorder.stop();
      } catch (Exception e) {
        Logging.logError(e);
      }
    }
  }

  /**
   * Stops the service. Stops threads.
   */
  @Override
  public void stopService() {
    isRunning = false;
    outbox.stop();
    if (thisThread != null) {
      thisThread.interrupt();
    }
    thisThread = null;
    // save(); removed by GroG
  }

  // -------------- Messaging Begins -----------------------
  public void subscribe(NameProvider topicName, String topicMethod) {
    String callbackMethod = CodecUtils.getCallBackName(topicMethod);
    subscribe(topicName.getName(), topicMethod, getName(), callbackMethod);
  }

  public void subscribe(String topicName, String topicMethod) {
    String callbackMethod = CodecUtils.getCallBackName(topicMethod);
    subscribe(topicName, topicMethod, getName(), callbackMethod);
  }

  public void subscribe(String topicName, String topicMethod, String callbackName, String callbackMethod) {
    log.info("subscribe [{}/{} ---> {}/{}]", topicName, topicMethod, callbackName, callbackMethod);
    MRLListener listener = new MRLListener(topicMethod, callbackName, callbackMethod);
    cm.send(Message.createMessage(this, topicName, "addListener", listener));
  }

  public void sendPeer(String peerKey, String method, Object... params) {
    cm.send(Message.createMessage(this, getPeerName(peerKey), method, params));
  }

  public void unsubscribe(NameProvider topicName, String topicMethod) {
    String callbackMethod = CodecUtils.getCallBackName(topicMethod);
    unsubscribe(topicName.getName(), topicMethod, getName(), callbackMethod);
  }

  public void unsubscribe(String topicName, String topicMethod) {
    String callbackMethod = CodecUtils.getCallBackName(topicMethod);
    unsubscribe(topicName, topicMethod, getName(), callbackMethod);
  }

  public void unsubscribe(String topicName, String topicMethod, String callbackName, String callbackMethod) {
    log.info("unsubscribe [{}/{} ---> {}/{}]", topicName, topicMethod, callbackName, callbackMethod);
    cm.send(Message.createMessage(this, topicName, "removeListener", new Object[] { topicMethod, callbackName, callbackMethod }));
  }

  // -------------- Messaging Ends -----------------------
  // ---------------- Status processing begin ------------------
  public Status error(Exception e) {
    Status ret = Status.error(e);
    ret.name = getName();
    invoke("publishStatus", ret);
    return ret;
  }

  @Override
  public Status error(String format, Object... args) {
    Status ret = Status.error(String.format(format, args));
    ret.name = getName();
    invoke("publishStatus", ret);
    return ret;
  }

  public Status error(String msg) {
    return error(msg, (Object[]) null);
  }

  public Status warn(String msg) {
    return warn(msg, (Object[]) null);
  }

  @Override
  public Status warn(String format, Object... args) {
    Status status = Status.warn(format, args);
    invoke("publishStatus", status);
    return status;
  }

  /**
   * set status broadcasts an info string to any subscribers
   * @param msg m
   * @return string
   */
  public Status info(String msg) {
    return info(msg, (Object[]) null);
  }

  /**
   * set status broadcasts an formatted info string to any subscribers
   */
  @Override
  public Status info(String format, Object... args) {
    Status status = Status.info(format, args);
    invoke("publishStatus", status);
    return status;
  }

  /**
   * error only channel publishing point versus publishStatus which handles
   * info, warn &amp; error
   * @param status status
   * @return the status
   */
  public Status publishError(Status status) {
    return status;
  }

  public Status publishStatus(Status status) {
    status.name = getName();
    if (status.level.equals(StatusLevel.ERROR)) {
      lastError = status;
      log.error(status.toString());
      invoke("publishError", status);
    } else {
      log.info(status.toString());
    }
    return status;
  }

  // ---------------- Status processing end ------------------
  @Override
  public String toString() {
    return getName();
  }

  // interesting this is not just in memory
  public Map<String, MethodEntry> getMethodMap() {
    return Runtime.getMethodMap(getName());
  }

  @Override
  public void updateStats(QueueStats stats) {
    invoke("publishStats", stats);
  }

  @Override
  public QueueStats publishStats(QueueStats stats) {
    // log.error(String.format("===stats - dequeued total %d - %d bytes in
    // %d ms %d Kbps",
    // stats.total, stats.interval, stats.ts - stats.lastTS, 8 *
    // stats.interval/ (stats.delta)));
    return stats;
  }

  /*
   * static public ArrayList<ServiceReservation> getPeerMetaData(String
   * serviceType) { ArrayList<ServiceReservation> peerList = new
   * ArrayList<ServiceReservation>(); try {
   * 
   * Class<?> theClass = Class.forName(serviceType); Method method =
   * theClass.getMethod("getPeers", String.class); Peers peers = (Peers)
   * method.invoke(null, new Object[] { "" }); if (peers != null) { log.info(
   * "has peers"); peerList = peers.getDNA().flatten();
   * 
   * // add peers to serviceData serviceType }
   * 
   * } catch (Exception e) { // dont care }
   * 
   * return peerList; }
   */

  /**
   * Calls the static method getMetaData on the appropriate class. The class
   * static data is passed back as a template to be merged in with the global
   * static dna
   * @param serviceClass sc
   * @return the service type info
   */
  static public ServiceType getMetaData(String serviceClass) {
    String serviceType;
    if (!serviceClass.contains(".")) {
      serviceType = String.format("org.myrobotlab.service.%s", serviceClass);
    } else {
      serviceType = serviceClass;
    }

    try {

      Class<?> theClass = Class.forName(serviceType);

      // execute static method to get meta data

      Method method = theClass.getMethod("getMetaData");
      ServiceType meta = (ServiceType) method.invoke(null);
      return meta;

    } catch (Exception e) {
      // dont care
    }

    return null;
  }

  public String getDescription() {
    String description = getMetaData(getClass().getSimpleName()).getDescription();
    return description;
  }

  /**
   * Attachable.detach(serviceName) - routes to reference parameter
   * Attachable.detach(Attachable)
   */
  public void detach(String serviceName) {
    detach(Runtime.getService(serviceName));
  }

  /**
   * detaches ALL other services from this service
   */
  public void detach() {
    log.info("detach was called but I'm a NOOP in Service.java - probably not what you wanted - override me !");
    // FIXME - attach should probably have a Service.java level of understanding where a Service understands
    // that another service is attached
  }

  /**
   * Attachable.attach(serviceName) - routes to reference parameter
   * Attachable.attach(Attachable)
   */
  public void attach(String serviceName) throws Exception {
    attach(Runtime.getService(serviceName));
  }

  public boolean isAttached(String serviceName) {
    return isAttached(Runtime.getService(serviceName));
  }

  /**
   * This detach when overriden "routes" to the appropriately typed parameterized
   * detach within a service.
   * 
   * When overriden, the first thing it should do is check to see if the
   * referenced service is already detached. If it is already detached it should
   * simply return.
   * 
   * If its detached to this service, it should first detach itself, modifying
   * its own data if necessary. The last thing it should do is call the
   * parameterized service's detach. This gives the other service an opportunity
   * to detach. e.g.
   * 
   * <pre>
   * 
   * public void detach(Attachable service) {
   *    if (ServoControl.class.isAssignableFrom(service.getClass())) {
   *        detachServoControl((ServoControl) service);
   *        return;
   *    }
   *    
   *    ...  route to more detach functions   ....
   *    
   *    error("%s doesn't know how to detach a %s", getClass().getSimpleName(), service.getClass().getSimpleName());
   *  }
   *  
   *  And within detachServoControl :
   *  
   *  public void detachServoControl(ServoControl service) {
   *       // guard
   *       if (!isAttached(service)){
   *           return;
   *       }
   *       
   *       ... detach logic ....
   * 
   *       // call to detaching service
   *       service.detach(this);  
   * }  
   * </pre>
   * 
   * @param service
   *          - the service to detach from this service
   */
  @Override
  public void detach(Attachable service) {
  }

  /**
   * the "routing" isAttached - when overridden by a service this
   * "routes" to the appropriate typed isAttached
   */
  @Override
  public boolean isAttached(Attachable instance) {
    return false;
  }

  /**
   * returns all currently attached services
   */
  @Override
  public Set<String> getAttached() {
    return new HashSet<String>();
  }

  /**
   * This attach when overriden "routes" to the appropriately typed parameterized
   * attach within a service.
   * 
   * When overriden, the first thing it should do is check to see if the
   * referenced service is already attached. If it is already attached it should
   * simply return.
   * 
   * If its attached to this service, it should first attach itself, modifying
   * its own data if necessary. The last thing it should do is call the
   * parameterized service's attach. This gives the other service an opportunity
   * to attach. e.g.
   * 
   * <pre>
   * 
   * public void attach(Attachable service) {
   *    if (ServoControl.class.isAssignableFrom(service.getClass())) {
   *        attachServoControl((ServoControl) service);
   *        return;
   *    }
   *    
   *    ...  route to more attach functions   ....
   *    
   *    error("%s doesn't know how to attach a %s", getClass().getSimpleName(), service.getClass().getSimpleName());
   *  }
   *  
   *  And within attachServoControl :
   *  
   *  public void attachServoControl(ServoControl service) {
   *       // guard
   *       if (!isAttached(service)){
   *           return;
   *       }
   *       
   *       ... attach logic ....
   * 
   *       // call to attaching service
   *       service.attach(this);  
   * }  
   * </pre>
   * 
   * @param service
   *          - the service to attach from this service
   */
  @Override
  public void attach(Attachable service) throws Exception {
    log.info("Service.attach does not know how to attach {} to a {}", service.getClass().getSimpleName(), this.getClass().getSimpleName());
  }

  public void setVirtual(boolean b) {
    this.isVirtual = b;
  }

  public boolean isVirtual() {
    return isVirtual;
  }
}
