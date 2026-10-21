package org.myrobotlab.service.meta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.io.FilenameUtils;
import org.junit.Test;
import org.myrobotlab.framework.repo.ServiceDependency;
import org.myrobotlab.framework.repo.ServiceExclude;

public class RuntimeMetaTest {

  @Test
  public void declaresMatchingRuntimeLibraries() {
    RuntimeMeta meta = new RuntimeMeta();
    ServiceDependency commonsIo = null;
    ServiceDependency commonsLang3 = null;
    ServiceDependency googleCredentials = null;
    ServiceDependency googleOauth = null;
    ServiceDependency logbackClassic = null;
    ServiceDependency logbackCore = null;
    for (ServiceDependency dependency : meta.getDependencies()) {
      if ("commons-io".equals(dependency.getOrgId()) && "commons-io".equals(dependency.getArtifactId())) {
        commonsIo = dependency;
      }
      if ("org.apache.commons".equals(dependency.getOrgId()) && "commons-lang3".equals(dependency.getArtifactId())) {
        commonsLang3 = dependency;
      }
      if ("com.google.auth".equals(dependency.getOrgId()) && "google-auth-library-credentials".equals(dependency.getArtifactId())) {
        googleCredentials = dependency;
      }
      if ("com.google.auth".equals(dependency.getOrgId()) && "google-auth-library-oauth2-http".equals(dependency.getArtifactId())) {
        googleOauth = dependency;
      }
      if ("ch.qos.logback".equals(dependency.getOrgId()) && "logback-classic".equals(dependency.getArtifactId())) {
        logbackClassic = dependency;
      }
      if ("ch.qos.logback".equals(dependency.getOrgId()) && "logback-core".equals(dependency.getArtifactId())) {
        logbackCore = dependency;
      }
    }

    assertNotNull(commonsIo);
    assertEquals("2.22.0", commonsIo.getVersion());
    assertTrue(commonsIo.getIncludeInOneJar());
    assertNotNull(commonsLang3);
    assertEquals("3.19.0", commonsLang3.getVersion());
    assertTrue(commonsLang3.getIncludeInOneJar());
    assertNotNull(googleCredentials);
    assertEquals("1.7.0", googleCredentials.getVersion());
    assertTrue(googleCredentials.getIncludeInOneJar());
    assertNotNull(googleOauth);
    assertEquals("1.7.0", googleOauth.getVersion());
    assertTrue(googleOauth.getIncludeInOneJar());
    assertTrue(excludes(googleOauth, "com.google.guava", "guava"));
    assertNotNull(logbackClassic);
    assertNotNull(logbackCore);
    assertEquals(logbackClassic.getVersion(), logbackCore.getVersion());
    assertEquals("1.2.13", logbackCore.getVersion());
    assertEquals("py", FilenameUtils.getExtension("gesture.py"));
  }

  @Test
  public void googleServicesInstallTheCredentialsJar() {
    assertCredentialsJar(new GoogleCloudMeta());
    assertCredentialsJar(new GoogleTranslateMeta());
    ServiceDependency oauth = null;
    for (ServiceDependency dependency : new GoogleTranslateMeta().getDependencies()) {
      if ("google-auth-library-oauth2-http".equals(dependency.getArtifactId())) {
        oauth = dependency;
      }
    }
    assertNotNull(oauth);
    assertEquals("1.7.0", oauth.getVersion());
  }

  @Test
  public void serviceMetasUseTheSameCommonsIoRevision() {
    assertCommonsIo(new ProgramABMeta());
    assertCommonsIo(new SolrMeta());
  }

  private static void assertCredentialsJar(org.myrobotlab.service.meta.abstracts.MetaData meta) {
    ServiceDependency credentials = null;
    for (ServiceDependency dependency : meta.getDependencies()) {
      if ("com.google.auth".equals(dependency.getOrgId()) && "google-auth-library-credentials".equals(dependency.getArtifactId())) {
        credentials = dependency;
      }
    }
    assertNotNull(meta.getSimpleName(), credentials);
    assertEquals("1.7.0", credentials.getVersion());
  }

  private static boolean excludes(ServiceDependency dependency, String groupId, String artifactId) {
    for (ServiceExclude exclude : dependency.getExcludes()) {
      if (groupId.equals(exclude.getOrgId()) && artifactId.equals(exclude.getArtifactId())) {
        return true;
      }
    }
    return false;
  }

  private static void assertCommonsIo(org.myrobotlab.service.meta.abstracts.MetaData meta) {
    ServiceDependency commonsIo = null;
    for (ServiceDependency dependency : meta.getDependencies()) {
      if ("commons-io".equals(dependency.getArtifactId())) {
        commonsIo = dependency;
      }
    }
    assertNotNull(meta.getSimpleName(), commonsIo);
    assertEquals("2.22.0", commonsIo.getVersion());
  }

}
