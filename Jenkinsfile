/**********************************************************************************
 * JenkinsFile for myrobotlab
 *
 * for adjusting build number for specific branch build
 * Jenkins.instance.getItemByFullName("myrobotlab-multibranch/develop").updateNextBuildNumber(185)
 *
 ***********************************************************************************/

pipeline {

    // https://plugins.jenkins.io/agent-server-parameter/
    agent { label params['agent-name'] } 

    parameters {
      agentParameter name:'agent-name'
      choice(choices: ['standard', 'javadoc', 'quick'], description: 'build type', name: 'buildType')
      // choice(choices: ['plan', 'apply -auto-approve', 'destroy -auto-approve'], description: 'terraform command for master branch', name: 'terraform_cmd')
    }

    // echo params.agentName    
    tools { 
        maven 'M3' // defined in global tools
        jdk 'openjdk-11-linux' // defined in global tools
    }
    
    // JAVA_HOME="${tool 'openjdk-11-linux'}/jdk-11.0.1"
    // JAVA_HOME="/home/jenkins/agent/tools/hudson.model.JDK/openjdk-11-linux/jdk-11.0.1"
    environment {
        DB_ENGINE    = 'sqlite'
        JDK_HOME = "${tool 'openjdk-11-linux'}/jdk-11.0.1"
        JAVA_HOME = "${JDK_HOME}"
        PATH="${env.JAVA_HOME}/bin:${env.PATH}"
    }

    stages {
        stage ('initialize') {
            steps {
               print params['agent-name'] 
               script {
                  sh '''
                     # jenkins redefines JAVA_HOME incorrectly - fix here
                     export JAVA_HOME="${JDK_HOME}"

                     mvn -version
                     java -version

                     # create git meta files
                     git rev-parse --abbrev-ref HEAD > GIT_BRANCH
                     git rev-parse HEAD > GIT_COMMIT
                  '''

                  git_commit = readFile('GIT_COMMIT').trim()
                  echo git_commit

                  git_branch = readFile('GIT_BRANCH').trim()
                  echo git_branch

                  echo sh(script: 'env|sort', returnStdout: true)
                }
            }
        } // stage build

      stage('compile') {
         steps {
            script {
               echo git_commit
               echo "git_commit=$git_commit"
               // Run the maven build
               if (isUnix()) {
                  // -o == offline
                  // sh "'${mvnHome}/bin/mvn' -Dbuild.number=${env.BUILD_NUMBER} -Dgit_commit=$git_commit -Dgit_branch=$git_branch -Dmaven.test.failure.ignore -q clean compile "
                  sh '''
                     # jenkins is messing this var up - force it to be correct here
                     export JAVA_HOME=${JDK_HOME}
                     mvn -Dbuild.number=${BUILD_NUMBER} -DskipTests -Dmaven.test.failure.ignore -q clean compile
                  '''
               } else {
                  // bat(/"${mvnHome}\bin\mvn" -Dbuild.number=${env.BUILD_NUMBER} -Dgit_commit=$git_commit -Dgit_branch=$git_branch -Dmaven.test.failure.ignore -q clean compile  /)
                  bat(/"${MAVEN_HOME}\bin\mvn" -Dbuild.number=${env.BUILD_NUMBER} -DskipTests -Dmaven.test.failure.ignore -q clean compile  /)
               }
            }
         }
      } // stage compile

      stage('verify') {
         steps {
            script {
               // TODO - integration tests !
               if (isUnix()) {
                  sh '''
                     # jenkins is messing this var up - force it to be correct here
                     export JAVA_HOME=${JDK_HOME}
                     mvn -Dfile.encoding=UTF-8 verify
                  '''
               } else {
                  bat(/"${mvnHome}\bin\mvn" -Dfile.encoding=UTF-8 verify/)
               }
            }
         }
      } // stage verify

      stage('javadoc') {
         steps {
            script {
                  if (params.buildType == 'javadoc') {
                     if (isUnix()) {
                        sh '''
                           # jenkins is messing this var up - force it to be correct here
                           export JAVA_HOME=${JDK_HOME}
                           mvn -Dfile.encoding=UTF-8 verify
                        '''
                     } else {
                        bat(/"${mvnHome}\bin\mvn" -q javadoc:javadoc/)
                     }
                  }
            }
         }
      } // stage javadoc

      stage('archive') {
         steps {
            // archiveArtifacts 'target/myrobotlab.jar'
            archiveArtifacts 'target/myrobotlab.jar, target/surefire-reports/*, target/*.exec, site/*'
         }
      }
      stage('jacoco') {
         steps {
            jacoco()
         // jacoco(execPattern: 'target/*.exec', classPattern: 'target/classes', sourcePattern: 'src/main/java', exclusionPattern: 'src/test*')
         // jacoco(execPattern: '**/*.exec')
         }
      }
      // TODO - publish
      stage('clean') {
         steps {
            cleanWs()
         }
      }
   } // stages 
}