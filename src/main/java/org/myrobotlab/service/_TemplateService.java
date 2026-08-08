package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config._TemplateServiceConfig;
import org.slf4j.Logger;

/**
 * Copy this class (plus {@code _TemplateServiceConfig} and
 * {@code _TemplateServiceMeta}) when creating a new service.
 * <p>
 * Agent / API guidance:
 * <ul>
 * <li>Prefer typed fields on {@code *Config} and real Java methods over
 * string-only {@code invoke("method")} as the primary surface.</li>
 * <li>Declare runtime jars in {@code *Meta.addDependency}, and mirror them in
 * {@code pom.xml} — see {@code doc/agent/dependency-updates.md}.</li>
 * <li>Keep WebGui JS / resource scripts in sync when changing pub/sub topics.</li>
 * </ul>
 */
public class _TemplateService extends Service<_TemplateServiceConfig>
{

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(_TemplateService.class);

  public _TemplateService(String n, String id) {
    super(n, id);
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Runtime.start("template", "_TemplateService");
      Runtime.start("webgui", "WebGui");

    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
