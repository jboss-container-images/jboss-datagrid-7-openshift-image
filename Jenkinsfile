#!/usr/bin/env groovy

pipeline {
   agent {
      label 'slave-group-normal'
   }

   options {
      timeout(time: 1, unit: 'HOURS')
   }

   stages {
      stage('Prepare') {
         steps {
            // Workaround for JENKINS-47230
            script {
               env.MAVEN_HOME = tool('Maven')

               // See https://github.com/openshift/origin/issues/15038#issuecomment-345252400
               sh 'sudo rm -rf /usr/share/rhel/secrets'
            }

            checkout scm
         }
      }

      stage('Unit and Functional tests') {
         steps {
            script {
               configFileProvider([configFile(fileId: 'maven-settings-with-prod', variable: 'MAVEN_SETTINGS')]) {
                  script {
                     try {
                        sh 'make MVN_COMMAND="$MAVEN_HOME/bin/mvn -s $MAVEN_SETTINGS -Dorg.apache.maven.user-settings=$MAVEN_SETTINGS" test-ci'
                     } finally {
                        sh 'tail -n +1 -- services/functional-tests/target/surefire-reports/*.txt'
                        sh 'make MVN_COMMAND="$MAVEN_HOME/bin/mvn -s $MAVEN_SETTINGS" clean-ci'
                     }
                  }
               }
            }
         }
      }

      stage('Gather test results') {
         steps {
            script {
               junit allowEmptyResults: true, testResults: '**/target/*-reports/*.xml'
            }
         }
      }
   }
}
