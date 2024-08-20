#!/usr/bin/env groovy
pipeline {
    agent any
    tools {
        maven 'maven_nana_3.9.6'
    }
    stages {
        stage('increment version') {
            steps{
                script {
                    echo 'increment app version'
                    sh 'mvn build-helper:parse-version versions:set \
                        -DnewVersion=\\\${parsedVersion.majorVersion}.\\\${parsedVersion.minorVersion}.\\\${parsedVersion.nextIncrementalVersion} \
                        versions:commit'
                    def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
                    def version = matcher[0][1]
                    env.IMAGE_NAME = "$version-$BUILD_NUMBER"
                }
            }
        }
        stage('Build Jar') {
            steps {
                script {
                    echo "building the Jar..."
                    sh 'mvn clean package'
                }
            }
        }
        stage('Build IMAGE') {
            steps {
                script {
                    echo "building the docker Image..."
                    withCredentials([usernamePassword(credentialsId: 'docker_hub_repo', passwordVariable: 'PASS', usernameVariable: 'USER')]) {
                        sh "docker build -t bnnyo/bnnyorepo:${IMAGE_NAME} ."
                        sh "echo $PASS | docker login -u $USER --password-stdin"
                        sh "docker push bnnyo/bnnyorepo:${IMAGE_NAME}"
                    }
                }
            }
        }
        stage('deploy') {
            steps {
                script {
                    echo 'deploying docker image to EC2...'
                }
            }
        } 
    }
}