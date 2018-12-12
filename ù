pipeline {
    agent {
        docker { 
            image 'maven:3.6-jdk-8-alpine' 
            args env.JOB_DOCKER_OPTS
        }
    }
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean package'
            }
        }
         stage('Dist Local') {
            steps {
                sh 'mvn deploy'
            }
        }
    }
}

