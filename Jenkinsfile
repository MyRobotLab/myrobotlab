/**********************************************************************************
 * JenkinsFile for myrobotlab
 *
 * for adjusting build number for specific branch build
 * Jenkins.instance.getItemByFullName("myrobotlab-multibranch/develop").updateNextBuildNumber(185)
 *
 ***********************************************************************************/

def mvnHome = tool 'M3'


pipeline {

   // properties([buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '3')), [$class: 'GithubProjectProperty', displayName: '', projectUrlStr: 'https://github.com/MyRobotLab/myrobotlab/'], pipelineTriggers([[$class: 'PeriodicFolderTrigger', interval: '2m']])])

   parameters {
      choice(choices: ['standard', 'javadoc', 'quick'], description: 'build type', name: 'buildType')
   // choice(choices: ['plan', 'apply -auto-approve', 'destroy -auto-approve'], description: 'terraform command for master branch', name: 'terraform_cmd')
   }

   stages {
      stage('preparation') { // for display purposes
        steps {
           script {
               // initial clean - remove afte successful build
               cleanWs() // - unless bootstrap is needed - cleanWS should be done at the end of the build

               // Get some code from a GitHub repository
               checkout scm
               // checkout([$class: 'GitSCM', branches: [[name: '*/develop']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/MyRobotLab/myrobotlab.git']]])
               // git 'https://github.com/MyRobotLab/myrobotlab.git'
               // git url: 'https://github.com/MyRobotLab/myrobotlab.git', branch: 'develop'

               sh 'git rev-parse --abbrev-ref HEAD > GIT_BRANCH'
               git_branch = readFile('GIT_BRANCH').trim()
               echo git_branch

               sh 'git rev-parse HEAD > GIT_COMMIT'
               git_commit = readFile('GIT_COMMIT').trim()
               echo git_commit

               // Get the Maven tool.
               // ** NOTE: This 'M3' Maven tool must be configured
               // **       in the global configuration.
               // mvnHome = tool 'M3'

               // env.JAVA_HOME="${tool 'Java8'}"
               // env.PATH="${env.JAVA_HOME}/bin:${env.PATH}"
               sh 'java -version'
               echo sh(script: 'env|sort', returnStdout: true)
            }
         }
      }
      stage('compile') {
         steps {
            script {
               echo git_commit
               echo "git_commit=$git_commit"
               // Run the maven build
               if (isUnix()) {
                  // -o == offline
                  // sh "'${mvnHome}/bin/mvn' -Dbuild.number=${env.BUILD_NUMBER} -Dgit_commit=$git_commit -Dgit_branch=$git_branch -Dmaven.test.failure.ignore -q clean compile "
                  sh "'${mvnHome}/bin/mvn' -Dbuild.number=${env.BUILD_NUMBER} -DskipTests -Dmaven.test.failure.ignore -q clean compile "
               } else {
                  // bat(/"${mvnHome}\bin\mvn" -Dbuild.number=${env.BUILD_NUMBER} -Dgit_commit=$git_commit -Dgit_branch=$git_branch -Dmaven.test.failure.ignore -q clean compile  /)
                  bat(/"${mvnHome}\bin\mvn" -Dbuild.number=${env.BUILD_NUMBER} -DskipTests -Dmaven.test.failure.ignore -q clean compile  /)
               }
            }
         }
      }
      stage('verify') {
         steps {
            script {
               // TODO - integration tests !
               if (isUnix()) {
                  // -o == offline
                  sh "'${mvnHome}/bin/mvn' -Dfile.encoding=UTF-8 verify"
               } else {
                  bat(/"${mvnHome}\bin\mvn" -Dfile.encoding=UTF-8 verify/)
               }
            }
         }
      }
      stage('javadoc') {
         steps {
            script {
                  if (params.environment == 'javadoc') {
                     if (isUnix()) {
                        sh "'${mvnHome}/bin/mvn' -q javadoc:javadoc"
                  } else {
                        bat(/"${mvnHome}\bin\mvn" -q javadoc:javadoc/)
                     }
                  }
            }
         }
      }
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
   }

}