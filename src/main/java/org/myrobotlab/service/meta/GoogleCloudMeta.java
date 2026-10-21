package org.myrobotlab.service.meta;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class GoogleCloudMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(GoogleCloudMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public GoogleCloudMeta() {
    addDescription("google api client service");
    setAvailable(true);
    addDependency("com.google.cloud", "google-cloud-vision", "2.1.3");
    // Pin the revisions Ivy selects. Maven otherwise keeps the older vision transitives.
    addDependency("com.google.guava", "guava", "33.1.0-jre");
    addDependency("com.google.guava", "failureaccess", "1.0.2");
    addDependency("com.google.errorprone", "error_prone_annotations", "2.26.1");
    addDependency("org.checkerframework", "checker-qual", "3.42.0");
    addDependency("com.google.j2objc", "j2objc-annotations", "3.0.0");
    addDependency("com.google.code.gson", "gson", "2.9.1");
    // com.google.auth.Credentials. Declare it directly so Ivy installs the jar
    // even when the vision BOM does not resolve this transitive.
    addDependency("com.google.auth", "google-auth-library-credentials", "1.7.0");
    addCategory("cloud", "vision");
    setCloudService(true);
    setRequiresKeys(true);

  }

}
