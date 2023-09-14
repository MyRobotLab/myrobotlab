package org.myrobotlab.service;

import java.util.Arrays;
import java.util.List;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.config.DockerConfig;
import org.slf4j.Logger;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

public class Docker extends Service<DockerConfig> {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(Docker.class);

  public Docker(String n, String id) {
    super(n, id);
  }
  
  public static void main(String[] args) {
    try {

      // LoggingFactory.init(Level.INFO);

      Runtime.start("docker", "Docker");
      
      
      DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
//          .withDockerHost("tcp://localhost:2376")
//          .withDockerTlsVerify(true)
//          .withDockerCertPath("/home/user/.docker/certs")
//          .withDockerConfig("/Users/vjay/.docker")
//          .withApiVersion("1.30") // optional
//          .withRegistryUrl("https://index.docker.io/v1/")//填私库地址
//          .withRegistryUsername("username")//填私库用户名
//          .withRegistryPassword("123456")//填私库密码
//          .withRegistryEmail("username@github.com")//填私库注册邮箱
//          .build();
      
      DockerClient dockerClient = DockerClientBuilder.getInstance(config).build();
      
//      DefaultDockerClientConfig.Builder config 
//      = DefaultDockerClientConfig.createDefaultConfigBuilder();
//    DockerClient dockerClient = DockerClientBuilder.getInstance(config)
//      .build();

      // Arrays.asList("running")
      // Arrays.asList("exited")
      List<Container> containers = dockerClient.listContainersCmd()
          .withShowSize(true)
          .withShowAll(true)
          .withStatusFilter(Arrays.asList("exited")).exec();
      
      log.info("containers {}", containers);
      
      for (Container container : containers) {
        // System.out.println(container.toString());
        System.out.println(container.getImage());
      }
      
//      CreateContainerResponse container
//      = dockerClient.createContainerCmd("mongo:3.6")
//        .withCmd("--bind_ip_all")
//        .withName("mongo")
//        .withHostName("baeldung")
//        .withEnv("MONGO_LATEST_VERSION=3.6")
//        .withPortBindings(PortBinding.parse("9999:27017"))
//        .withBinds(Bind.parse("/Users/baeldung/mongo/data/db:/data/db")).exec();
      

      // dockerClient.startContainerCmd(container.getId()).exec();
      dockerClient.startContainerCmd("fc0cbe3ad022").exec();
      
      log.info("here");
      
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }
}
