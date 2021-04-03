/**********************************************************************************
 * JenkinsFile for myrobotlab
 *
 * for adjusting build number for specific branch build
 * Jenkins.instance.getItemByFullName("myrobotlab-multibranch/develop").updateNextBuildNumber(185)
 *
 ***********************************************************************************/

pipeline {
    agent any

    parameters {
      choice(choices: ['standard', 'javadoc', 'quick'], description: 'build type', name: 'buildType')
      // choice(choices: ['plan', 'apply -auto-approve', 'destroy -auto-approve'], description: 'terraform command for master branch', name: 'terraform_cmd')
    }

    
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
        stage ('Initialize') {
            steps {
               step {
                sh '''
                    export JAVA_HOME="${JDK_HOME}"

                    echo "===================env========================"
                    printenv
                    echo "===================env========================"

                    mvn -version
                    java -version

                    # create git meta files
                    git rev-parse --abbrev-ref HEAD > GIT_BRANCH
                    git rev-parse HEAD > GIT_COMMIT
                '''
               }
               step {
                git_commit = readFile('GIT_COMMIT').trim()
                echo git_commit
               }
               step {
                git_branch = readFile('GIT_BRANCH').trim()
                echo git_branch
               }
            }
             
        }

        stage ('Build') {
            steps {
                echo 'This is a minimal pipeline.'
            }
        }
    }
}