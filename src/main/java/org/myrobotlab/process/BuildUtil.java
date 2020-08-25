package org.myrobotlab.process;

import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(mixinStandardHelpOptions = true, version = "Build utilities - 1.0")
public class BuildUtil implements Runnable {
  
  public final static Logger log = LoggerFactory.getLogger(BuildUtil.class);
  
  public static void getManifest() {
  }
  

  @Override
  public void run() {
    // TODO Auto-generated method stub
    System.out.println("================= RUN !!!!! =================");
  }
  
  /**
   * based on time prefixVersion 1.1.${build.number}
   * @return
   */
  public int getLocalBuildNumber() {
    return (int)System.currentTimeMillis()/1000;
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);

    try {
      
      // check for maven
      
      // if not installed - install
      
      // "standardized" meta version extraction
      
      // "standard" local archive/myrobotlab-{branch}-{version}.jar
      // "standard" local archive/myrobotlab-develop-1.1.993.jar
      
      // "standard" local build numbering ...
      
      String branch = "develop";
      String version = null;// latest

      // http://build.myrobotlab.org:8080/job/myrobotlab/job/develop/lastSuccessfulBuild/artifact/target/myrobotlab.jar
      BuildUtil builder = new BuildUtil();
      // String url = String.format(LATEST_BUILD_URL, branch);
      // what version ????
      // /archive/target/version.xml /txt/json/manifest ?
      
      
      new CommandLine(builder).parseArgs(new String[] {});
      
      // download jar from branch
      System.out.println("================= AHOY !!!!! =================");
      
      
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }


}
