package org.myrobotlab.framework.repo;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ivy.Main;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.util.filter.Filter;
import org.apache.ivy.util.filter.NoFilter;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.io.FileIO;
import org.myrobotlab.io.Zip;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;

// FIXME - 2 layer abstraction - because to generate build files and 
// other critical methods - they do not require actual "ivy" components
// so these methods should be un-hindered by actual maven, gradle or ivy imports !
public class IvyWrapper extends Repo {

  static IvyWrapper localInstance = null;

  public static final Filter NO_FILTER = NoFilter.INSTANCE;

  static String ivyXmlTemplate = null;
  static String ivysettingsXmlTemplate = null;

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

  @Override
  public void install(String location, String[] serviceTypes) {

    try {

      Set<ServiceDependency> libraries = getUnfulfilledDependencies(serviceTypes);

      log.info("installing {} services into {}", serviceTypes.length, location);

      // create build files - generates appropriate ivy.xml and settings files
      // this service file should be marked as dependencies all others
      // should be marked as provided
      // ??? do "provided" get incorporate in the resolve ?
      createBuildFiles(location, serviceTypes);

      Platform platform = Platform.getLocalInstance();

      String[] cmd = new String[] { "-settings", location + "/ivysettings.xml", "-ivy", location + "/ivy.xml", "-retrieve", location + "/[originalname].[ext]" };

      StringBuilder sb = new StringBuilder("java -jar ..\\..\\ivy-2.4.0-2.jar");
      for (String s : cmd) {
        sb.append(" ");
        sb.append(s);
      }
      log.info("cmd {}", sb);
      ResolveReport report = Main.run(cmd);

      // if no errors -
      // mark "service" as installed
      // mark all libraries as installed

      List<?> err = report.getAllProblemMessages();

      if (err.size() > 0) {
        for (int i = 0; i < err.size(); ++i) {
          String errStr = err.get(i).toString();
          error(errStr);
        }
        return;
      }

      for (ServiceDependency library : libraries) {
        // set as installed & save state
        info("installed %s platform %s", library, platform.getPlatformId());
        library.setInstalled(true);
      }
      save();

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

    } catch (Exception e) {
      error(e.getMessage());
      log.error(e.getMessage(), e);
    }

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

  public void createIvy(String location, String[] serviceTypes) throws IOException {
    Map<String, String> snr = new HashMap<String, String>();
    StringBuilder sb = null;

    StringBuilder ret = new StringBuilder();
    ServiceData sd = ServiceData.getLocalInstance();

    ret.append("  <dependencies>\n\n");

    for (String serviceType : serviceTypes) {
      ServiceType service = sd.getServiceType(serviceType);
      List<ServiceDependency> dependencies = service.getDependencies();

      if (dependencies.size() == 0) {
        continue;
      }

      sb = new StringBuilder();
      sb.append(String.format("  <!-- %s -->\n", service.getSimpleName()));

      for (ServiceDependency dependency : dependencies) {
        sb.append("  <dependency"); // conf="provided->master"
        sb.append(String.format(" org=\"%s\" name=\"%s\" rev=\"%s\"", dependency.getOrgId(), dependency.getArtifactId(), dependency.getVersion()));
        // FIXME -
        // https://stackoverflow.com/questions/37840659/ivy-dependecy-as-provided
        if (!service.includeServiceInOneJar()) {
          // sb.append(" conf=\"provided->master\" ");
        }

        // <dependency /> or <dependency></dependency> - ie do we have more
        // stuff ?
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

  /**
   * <pre>
   * generates all ivy related build files based on Service.getMetaData
   * 
   * ivy.xml
   * ivysettings.xml 
   * build.xml
   * 
   * </pre>
   * 
   * @return
   */
  public void createBuildFiles(String location, String[] serviceTypes) {
    try {

      location = createWorkDirectory(location);
      createIvySettings(location);
      createIvy(location, serviceTypes);

    } catch (Exception e) {
      log.error("could not generate build files", e);
    }
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);
      // use an anonymous subclass since ProjectComponent is abstract
      Repo ivy = Repo.getInstance("IvyWrapper");

      // String dir = String.format("build.ivy.%d", System.currentTimeMillis());
      String dir = String.format("install.ivy.%d", System.currentTimeMillis());

      ivy.installTo("install.ivy");
      // ivy.install("install.opencv.ivy", "OpenCV");
      // ivy.install("install.artoolkitplus.ivy", "_TemplateService");
      
      boolean done = true;
      if (done) {
        return;
      }

      

      ivy.install(dir, "Joystick");

      // ivy.createBuildFiles(dir, "Arm");
      // ivy.createBuildFilesTo(dir);
      // ivy.install("Joystick");
      // ivy.install();
      // ivy.installEach();

      log.info("here");

    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

  }

}
