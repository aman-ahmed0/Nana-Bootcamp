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
                    echo 'الحمد لله'
                    def shellCMD = "bash ./server-cmds.sh 'bnnyo/bnnyorepo:${IMAGE_NAME}'"
                    sshagent(['ec2-server-key']) {
                        sh "scp server-cmds.sh ec2-user@18.185.177.136:/home/ec2-user"
                        sh 'scp docker-compose.yaml ec2-user@18.185.177.136:/home/ec2-user'
                        sh "ssh -o StrictHostKeyChecking=no ec2-user@18.185.177.136 ${shellCMD}"
                    }
                }
            }
        }
        stage('commit version update') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'gitlab-banno', passwordVariable: 'PASS', usernameVariable: 'USER')]) {
                        sh 'git config --global user.email "aman@example.com"'
                        sh 'git config --global user.name "aman"'

                        sh 'git status'
                        sh 'git branch'
                        sh 'git config --list'

                        sh "git remote set-url origin https://gitlab-banno:testPass@gitlab.com/gitlab-banno/java-maven-app.git"
                        sh 'git add .'
                        sh 'git commit -m "test stopping commit loop"'
                        sh 'git push origin HEAD:Khad'
                    }
                }
            }
        }
    }
}