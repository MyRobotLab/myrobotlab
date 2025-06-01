package org.myrobotlab.framework;

/**
 * A Peer is a service "used" by another service. Services which use other
 * services are called composites. The naming convention for peers is
 * {parentName}.{peerName} E.g. in the case of InMoov head where InMoov service
 * is named i01, the default name of the head peer is i01.head. InMoov's "peer
 * key" of the head is simply called "head". The peerKey is hardcoded, but the
 * actual name and type of the peer can change via configuration. This allows
 * easy to swap services based on configuration. It also allows sharing of
 * services. A good example of this is the i01.headTracking.cv and
 * i01.eyeTracking.cv. In InMoov default config they share an opencv service
 * called i01.opencv (the actual name)
 *
 * @author GroG
 */
public class Peer {

  /**
   * The actual name of the peer - can differ from the hardcoded "peerKey" the
   * composite service uses to refer to this peer.
   */
  public String name;
  /**
   * default type of this peer - can be replaced by another service type if the
   * interface matches. E.g. MarySpeech vs Polly
   */
  public String type;
  /**
   * autoStart will request the runtime start this peer when its composite
   * starts. Ultimate control is given to the Plan's RuntimeConfig.registry
   * entry. If runtime.yml does not specify its startup, and all service config
   * are saved as files which repress the starting of this peer, the peer will
   * not start even if default autoStart is true.
   */
  public boolean autoStart = true;

  public Peer() {
  }

  public Peer(String name, String type) {
    this.name = name;
    this.type = type;
  }

  public Peer(String name, String type, boolean autoStart) {
    this.name = name;
    this.type = type;
    this.autoStart = autoStart;
  }

  public String toString() {
    return String.format("peer %s %s %b", name, type, autoStart);
  }
}
