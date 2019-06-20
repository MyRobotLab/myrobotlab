properties([buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '3')), [$class: 'GithubProjectProperty', displayName: '', projectUrlStr: 'https://github.com/MyRobotLab/myrobotlab/'], pipelineTriggers([[$class: 'PeriodicFolderTrigger', interval: '2m']])])

node { // use any node

// node ('ubuntu') {  // use labels to direct build

   // withEnv(javaEnv) {
   
   def mvnHome
   stage('preparation') { // for display purposes
      // Get some code from a GitHub repository
      checkout scm
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
      mvnHome = tool 'M3'
      
      env.JAVA_HOME="${tool 'Java8'}"
      env.PATH="${env.JAVA_HOME}/bin:${env.PATH}"
      sh 'java -version'
      echo sh(script: 'env|sort', returnStdout: true)
   }
   stage('compile') {
      echo git_commit
      echo "git_commit=$git_commit"
      // Run the maven build
      if (isUnix()) {
         sh "'${mvnHome}/bin/mvn' -Dgit_commit=$git_commit -Dgit_branch=$git_branch -Dmaven.test.failure.ignore -q clean compile -Dbuild.number=${env.BUILD_NUMBER}"
      } else {
         bat(/"${mvnHome}\bin\mvn" -Dgit_commit=$git_commit -Dgit_branch=$git_branch -Dmaven.test.failure.ignore -q clean compile -Dbuild.number=${env.BUILD_NUMBER} /)
      }
   }
   stage('verify'){
	   if (isUnix()) {
	     sh "'${mvnHome}/bin/mvn' verify"
	   } else {
	     bat(/"${mvnHome}\bin\mvn" verify/)
	   }
   }
   stage('javadoc'){
	   if (isUnix()) {
	     sh "'${mvnHome}/bin/mvn' -q javadoc:javadoc"
	   } else {
	     bat(/"${mvnHome}\bin\mvn" -q javadoc:javadoc/)
	   }
   }
   stage('results') {
      junit '**/target/surefire-reports/TEST-*.xml'
      archiveArtifacts 'target/*.jar'      
      jacoco(execPattern: 'target/*.exec',classPattern: 'target/classes',sourcePattern: 'src/main/java',exclusionPattern: 'src/test*')
   } 
   stage('publish') {
   
//    	def server = Artifactory.server 'artifactory01' 
//    	def uploadSpec = """{
// 								"files": [
//										    {
//										      "pattern": "target/myrobotlab.jar",
//										      "target": "org/myrobotlab/"
//										    }
//										 ]
//										}"""
//		server.upload(uploadSpec)

	}
}
