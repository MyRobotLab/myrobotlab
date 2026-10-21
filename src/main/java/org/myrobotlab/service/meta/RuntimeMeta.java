package org.myrobotlab.service.meta;

import org.myrobotlab.framework.repo.IvyWrapper;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class RuntimeMeta extends MetaData {
  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(RuntimeMeta.class);

  /**
   * This class is contains all the meta data details of a service. It's peers,
   * dependencies, and all other meta data related to the service.
   */
  public RuntimeMeta() {

    addDescription("is a singleton service responsible for the creation, starting, stopping, releasing and registration of all other services");
    addCategory("framework");

    // 1.2.13 is the newest logback that works with slf4j 1.7.
    // classic and core must be the same revision.
    addDependency("ch.qos.logback", "logback-classic", "1.2.13");
    addDependency("ch.qos.logback", "logback-core", "1.2.13");

    includeServiceInOneJar(true);

    // for proxy generation
    addDependency("net.bytebuddy", "byte-buddy", "1.12.16");

    // apache 2.0 license
    addDependency("com.fasterxml.jackson.core", "jackson-core", "2.17.1");
    addDependency("com.fasterxml.jackson.core", "jackson-annotations", "2.17.1");
    addDependency("com.fasterxml.jackson.core", "jackson-databind", "2.17.1");
    addDependency("com.fasterxml.jackson.module", "jackson-module-no-ctor-deser", "2.17.1");
    // apache 2.0 license
    // addDependency("org.apache.ivy", "ivy", "2.4.0-5");
    addDependency("org.apache.ivy", "ivy", IvyWrapper.IVY_VERSION);

    // apache 2.0 license - REMOVE in favor of okhttp
    // FIXME - replace apache with okhttp
    addDependency("org.apache.httpcomponents", "httpclient", "4.5.14");
    // Pin the revisions Ivy selects so Maven does not keep httpclient's older transitives.
    addDependency("commons-codec", "commons-codec", "1.19.0");
    addDependency("org.apache.httpcomponents", "httpcore", "4.4.16");

    // apache 2.0 license
    addDependency("info.picocli", "picocli", "4.4.0");
    // all your logging needs
    addDependency("org.slf4j", "slf4j-api", "1.7.36");

    // for config file support.
    addDependency("org.yaml", "snakeyaml", "1.32");

    // ws best client websockets with Apache license
    addDependency("com.squareup.okhttp3", "okhttp", "4.10.0");
    // okhttp 4.10 brings kotlin-stdlib 1.6.20 and stdlib-jdk8 1.5.31.
    // Pin common to 1.6.20 so Maven does not keep the older jdk8 transitive.
    addDependency("org.jetbrains.kotlin", "kotlin-stdlib-common", "1.6.20");

    // force correct version of netty - needed for Vertx but not for Runtime ?
    addDependency("io.netty", "netty-all", "4.1.82.Final");

    // FilenameUtils is used by InMoov2 and AudioFile.
    addDependency("commons-io", "commons-io", "2.22.0");

    // StringUtils is used by framework code (MethodCache) and by HtmlFilter.
    addDependency("org.apache.commons", "commons-lang3", "3.19.0");

    // Service-type scans Class.forName every service, including GoogleTranslate,
    // which links com.google.auth.Credentials and GoogleCredentials.
    // Keep the oauth2 jar's heavy transitives out of the one-jar; Ivy still
    // resolves them when GoogleTranslate or GoogleCloud is installed.
    addDependency("com.google.auth", "google-auth-library-credentials", "1.7.0");
    addDependency("com.google.auth", "google-auth-library-oauth2-http", "1.7.0");
    exclude("com.google.guava", "guava");
    exclude("com.google.http-client", "google-http-client");
    exclude("com.google.http-client", "google-http-client-gson");
    exclude("com.google.auto.value", "auto-value-annotations");
    exclude("com.google.code.findbugs", "jsr305");

  }

}
