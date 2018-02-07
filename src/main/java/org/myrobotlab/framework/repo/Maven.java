package org.myrobotlab.framework.repo;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.cli.MavenCli;
import org.myrobotlab.framework.ServiceReservation;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

// https://stackoverflow.com/questions/1631802/set-the-logger-for-a-localInstance-embedder-execution

public class Maven extends Repo {

  public final static Logger log = LoggerFactory.getLogger(Maven.class);

  // transient static Maven localInstance = null;

  String installDir = "install";

  transient static MavenCli mavenCli = null;

  static String buildPomHeader = null;
  static String buildPomFooter = null;
  static String repositories = null;

  static String installServicePomTemplate = null;

  static public Repo getInstance() {
    if (localInstance == null) {
      init();
    }
    return localInstance;
  }

  static private synchronized void init() {
    if (localInstance == null) {
      mavenCli = new MavenCli();
      localInstance = new Maven();
      buildPomHeader = FileIO.resourceToString("framework/buildPomHeader.xml");
      buildPomFooter = FileIO.resourceToString("framework/buildPomFooter.xml");
      repositories = FileIO.resourceToString("framework/repositories.xml");
      installServicePomTemplate = FileIO.resourceToString("framework/installServicePomTemplate.xml");
    }
    
    //FIXME - deserialize the Libraries repo/cache
    /*
    try {
      
      // DID NOT WORK
      // repoImplClass =  Class.forName(repoManagerClassName );
      
      String data = FileIO.toString(REPO_STATE_FILE_NAME);
      // localInstance = (Repo)CodecUtils.fromJson(data, repoImplClass);
      localInstance = (Repo)CodecUtils.fromJson(data, Maven.class);
      if (localInstance == null) {
        throw new IOException(String.format("%s empty", REPO_STATE_FILE_NAME));
      }
    } catch (Exception e) {
      log.info("{} file not found", REPO_STATE_FILE_NAME);
      // default we are using Maven now .. not Ivy
      // localInstance = new Ivy();
      localInstance = Maven.getLocalInstance();
    } */
    
    
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.myrobotlab.framework.repo.Repo#installService(java.lang.String,
   * java.lang.String)
   */
  @Override
  synchronized public void install(String location, String serviceType) {
    info("===== starting installation of %s =====", serviceType);
    try {
      Set<ServiceDependency> unfulfilled = getUnfulfilledDependencies(serviceType);
      // FIXME - callback logging

      // directory to run localInstance over a default pom
      File f = new File(installDir);
      f.mkdirs();

      for (ServiceDependency library : unfulfilled) {
        // mavenGenerateServiceInstallPom();
        log.info(String.format("===== installing dependency %s =====", library));
        String installPom = getArtifactInstallPom(library.getOrgId(), library.getArtifactId(), library.getVersion());
        installPom = installPom.replace("{{location}}", location);

        // search and replace - add
        File installPomDir = new File(location);
        installPomDir.mkdirs();

        FileOutputStream fos = new FileOutputStream(installDir + File.separator + "pom.xml");
        fos.write(installPom.getBytes());
        fos.close();

        mavenCli.doMain(new String[] { "install" }, "install", System.out, System.out);
      }
    } catch (Exception e) {
      // FIXME - add to errors !!!
      info("===== ERROR in installation of %s =====", serviceType);
      log.error("installServiceDir threw", e);
      // FIXME - callback error
    }
    info("===== finished installation of %s =====", serviceType);

    // save updated repo/library state
    save();
  }

  public String getArtifactInstallPom(String orgId, String artifactId, String version) {

    // all
    String installPom = installServicePomTemplate.replace("{{repositories}}", repositories);
    installPom = installPom.replace("{{orgId}}", orgId);
    installPom = installPom.replace("{{artifactId}}", artifactId);
    installPom = installPom.replace("{{version}}", version);
    installPom = installPom.replace("{{scope}}", "runtime");

    // replace parts of template
    return installPom;
  }

  public Set<ServiceDependency> getUnfulfilledDependencies(String type) {
    if (!type.contains(".")) {
      type = String.format("org.myrobotlab.service.%s", type);
    }
    HashSet<ServiceDependency> ret = new LinkedHashSet<ServiceDependency>();

    // get the dependencies required by the type
    ServiceData sd = ServiceData.getLocalInstance();
    if (!sd.containsServiceType(type)) {
      log.error(String.format("%s not found", type));
      return ret;
    }

    ServiceType st = sd.getServiceType(type);

    // look through our repo and resolve
    // if we dont have it - we need it
    List<ServiceDependency> metaDependencies = st.getDependencies();

    if (metaDependencies != null && metaDependencies.size() > 0) {
      for (ServiceDependency library : metaDependencies) {
        String key = library.getKey();
        if (!libraries.containsKey(key) || !libraries.get(key).isInstalled()) {
          ret.add(library);
        }
      }
    }

    Map<String, ServiceReservation> peers = st.getPeers();
    if (peers != null) {
      for (String key : peers.keySet()) {
        ServiceReservation sr = peers.get(key);
        ret.addAll(getUnfulfilledDependencies(sr.fullTypeName));
      }
    }

    return ret;
  }

  /*
   * public void executeMavenInstall() { MavenCli localInstance = new MavenCli();
   * localInstance.doMain(new String[] { "dependency:copy-dependencies" },
   * "path/to/project/root", System.out, System.out); }
   */

  // TODO generate full pom - use header & footer templates
  // FIXME - generate Template which is build "runtime" and "real runtime" with
  // selected "provided" scopes
  public String generateBuildPomFromMetaData() {
    StringBuilder sb = new StringBuilder();
    sb.append(buildPomHeader);
    sb.append(repositories);
    sb.append(generatePomDependenciesMetaData(null));
    sb.append(buildPomFooter);
    return sb.toString();
  }

  public void saveBuildPom() {
    try {
      String filename = String.format("pom.%d.xml", System.currentTimeMillis());
      FileOutputStream fos = new FileOutputStream(filename);
      fos.write(generateBuildPomFromMetaData().getBytes());
      fos.close();
    } catch (Exception e) {
      log.error("saveBuildPom threw", e);
    }
  }

  public String generatePomDependenciesMetaData(String serviceType) {
    StringBuilder ret = new StringBuilder();

    ret.append("<dependencies>\n\n");
    ServiceData sd = ServiceData.getLocalInstance();
    List<ServiceType> services = new ArrayList<ServiceType>();
    if (serviceType != null) {
      services.add(sd.getServiceType(serviceType));
    } else {
      services = sd.getServiceTypes();
    }

    for (ServiceType service : services) {
      StringBuilder sb = new StringBuilder();
      sb.append(String.format("<!-- %s begin -->\n", service.getSimpleName()));
      List<ServiceDependency> dependencies = service.getDependencies();
      for (ServiceDependency dependency : dependencies) {
        sb.append("<dependency>\n");
        sb.append(String.format("  <groupId>%s</groupId>\n", dependency.getOrgId()));
        sb.append(String.format("  <artifactId>%s</artifactId>\n", dependency.getArtifactId()));
        // optional - means latest ???
        sb.append(String.format("  <version>%s</version>\n", dependency.getVersion()));
        if (!service.includeServiceInOneJar()) {
          sb.append("  <scope>provided</scope>\n");
        }
        if (dependency.getExt() != null) {
          sb.append(String.format("  <type>%s</type>\n", dependency.getExt()));
        }
        List<ServiceExclude> excludes = dependency.getExcludes();

        // exclusions begin ---
        StringBuilder ex = new StringBuilder("    <exclusions>\n");
        for (ServiceExclude exclude : excludes) {
          ex.append("      <exclusion>\n");
          ex.append(String.format("        <groupId>%s</groupId>\n", exclude.getOrgId()));
          ex.append(String.format("        <artifactId>%s</artifactId>\n", exclude.getArtifactId()));
          if (exclude.getVersion() != null) {
            ex.append(String.format("        <version>%s</version>\n", exclude.getVersion()));
          }
          if (exclude.getExt() != null) {
            ex.append(String.format("        <type>%s</type>\n", exclude.getExt()));
          }
          ex.append("      </exclusion>\n");
        }
        ex.append("    </exclusions>\n");
        if (excludes.size() > 0) {
          sb.append(ex);
        }
        // exclusions end ---

        sb.append("</dependency>\n");
      }
      sb.append(String.format("<!-- %s end -->\n\n", service.getSimpleName()));
      if (dependencies.size() > 0) {
        ret.append(sb);
      }
    }

    ret.append("</dependencies>\n");
    try {
      FileOutputStream fos = new FileOutputStream("dependencies.xml");
      fos.write(ret.toString().getBytes());
      fos.close();
    } catch (Exception e) {
      log.error("writing dependencies.xml threw", e);
    }
    return ret.toString();
  }

  public static void main(String[] args) {

    try {

      Maven localInstance = (Maven)Maven.getInstance();
      localInstance.saveBuildPom();
      localInstance.generateBuildPomFromMetaData();
      localInstance.install("Joystick");
      // localInstance.installService("OpenCV", "OpenCV");

      log.info("here");

    } catch (Exception e) {
      log.error("main threw", e);
    }

  }
}
