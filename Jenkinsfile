pipeline {
    agent any
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
                sh '''
                    echo ${JAVA_HOME}
                    # export JAVA_HOME="/home/jenkins/agent/tools/hudson.model.JDK/openjdk-11-linux/jdk-11.0.1"
                    export JAVA_HOME="${JDK_HOME}"
                    echo "===================env========================"
                    printenv
                    echo "===================env========================"
                    # echo "PATH = ${PATH}"
                    # echo "M2_HOME = ${M2_HOME}"
                    mvn -version
                    java -version
                ''' 
            }
        }

        stage ('Build') {
            steps {
                echo 'This is a minimal pipeline.'
            }
        }
    }
}