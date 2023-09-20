package org.myrobotlab.framework.repo;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.Ivy;
import org.apache.ivy.Main;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.util.AbstractMessageLogger;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.filter.Filter;
import org.apache.ivy.util.filter.NoFilter;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.StatusLevel;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.io.Zip;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;

// FIXME - 2 layer abstraction - because to generate build files and 
// other critical methods - they do not require actual "ivy" components
// so these methods should be un-hindered by actual maven, gradle or ivy imports !
public class IvyWrapper extends Repo implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final String IVY_VERSION = "2.5.1";

  class IvyWrapperLogger extends AbstractMessageLogger {

    private int level = Message.MSG_INFO;

    public IvyWrapperLogger(int level) {
      this.level = level;
    }

    @Override
    public void doEndProgress(String msg) {
      log.info(msg);
    }

    @Override
    public void doProgress() {
      // log.info(".");
    }

    public int getLevel() {
      return level;
    }

    @Override
    public void log(String msg, int level) {
      if (level <= this.level) {
        publishStatus(msg, level);
      }
    }

    @Override
    public void rawlog(String msg, int level) {
      log(msg, level);
    }
  }

  static String ivysettingsXmlTemplate = null;

  static String ivyXmlTemplate = null;

  transient static IvyWrapper localInstance = null;

  public static final Filter NO_FILTER = NoFilter.INSTANCE;

  static public Repo getTypeInstance() {
    if (localInstance == null) {
      init();
    }
    return localInstance;
  }

  static private synchronized void init() {
    if (localInstance == null) {
      localInstance = new IvyWrapper();
      ivyXmlTemplate = FileIO.resourceToString("framework/ivy.xml.template");
      ivysettingsXmlTemplate = FileIO.resourceToString("framework/ivysettings.xml.template");
    }
  }

  /**
   * <pre>
   * generates all ivy related build files based on their meta data
   * 
   * ivy.xml
   * ivysettings.xml 
   * build.xml
   * 
   * </pre>
   * 
   * @param location
   *          - location of work directory
   * @param serviceTypes
   *          - list of services to process
   * 
   */
  @Override
  public void createBuildFiles(String location, String[] serviceTypes) {
    try {

      location = createWorkDirectory(location);
      createIvySettings(location);
      createIvy(location, serviceTypes);

    } catch (Exception e) {
      log.error("could not generate build files", e);
    }
  }

  public void createIvyForDependency(String location, ServiceDependency dependency) throws IOException {
    Map<String, String> snr = new HashMap<String, String>();
    StringBuilder sb = new StringBuilder();

    StringBuilder ret = new StringBuilder();

    ret.append("  <dependencies>\n\n");

    // for (ServiceDependency dependency : dependencies) {

    sb.append("  <dependency"); // conf="provided->master"
    sb.append(String.format(" org=\"%s\" name=\"%s\" rev=\"%s\"", dependency.getOrgId(), dependency.getArtifactId(),
        dependency.getVersion() == null ? "latest.integration" : dependency.getVersion()));

    List<ServiceExclude> excludes = dependency.getExcludes();
    boolean twoTags = dependency.getExt() != null || excludes != null && excludes.size() > 0;
    if (twoTags) {
      // more stuffs ! - we have 2 tags - end this one without />
      sb.append(">\n");
    }

    if (dependency.getExt() != null) {
      // http://ant.apache.org/ivy/history/latest-milestone/ivyfile/artifact.html
      // " <artifact name=\"foo-src\" type=\"%s\" ext=\"%s\"
      // conf=\"provided->master\"
      // />\n",
      sb.append(String.format("    <artifact name=\"%s\" type=\"%s\" ext=\"%s\" />\n", dependency.getArtifactId(), dependency.getExt(), dependency.getExt()));
    }

    // exclusions begin ---
    if (excludes != null && excludes.size() > 0) {
      StringBuilder ex = new StringBuilder();
      for (ServiceExclude exclude : excludes) {
        ex.append("      <exclude ");
        ex.append(String.format(" org=\"%s\" ", exclude.getOrgId()));
        ex.append(String.format(" name=\"%s\" ", exclude.getArtifactId()));
        ex.append("/>\n");
      }

      sb.append(ex);
    }

    if (twoTags) {
      sb.append("  </dependency>\n");
    } else {
      // single tag
      sb.append("/>\n");
    }
    // exclusions end ---
    // } // for each dependency
    // sb.append(String.format("<!-- %s end -->\n\n",
    // service.getSimpleName()));

    sb.append("\n");

    ret.append(sb);

    ret.append("  </dependencies>\n");

    snr.put("{{dependencies}}", ret.toString());

    createFilteredFile(snr, location, "ivy", "xml");
  }

  public void createIvy(String location, String[] serviceTypes) throws IOException {
    Map<String, String> snr = new HashMap<String, String>();
    StringBuilder sb = null;

    StringBuilder ret = new StringBuilder();
    ServiceData sd = ServiceData.getLocalInstance();
    if (serviceTypes == null) {
      List<MetaData> ats = sd.getAvailableServiceTypes();
      serviceTypes = new String[ats.size()];
      for (int i = 0; i < ats.size(); ++i) {
        serviceTypes[i] = ats.get(i).getType();
      }
    }

    ret.append("  <dependencies>\n\n");

    for (String serviceType : serviceTypes) {
      MetaData service = ServiceData.getMetaData(serviceType);
      Set<ServiceDependency> dependencies = getUnfulfilledDependencies(serviceType);

      if (dependencies.size() == 0) {
        continue;
      }

      sb = new StringBuilder();
      sb.append(String.format("  <!-- %s -->\n", service.getSimpleName()));

      for (ServiceDependency dependency : dependencies) {
        if (service.includeServiceInOneJar()) {
          continue;
        }

        sb.append("  <dependency"); // conf="provided->master"
        sb.append(String.format(" org=\"%s\" name=\"%s\" rev=\"%s\"", dependency.getOrgId(), dependency.getArtifactId(),
            dependency.getVersion() == null ? "latest.integration" : dependency.getVersion()));

        List<ServiceExclude> excludes = dependency.getExcludes();
        boolean twoTags = dependency.getExt() != null || excludes != null & excludes.size() > 0;
        if (twoTags) {
          // more stuffs ! - we have 2 tags - end this one without />
          sb.append(">\n");
        }

        if (dependency.getExt() != null) {
          // http://ant.apache.org/ivy/history/latest-milestone/ivyfile/artifact.html
          // " <artifact name=\"foo-src\" type=\"%s\" ext=\"%s\"
          // conf=\"provided->master\"
          // />\n",
          sb.append(String.format("    <artifact name=\"%s\" type=\"%s\" ext=\"%s\" />\n", dependency.getArtifactId(), dependency.getExt(), dependency.getExt()));
        }

        // exclusions begin ---
        if (excludes != null & excludes.size() > 0) {
          StringBuilder ex = new StringBuilder();
          for (ServiceExclude exclude : excludes) {
            ex.append("      <exclude ");
            ex.append(String.format(" org=\"%s\" ", exclude.getOrgId()));
            ex.append(String.format(" name=\"%s\" ", exclude.getArtifactId()));
            ex.append("/>\n");
          }

          sb.append(ex);
        }

        if (twoTags) {
          sb.append("  </dependency>\n");
        } else {
          // single tag
          sb.append("/>\n");
        }
        // exclusions end ---
      } // for each dependency
        // sb.append(String.format("<!-- %s end -->\n\n",
        // service.getSimpleName()));

      sb.append("\n");

      if (dependencies.size() > 0) {
        ret.append(sb);
      }

    } // for each service
    ret.append("  </dependencies>\n");

    snr.put("{{dependencies}}", ret.toString());

    createFilteredFile(snr, location, "ivy", "xml");
  }

  public void createIvySettings(String location) throws IOException {
    Map<String, String> snr = new HashMap<String, String>();

    StringBuilder sb = new StringBuilder();
    for (RemoteRepo repo : remotes) {
      if (repo.comment != null) {
        sb.append("            <!-- " + repo.comment + "  -->\n");
      }
      sb.append("            <ibiblio name=\"");
      sb.append(repo.id);
      sb.append("\" m2compatible=\"true\" ");
      if (repo.url != null) {
        sb.append("root=\"" + repo.url + "\" ");
      }
      sb.append("/>\n\n");
    }

    snr.put("{{repos}}", sb.toString());

    createFilteredFile(snr, location, "ivysettings", "xml");
  }

  public String[] buidCmdLine(String location) {

    // TODO - noterminate :P
    // String[] cmd = new String[] { "-settings", location +
    // "/ivysettings.xml", "-ivy", location + "/ivy.xml", "-retrieve",
    // location + "/jar" + "/[originalname].[ext]", "-noterminate" };
    // [artifact]-[revision].[ext]
    String[] cmd = new String[] { "-settings", location + "/ivysettings.xml", "-ivy", location + "/ivy.xml", "-retrieve", location + "/jar" + "/[originalname].[ext]" };
    // String[] cmd = new String[] { "-settings", location +
    // "/ivysettings.xml", "-ivy", location + "/ivy.xml", "-retrieve",
    // location + "/jar" + "/[artifact]-[revision].[ext]" };

    StringBuilder sb = new StringBuilder("java -jar ..\\..\\ivy-" + IVY_VERSION + ".jar");
    for (String s : cmd) {
      sb.append(" ");
      sb.append(s);
    }
    log.info("cmd {}", sb);

    return cmd;

  }

  @Override
  public void installDependency(String location, ServiceDependency library) {
    // creating build files
    try {
      location = createWorkDirectory(location);
      createIvySettings(location);
      createIvyForDependency(location, library);

      Platform platform = Platform.getLocalInstance();

      String[] cmd = buidCmdLine(location);

      // TODO: this breaks for me! please review why this needed to be commented
      // out.
      // Ivy ivy = Ivy.newInstance(); <-- for future 2.5.x release
      // ivy.getLoggerEngine().pushLogger(new
      // IvyWrapperLogger(Message.MSG_INFO)); <-- for future 2.5.x release
      // Main.setLogger(new IvyWrapperLogger(Message.MSG_INFO));
      ResolveReport report = Main.run(cmd);

      // if no errors -h
      // mark "service" as installed
      // mark all libraries as installed

      List<?> err = report.getAllProblemMessages();

      boolean error = false;
      if (err.size() > 0) {
        for (int i = 0; i < err.size(); ++i) {
          String errStr = err.get(i).toString();
          if (!errStr.startsWith("WARN:  symlinkmass")) {
            error = true;
          }
          error(errStr);
        }
      }

      if (error) {
        log.error("had errors - repo will not be updated");
        return;
      }

      // TODO - promote to Repo.setInstalled
      // for (ServiceDependency library : targetLibraries) {
      // set as installed & save state
      library.setInstalled(true);
      installedLibraries.put(library.toString(), library);
      info("installed %s platform %s", library, platform.getPlatformId());
      // }

      ArtifactDownloadReport[] artifacts = report.getAllArtifactsReports();
      for (int i = 0; i < artifacts.length; ++i) {
        ArtifactDownloadReport ar = artifacts[i];
        Artifact artifact = ar.getArtifact();
        // String filename = IvyPatternHelper.substitute("[originalname].[ext]",
        // artifact);

        File file = ar.getLocalFile();
        String filename = file.getAbsoluteFile().getAbsolutePath();
        log.info("{}", filename);

        if ("zip".equalsIgnoreCase(artifact.getExt())) {
          info("unzipping %s", filename);
          try {
            Zip.unzip(filename, "./");
            info("unzipped %s", filename);
          } catch (Exception e) {
            log.error(e.getMessage(), e);
          }
        }
      }

      // save repo.json file
      save();
      publishStatus(Status.newInstance(Repo.class.getSimpleName(), StatusLevel.INFO, Repo.INSTALL_FINISHED, String.format("finished install of %s", library)));

    } catch (Exception e) {
      error(e.getMessage());
      log.error(e.getMessage(), e);
    }

  }

  @Override
  synchronized public void install(String location, String[] serviceTypes) {

    try {

      Set<ServiceDependency> targetLibraries = getUnfulfilledDependencies(serviceTypes);

      if (targetLibraries.size() == 0) {
        StringBuilder sb = new StringBuilder();
        for (String type : serviceTypes) {
          if (type.lastIndexOf(".") > 0) {
            sb.append(type.substring(type.lastIndexOf(".") + 1));
          } else {
            sb.append(type);
          }
          sb.append(" ");
        }
        info("%s already installed", sb.toString());
        return;
      }

      publishStatus(Status.newInstance(Repo.class.getSimpleName(), StatusLevel.INFO, Repo.INSTALL_START, String.format("starting install of %s", (Object[]) serviceTypes)));

      log.info("installing {} services into {}", serviceTypes.length, location);

      // create build files - generates appropriate ivy.xml and settings files
      // this service file should be marked as dependencies all others
      // should be marked as provided
      // ??? do "provided" get incorporate in the resolve ?
      createBuildFiles(location, serviceTypes);

      Platform platform = Platform.getLocalInstance();

      // templates [originalname](-[classifier])(-[revision]).[ext] parens are
      // "optional"

      List<String> cmd = new ArrayList<>();
      cmd.add("-settings");
      cmd.add(location + "/ivysettings.xml");
      cmd.add("-ivy");
      cmd.add(location + "/ivy.xml");
      cmd.add("-retrieve");
      cmd.add(location + "/jar" + "/[originalname].[ext]");

      int msgLevel = 1;
      if (log.isWarnEnabled() || log.isErrorEnabled()) {
        msgLevel = Message.MSG_WARN;
        cmd.add("-warn");
      } else {
        msgLevel = Message.MSG_INFO;
      }

      StringBuilder sb = new StringBuilder();
      sb.append("wget https://repo1.maven.org/maven2/org/apache/ivy/ivy/" + IVY_VERSION + "/ivy-" + IVY_VERSION + ".jar\n");
      sb.append("java -jar ivy-" + IVY_VERSION + ".jar");
      for (String s : cmd) {
        sb.append(" ");
        sb.append(s);
      }

      sb.append("\n");

      log.info("cmd {}", sb);
      FileIO.toFile("libraries/install.sh", sb.toString().getBytes());

      Ivy ivy = Ivy.newInstance(); // <-- for future 2.5.x release
      ivy.getLoggerEngine().pushLogger(new IvyWrapperLogger(msgLevel));

      ResolveReport report = null;
      List<String> err = new ArrayList<>();
      try {
        report = Main.run(cmd.toArray(new String[cmd.size()]));
      } catch (Exception e) {
        err.add(e.toString());
      }

      // if no errors -h
      // mark "service" as installed
      // mark all libraries as installed

      if (report != null) {
        List<String> problems = report.getAllProblemMessages();
        for (String problem : problems) {
          if (!problem.startsWith("WARN:  symlinkmass")) {
            err.add(problem);
          }
        }
      }

      if (err.size() > 0) {
        log.error("had errors - repo will not be updated. Errors:\n{}", err);
      } else {

        // TODO - promote to Repo.setInstalled
        for (ServiceDependency library : targetLibraries) {
          // set as installed & save state
          library.setInstalled(true);
          installedLibraries.put(library.toString(), library);
          info("installed %s platform %s", library, platform.getPlatformId());
        }
        save();
      }

      if (report == null) {
        log.error("problems resolving dependencies");
        publishStatus(Status.newInstance(Repo.class.getSimpleName(), StatusLevel.ERROR, Repo.INSTALL_FINISHED,
            String.format("there was problems resolving dependencies %s", (Object[]) serviceTypes)));
      } else {

        ArtifactDownloadReport[] artifacts = report.getAllArtifactsReports();
        for (int i = 0; i < artifacts.length; ++i) {
          ArtifactDownloadReport ar = artifacts[i];
          Artifact artifact = ar.getArtifact();
          // String filename =
          // IvyPatternHelper.substitute("[originalname].[ext]",
          // artifact);

          File file = ar.getLocalFile();
          String filename = file.getAbsoluteFile().getAbsolutePath();
          log.info("{}", filename);

          if ("zip".equalsIgnoreCase(artifact.getExt())) {
            info("unzipping %s", filename);
            try {
              Zip.unzip(filename, "./");
              info("unzipped %s", filename);
            } catch (Exception e) {
              log.error("unable to unzip file {}", filename, e);
            }
          }
        }

        publishStatus(Status.newInstance(Repo.class.getSimpleName(), StatusLevel.INFO, Repo.INSTALL_FINISHED, String.format("finished install of %s", (Object[]) serviceTypes)));
      }
    } catch (Exception e) {
      error(e.getMessage());
      log.error(e.getMessage(), e);
    }

  }

  private void publishStatus(String msg, int level) {
    // if (level <= this.level) {
    Status status = Status.newInstance(Repo.class.getSimpleName(), StatusLevel.INFO, Repo.INSTALL_PROGRESS, msg);
    // FIXME - set it to the instance of IvyWrapper - really this method should
    // just call IvyWrapper.publishStatus(String msg, int
    // level)
    status.source = this;
    if (level == Message.MSG_ERR) {
      status.level = StatusLevel.ERROR;
    } else if (level == Message.MSG_WARN) {
      status.level = StatusLevel.WARN;
    }
    publishStatus(status);

  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      IvyWrapper repo = (IvyWrapper) Repo.getInstance("IvyWrapper");

      String serviceType = "all";
      long ts = System.currentTimeMillis();
      String dir = String.format("install.ivy.%s.update", serviceType);

      String[] types = null;

      types = ServiceData.getLocalInstance().getServiceTypeNames();
      repo.createBuildFiles(dir, types);

      String[] cmd = repo.buidCmdLine(".");

      StringBuilder sb = new StringBuilder();
      sb.append("wget https://repo1.maven.org/maven2/org/apache/ivy/ivy/" + IVY_VERSION + "/ivy-" + IVY_VERSION + ".jar\n");
      sb.append("java -jar ivy-" + IVY_VERSION + ".jar");

      for (String s : cmd) {
        sb.append(" ");
        sb.append(s);
      }

      FileIO.toFile(dir + "/test.sh", sb.toString().getBytes());

      // repo.installTo("install.ivy");
      // repo.install(dir, serviceType);

      // repo.install("install.dl4j.maven", "Deeplearning4j");
      // repo.install("install.opencv.maven","OpenCV");
      // repo.createBuildFiles(dir, "Arm");
      // repo.createBuildFilesTo(dir);
      // repo.installTo(dir);
      // repo.install();
      // repo.installEach(); <-- TODO - test

      log.info("done");

    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

  }

}
