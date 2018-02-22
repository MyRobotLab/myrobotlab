package org.myrobotlab.framework.repo;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.ivy.ant.IvyConvertPom;
import org.myrobotlab.framework.ServiceType;

public class Ivy extends Repo {

	static Ivy localInstance = null;

	static public Repo getTypeInstance() {
		if (localInstance == null) {
			init();
		}
		return localInstance;
	}

	static private synchronized void init() {
		if (localInstance == null) {

			// mavenCli = new MavenCli();
			localInstance = new Ivy();
			// buildPomHeader = FileIO.resourceToString("framework/buildPomHeader.xml");
			// buildPomFooter = FileIO.resourceToString("framework/buildPomFooter.xml");
			// repositories = FileIO.resourceToString("framework/repositories.xml");
			// installServicePomTemplate =
			// FileIO.resourceToString("framework/installServicePomTemplate.xml");
		}
		// FIXME - deserialize the Libraries repo/cache
	}

	@Override
	public void install(String location, String serviceType) {
		// TODO Auto-generated method stub

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

	// TODO generate full pom - use header & footer templates
	// FIXME - generate Template which is build "runtime" and "real runtime" with
	// selected "provided" scopes
	public String generateBuildPomFromMetaData() {
		StringBuilder sb = new StringBuilder();
		// sb.append(buildPomHeader);
		// sb.append(repositories);
		sb.append(generatePomDependenciesMetaData(null));
		// sb.append(buildPomFooter);
		return sb.toString();
	}
	
	public void convertPom() {
		convertPom("./pom.xml", String.format("./ivy.%d.xml", System.currentTimeMillis()));		
	}

	public void convertPom(String pomFile, String ivyFile) {
		org.apache.ivy.Ivy ivy = new org.apache.ivy.Ivy();
		IvyConvertPom convert = new IvyConvertPom();
		/*
		Project project = new Project();
		
        Location expectedLocation = new Location("./");
        String expectedDescription = "ivy";
        
        
		convert.setProject(project);
		convert.setPomFile(new File(pomFile));
		convert.setIvyFile(new File(ivyFile));
		convert.doExecute();
		*/
	}

	public static void main(String[] args) {
		try {

			

	        // use an anonymous subclass since ProjectComponent is abstract  
			Ivy localInstance = (Ivy) Repo.getInstance();
			// localInstance.saveBuildPom();
			// localInstance.generateBuildPomFromMetaData();
			// localInstance.install("Joystick");
			// localInstance.installService("OpenCV", "OpenCV");

			// FIXME - refactor to something more general
			// eg. generateBuildFiles
			// localInstance.generateBuildFiles();
			localInstance.convertPom();

			log.info("here");

		} catch (Exception e) {
			log.error("main threw", e);
		}

	}

	// generate build file
	public void generateBuildFiles() {
		// TODO Auto-generated method stub

	}

}
