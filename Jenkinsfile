/**********************************************************************************
 * JenkinsFile for myrobotlab
 *
 * for adjusting build number for specific branch build
 * Jenkins.instance.getItemByFullName("myrobotlab-multibranch/develop").updateNextBuildNumber(185)
 *
 ***********************************************************************************/

def mvnHome = tool 'M3'


pipeline {

   agent any

   // properties([buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '3')), [$class: 'GithubProjectProperty', displayName: '', projectUrlStr: 'https://github.com/MyRobotLab/myrobotlab/'], pipelineTriggers([[$class: 'PeriodicFolderTrigger', interval: '2m']])])

   tools { 
      maven 'M3' // defined in global tools
      jdk 'openjdk-11-linux' // defined in global tools
   }

   // JAVA_HOME="${tool 'openjdk-11-linux'}/jdk-11.0.1"
   // JAVA_HOME="/home/jenkins/agent/tools/hudson.model.JDK/openjdk-11-linux/jdk-11.0.1"
   environment {
      JDK_HOME = "${tool 'openjdk-11-linux'}/jdk-11.0.1"
      JAVA_HOME = "${JDK_HOME}"
      PATH="${JAVA_HOME}/bin:${env.PATH}"
   }


   parameters {
      choice(choices: ['standard', 'javadoc', 'quick'], description: 'build type', name: 'buildType')
   // choice(choices: ['plan', 'apply -auto-approve', 'destroy -auto-approve'], description: 'terraform command for master branch', name: 'terraform_cmd')
   }

   stages {
      stage('preparation') { // for display purposes
        steps {
           script {
               sh 'echo "==================printenv=================="'
               sh 'printenv'
               sh 'echo "==================printenv=================="'

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
  }

}