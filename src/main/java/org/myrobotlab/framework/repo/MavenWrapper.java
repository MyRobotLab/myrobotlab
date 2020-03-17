package org.myrobotlab.framework.repo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;

// FIXME - 2 layer abstraction - because to generate build files and 
// other critical methods - they do not require actual "ivy" components
// so these methods should be un-hindered by actual maven, gradle or ivy imports !
public class MavenWrapper extends Repo implements Serializable {

  private static final long serialVersionUID = 1L;

  String installDir = "install";

  static String pomXmlTemplate = null;

  static MavenWrapper localInstance = null;

  static public Repo getTypeInstance() {
    if (localInstance == null) {
      init();
    }
    return localInstance;
  }

  static private synchronized void init() {
    if (localInstance == null) {
      /*
       * ClassWorld classWorld = new ClassWorld(); ClassRealm classRealm =
       * (ClassRealm) Thread.currentThread().getContextClassLoader(); mavenCli =
       * new MavenCli(classRealm.getWorld());
       */
      // mavenCli = new MavenCli();
      localInstance = new MavenWrapper();
      pomXmlTemplate = FileIO.resourceToString("framework/pom.xml.template");
    }
  }

  // FIXME - fix do createBuildFiles
  // FIXME - call external mvn since Embedded Maven is not documented and buggy
  @Override
  synchronized public void install(String location, String[] serviceTypes) {

    createBuildFiles(location, serviceTypes);

    log.error("TODO - implement dependency copy with ProcessBuilder & external mvn install");
    boolean done = true;
    if (done) {
      return;
    }

    info("===== starting installation of %d services =====", serviceTypes.length);
    for (String serviceType : serviceTypes) {
      try {
        Set<ServiceDependency> unfulfilled = getUnfulfilledDependencies(serviceType);
        // FIXME - callback logging

        // directory to run localInstance over a default pom
        File f = new File(installDir);
        f.mkdirs();

        for (ServiceDependency library : unfulfilled) {
          // mavenGenerateServiceInstallPom();
          log.info("===== installing dependency {} =====", library);
          String installPom = null;// getArtifactInstallPom(library.getOrgId(),
          // library.getArtifactId(),library.getVersion());
          installPom = installPom.replace("{{location}}", location);

          // search and replace - add
          File installPomDir = new File(location);
          installPomDir.mkdirs();

          FileOutputStream fos = new FileOutputStream(installDir + File.separator + "pom.xml");
          fos.write(installPom.getBytes());
          fos.close();

          // mavenCli.doMain(new String[] { "install" }, "install", System.out,
          // System.out);
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
  }

  public String getRepositories() {

    StringBuilder ret = new StringBuilder("     <repositories>\n");
    for (RemoteRepo repo : remotes) {
      StringBuilder sb = new StringBuilder();
      sb.append("        <!-- " + repo.comment + "  -->\n");
      sb.append("        <repository>\n");
      sb.append("          <id>" + repo.id + "</id>\n");
      sb.append("          <name>" + repo.id + "</name>\n");
      sb.append("          <url>" + repo.url + "</url>\n");
      sb.append("        </repository>\n");
      ret.append(sb);
    }
    ret.append("     </repositories>\n");
    return ret.toString();
  }

  public void createPom(String location, String[] serviceTypes) throws IOException {

    Map<String, String> snr = new HashMap<String, String>();

    StringBuilder deps = new StringBuilder();

    ServiceData sd = ServiceData.getLocalInstance();

    snr.put("{{repositories}}", getRepositories());

    deps.append("<dependencies>\n\n");

    Set<String> listedDeps = new HashSet<String>();
    for (String serviceType : serviceTypes) {
      ServiceType service = sd.getServiceType(serviceType);

      // FIXME - getUnFufilledDependencies ???
      List<ServiceDependency> dependencies = service.getDependencies();

      if (dependencies.size() == 0) {
        continue;
      }

      StringBuilder dep = new StringBuilder();
      dep.append(String.format("<!-- %s begin -->\n", service.getSimpleName()));
      for (ServiceDependency dependency : dependencies) {
        String depKey = dependency.getOrgId() + "-" + dependency.getArtifactId() + "-" + dependency.getVersion();
        if (listedDeps.contains(depKey)) {
          dep.append("<!-- Duplicate entry for " + depKey + " skipping -->\n");
          continue;
        }
        if (dependency.getVersion() == null) {
          dep.append("<!-- skipping " + dependency.getOrgId() + " " + dependency.getArtifactId() + " " + depKey + " null version/latest -->\n");
          continue;          
        }
        
        listedDeps.add(depKey);
        dep.append("  <dependency>\n");
        dep.append(String.format("    <groupId>%s</groupId>\n", dependency.getOrgId()));
        dep.append(String.format("    <artifactId>%s</artifactId>\n", dependency.getArtifactId()));
        // optional - means latest ???
        dep.append(String.format("    <version>%s</version>\n", dependency.getVersion()));
        if (!service.includeServiceInOneJar()) {
          dep.append("    <scope>provided</scope>\n");
        }
        if (dependency.getExt() != null) {
          dep.append(String.format("    <type>%s</type>\n", dependency.getExt()));
        }
        List<ServiceExclude> excludes = dependency.getExcludes();

        // exclusions begin ---
        if (excludes != null & excludes.size() > 0) {
          StringBuilder ex = new StringBuilder();
          ex.append("      <exclusions>\n");
          for (ServiceExclude exclude : excludes) {
            ex.append("        <exclusion>\n");
            ex.append(String.format("          <groupId>%s</groupId>\n", exclude.getOrgId()));
            ex.append(String.format("          <artifactId>%s</artifactId>\n", exclude.getArtifactId()));
            if (exclude.getVersion() != null) {
              ex.append(String.format("          <version>%s</version>\n", exclude.getVersion()));
            }
            if (exclude.getExt() != null) {
              ex.append(String.format("          <type>%s</type>\n", exclude.getExt()));
            }
            ex.append("        </exclusion>\n");
          }
          ex.append("      </exclusions>\n");
          dep.append(ex);
        }

        dep.append("  </dependency>\n");

        // exclusions end ---
      } // for each dependency
      dep.append(String.format("<!-- %s end -->\n\n", service.getSimpleName()));

      if (dependencies.size() > 0) {
        deps.append(dep);
      }

    } // for each service
    deps.append("  </dependencies>\n");

    snr.put("{{dependencies}}", deps.toString());

    createFilteredFile(snr, location, "pom", "xml");
  }

  /**
   * (non-Javadoc)
   * @see org.myrobotlab.framework.repo.Repo#createBuildFiles(java.lang.String, java.lang.String[])
   */
  public void createBuildFiles(String location, String[] serviceTypes) {
    try {

      location = createWorkDirectory(location);
      createPom(location, serviceTypes);

    } catch (Exception e) {
      log.error("could not generate build files", e);
    }
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      Repo repo = Repo.getInstance("MavenWrapper");

      String serviceType = "_TemplateService";

      serviceType = null;

      long ts = System.currentTimeMillis();

      // String dir = String.format("install.maven.%s.%d", serviceType, ts);
      String dir = "install.maven.update";

      repo.createBuildFiles(dir, serviceType);

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

@Override
public void installDependency(String location, ServiceDependency serviceTypes) {
	// TODO Auto-generated method stub
	
}

}
