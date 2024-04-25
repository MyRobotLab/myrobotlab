/**********************************************************************************
 * JenkinsFile for myrobotlab
 *
 * for adjusting build number for specific branch build
 * Jenkins.instance.getItemByFullName("myrobotlab-multibranch/develop").updateNextBuildNumber(185)
 * Cancel all jobs - Jenkins.instance.queue.clear()
 ***********************************************************************************/
 
pipeline {
   // https://plugins.jenkins.io/agent-server-parameter/
   // agent { label params['agent-name'] }
   agent any

   parameters {
      // agentParameter name:'agent-name'
      choice(name: 'verify', choices: ['true', 'false'], description: 'verify')
      choice(name: 'javadoc', choices: ['false', 'true'], description: 'build javadocs')
      choice(name: 'githubPublish', choices: ['true', 'false'], description: 'publish to github')
   // choice(choices: ['plan', 'apply -auto-approve', 'destroy -auto-approve'], description: 'terraform command for master branch', name: 'terraform_cmd')
   }

   // echo params.agentName
   tools {
      maven 'M3' // defined in global tools - maven is one of the only installers that works well for global tool
   // jdk 'openjdk-11-linux' // defined in global tools
   }

   // JAVA_HOME="${tool 'openjdk-11-linux'}/jdk-11.0.1"
   // JAVA_HOME="/home/jenkins/agent/tools/hudson.model.JDK/openjdk-11-linux/jdk-11.0.1"
   environment {
         MOTD = 'you know, for robots !'
         VERSION_PREFIX = "1.1"
         VERSION = "${VERSION_PREFIX}" + ".${BUILD_NUMBER}"
         // MAVEN_OPTS = '-Xmx4096m -XX:MaxPermSize=256m -XX:+ExitOnOutOfMemoryError'
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

      stage('initialize') {
            steps {
               echo "VERSION_PREFIX ${VERSION_PREFIX}"
               echo "VERSION ${VERSION}"
               echo "BUILD_NUMBER ${BUILD_NUMBER}"

               print params['agent-name']
               // print System.properties['os.name'].toLowerCase() - access to java object requires permission changes
               script {
                  sh '''
                        git --version
                        java -version
                        mvn -version

                     '''
                  }
               }
        } // stage build

   
      // stage('dependencies') {
      //    when {
      //          expression { params.verify == 'true' }
      //    }
      //    steps {
      //       script {
      //             sh '''
      //                mvn test -Dtest=org.myrobotlab.framework.DependencyTest -q
      //             '''
      //       }
      //    }
      // } // stage dependencies      

      // --fail-fast
      // -DargLine="-Xmx1024m"
      stage('maven package') {
         steps {
            script {
                  sh '''
                     mvn -Dfile.encoding=UTF-8 -Dversion=${VERSION} clean package jacoco:report -q
                  '''
            }
         }
      } // stage package

      stage('javadoc') {
         when {
                 expression { env.BRANCH_NAME == 'master' || env.BRANCH_NAME == 'develop' || params.javadoc == 'true' }
         }
         steps {
                  sh '''
                     mvn -q javadoc:javadoc
                  '''
            }
      } // stage javadoc

      stage('archive') {
         // when {
         //    expression { env.BRANCH_NAME != 'master' && env.BRANCH_NAME != 'develop' }
         // }
         steps {
            archiveArtifacts 'target/**'
         }
      }

      stage('publish-github') {
         when { expression { env.BRANCH_NAME == 'master' || env.BRANCH_NAME == 'develop' } }
         steps {
            withCredentials([string(credentialsId: 'publish_token', variable: 'token')]) { // var name "token" is set in cred config and is case senstive
               echo "publishing ${VERSION_PREFIX}.${BUILD_NUMBER}"
               echo "version ${VERSION}"
               // for security - your supposed to make it non-interpretive single quotes and let the OS process the interpolation
               sh './publish-github.sh -v ${VERSION} -b ${BUILD_NUMBER} -t ${token}'
            }
         }
      }
   } // stages

   post {

      changed {  // success | aborted | unsuccessful
         script {
            echo 'build result is : ' + currentBuild.result
            workyNoWorky = 'noWorky !'
            if (currentBuild.result == 'SUCCESS'){
               workyNoWorky = 'Worky !'
            }

            // Fetch the Discord webhook URL from the secure store
            withCredentials([string(credentialsId: 'jenkins-discord-webhook', variable: 'DISCORD_WEBHOOK_URL')]) {
                // Use the secure webhook URL for sending the message
                discordSend description: workyNoWorky, footer: '', link: env.BUILD_URL, result: currentBuild.currentResult, title: JOB_NAME, webhookURL: DISCORD_WEBHOOK_URL
            }            
         }
      }

      always {
            // publish junit
            junit 'target/surefire-reports/**/*.xml'
            // Publish JaCoCo coverage report
            jacoco(execPattern: '**/target/jacoco.exec')
      }

  } // post
} // pipeline
