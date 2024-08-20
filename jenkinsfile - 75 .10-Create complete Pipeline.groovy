#!/usr/bin/env groovy
pipeline {
    agent any
    tools {
        maven 'maven_nana_3.9.6'
    }
    stages {

        stage('BuildJar') {
            steps {
                script {
                    echo "building the Jar..."
                    sh 'mvn package'
                }
            }
        }
        
        stage('BuildIMAGE') {
            steps {
                script {
                    echo "building the docker Image..."
                    withCredentials([usernamePassword(credentialsId: 'docker_hub_repo', passwordVariable: 'PASS', usernameVariable: 'USER')]) {
                        sh 'docker build -t bnnyo/bnnyorepo:aman7.14 .'
                        sh "echo $PASS | docker login -u $USER --password-stdin"
                        sh 'docker push bnnyo/bnnyorepo:aman7.14'
                    }
                }
            }
        }
    }
}