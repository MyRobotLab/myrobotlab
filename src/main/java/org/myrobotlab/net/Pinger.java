package org.myrobotlab.net;

import java.net.InetAddress;
import java.util.Map;

import org.myrobotlab.framework.interfaces.Invoker;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class Pinger implements Runnable {

  public final static Logger log = LoggerFactory.getLogger(Pinger.class);

  protected String ip;
  protected int timeout;
  protected Invoker invoker;
  protected Map<String, Host> hosts;

  public Pinger(Invoker invoker, Map<String, Host> hosts, String ip, int timeout) {
    this.ip = ip;
    this.timeout = timeout;
    this.invoker = invoker;
    this.hosts = hosts;
  }

  @Override
  public void run() {
    try {
      if (InetAddress.getByName(ip).isReachable(timeout)) {
        Host h = null;
        if (hosts.containsKey(ip)) {
          h = hosts.get(ip);
        } else {
          h = new Host();
          h.ip = ip;
          h.name = "new-host";
          hosts.put(ip, h);
        }

        if (h.state == null || "unknown".equals(h.state)) {
          invoker.invoke("publishFoundNewHost", h);
        }

        h.state = "active";
        h.lastActiveTs = System.currentTimeMillis();
        invoker.invoke("publishFoundHost", h);

        log.debug("host {}", h);
      } else {
        if (hosts.containsKey(ip)) {
          Host h = hosts.get(ip);
          if ("active".equals(h.state)) {
            log.info("host {} transitioned to unknown state", h);
            invoker.invoke("publishLostHost", h);
          }
          h.state = "unknown";
        }
      }

    } catch (Exception e) {
      log.error("pinger threw", e);
    }
  }

}
