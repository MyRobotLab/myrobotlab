/**********************************************************************************
 * JenkinsFile for myrobotlab
 *
 * for adjusting build number for specific branch build
 * Jenkins.instance.getItemByFullName("myrobotlab-multibranch/develop").updateNextBuildNumber(185)
 *
 ***********************************************************************************/

pipeline {

    // https://plugins.jenkins.io/agent-server-parameter/
    // agent { label params['agent-name'] } 
    agent any

    parameters {
      // agentParameter name:'agent-name'
      choice(name: 'verify', choices: ['true', 'false'], description: 'verify')
      choice(name: 'javadoc', choices: ['true', 'false'], description: 'build javadocs')
      // choice(choices: ['plan', 'apply -auto-approve', 'destroy -auto-approve'], description: 'terraform command for master branch', name: 'terraform_cmd')
    }

    // echo params.agentName    
    tools {
        maven 'M3' // defined in global tools - maven is one of the only installers that works well for global tool
        // jdk 'openjdk-11-linux' // defined in global tools
        // git 
    }
    
    // JAVA_HOME="${tool 'openjdk-11-linux'}/jdk-11.0.1"
    // JAVA_HOME="/home/jenkins/agent/tools/hudson.model.JDK/openjdk-11-linux/jdk-11.0.1"
    environment {
         MOTD = "you know, for robots !"
        // JDK_HOME = "${tool 'openjdk-11-linux'}/jdk-11.0.1"
        // JAVA_HOME = "${JDK_HOME}"
        // PATH="${env.JAVA_HOME}/bin:${env.PATH}"
    }

    stages {

         // using CleanBeforeCheckout - in configuration
         // stage('clean') {
         //    steps {
         //       cleanWs()
         //    }
         // }

        stage ('initialize') {
            steps {
               print params['agent-name']
               // print System.properties['os.name'].toLowerCase() - access to java object requires permission changes
               script {
                  if (isUnix()) {
                     sh '''
                        echo isUnix true
                        git --version
                        java -version
                        mvn -version

                     '''
                     echo sh(script: 'env|sort', returnStdout: true)
                  } else {
                     bat '''
                        echo isUnix false
                        git --version
                        java -version
                        mvn -version
                        set
                     '''
                  }
               }
            }
        } // stage build

      stage('compile') {
         steps {
            script {
               if (isUnix()) {
                  sh '''
                     mvn -DBUILD_NUMBER=${BUILD_NUMBER} -DskipTests -q clean compile
                  '''
               } else {
                  bat(/"${MAVEN_HOME}\bin\mvn" -DBUILD_NUMBER=${BUILD_NUMBER} -DskipTests -q clean compile  /)
               }
            }
         }
      } // stage compile

      stage('verify') {
         when {
               expression { params.verify == 'true' }
         }
         steps {
            script {
               // TODO - integration tests !
               if (isUnix()) {
                  sh '''
                     mvn -Dfile.encoding=UTF-8 verify --fail-fast
                  '''
               } else {
                  bat '''
                     mvn -Dfile.encoding=UTF-8 verify --fail-fast
                  '''
               }
            }
         }
      } // stage verify

      stage('package') {
         steps {
            script {
               if (isUnix()) {
                  sh '''
                     mvn -DBUILD_NUMBER=${BUILD_NUMBER} -DskipTests -q package
                  '''
               } else {
                  bat(/"${MAVEN_HOME}\bin\mvn" -DBUILD_NUMBER=${BUILD_NUMBER} -DskipTests -q package  /)
               }
            }
         }
      } // stage compile
      
      stage('javadoc') {
         when {
                 // expression { params.javadoc == 'true' }
                 expression { env.BRANCH_NAME == 'master' || env.BRANCH_NAME == 'develop' }
         }
         steps {
            script {
               if (isUnix()) {
                  sh '''
                     mvn -q javadoc:javadoc
                  '''
               } else {
                  bat '''
                     mvn -q javadoc:javadoc
                  '''
               }
            }
         }
      } // stage javadoc

      stage('archive-min') {
         when {
                 expression { env.BRANCH_NAME != 'master' && env.BRANCH_NAME != 'develop' }
         }
         steps {
            archiveArtifacts 'target/myrobotlab.jar, target/surefire-reports/*, target/*.exec'
         }
      }

      stage('archive-javadocs') {
         when {
                 expression { env.BRANCH_NAME == 'master' || env.BRANCH_NAME == 'develop' }
         }
         steps {
            archiveArtifacts 'target/myrobotlab.jar, target/surefire-reports/*, target/*.exec, target/site/**'
         }
      }

      stage('jacoco') {
         steps {
            jacoco(execPattern: 'target/*.exec', classPattern: 'target/classes', sourcePattern: 'src/main/java', exclusionPattern: 'src/test*')
            jacoco()
         }
      }
      
   } // stages 
}