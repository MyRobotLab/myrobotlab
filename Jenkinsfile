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
      choice(name: 'javadoc', choices: ['true', 'false'], description: 'build javadocs')
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

      stage('compile') {
         steps {
            script {
                  sh '''
                     mvn -Dbuild.number=${BUILD_NUMBER} -DskipTests -q clean compile
                  '''
            }
         }
      } // stage compile

      stage('dependencies') {
         when {
               expression { params.verify == 'true' }
         }
         steps {
            script {
                  sh '''
                     mvn test -Dtest=org.myrobotlab.framework.DependencyTest -q
                  '''
            }
         }
      } // stage dependencies      

      stage('verify') {
         when {
               expression { params.verify == 'true' }
         }
         steps {
            script {
                  sh '''
                     mvn -Dfile.encoding=UTF-8 -DargLine="-Xmx1024m" verify --fail-fast -q
                  '''
            }
         }
      } // stage verify

      stage('package') {
         steps {
            script {
                  sh '''
                     mvn -Dbuild.number=${BUILD_NUMBER} -DskipTests -q package
                  '''
            }
         }
      } // stage package

      stage('javadoc') {
         when {
                 // expression { params.javadoc == 'true' }
                 expression { env.BRANCH_NAME == 'master' || env.BRANCH_NAME == 'develop' }
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
            archiveArtifacts 'target/myrobotlab.jar, target/surefire-reports/**, target/*.exec, target/site/**'
         }
      }

      // stage('archive-javadocs') {
      //    when {
      //       expression { env.BRANCH_NAME == 'master' || env.BRANCH_NAME == 'develop' }
      //    }
      //    steps {
      //       archiveArtifacts 'target/myrobotlab.zip, target/surefire-reports/*, target/*.exec, target/site/**'
      //    }
      // }

      // stage('jacoco') {
      //    steps {
      //       jacoco(execPattern: 'target/*.exec', classPattern: 'target/classes', sourcePattern: 'src/main/java', exclusionPattern: 'src/test*')
      //       jacoco()
      //    }
      // }

      stage('publish-github') {
         // when { expression { env.BRANCH_NAME == 'master' || env.BRANCH_NAME == 'develop' } }
         when { expression { env.BRANCH_NAME == 'master'} }
         steps {
            withCredentials([string(credentialsId: 'supertick-github-token', variable: 'token')]) { // var name "token" is set in cred config and is case senstive
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
            discordSend description: workyNoWorky, footer: '', link: env.BUILD_URL, result: currentBuild.currentResult, title: JOB_NAME, webhookURL: 'https://discord.com/api/webhooks/1015707773260005388/1i6svmKMHYKAFbTXBgen_4CClypYpeqg4WEBMFnc-46Vmf1TNWCxW-ASgDE7mDkkix3u'
         }
      }

      always {
            // Publish JaCoCo coverage report
            jacoco(execPattern: '**/target/jacoco.exec')
      }


  } // post
} // pipeline
