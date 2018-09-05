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
                try {
                    sh 'make test-ci MVN_COMMAND="$MAVEN_HOME/bin/mvn -s services/functional-tests/maven-settings.xml" DEV_IMAGE_TAG="$BRANCH_NAME"'
                } finally {
                    sh 'tail -n +1 -- services/functional-tests/target/surefire-reports/*.txt'
                    sh 'make clean-ci MVN_COMMAND="$MAVEN_HOME/bin/mvn -s services/functional-tests/maven-settings.xml" DEV_IMAGE_TAG="$BRANCH_NAME"'
                }
            }
         }
      }

      stage('Gather test results') {
         steps {
            script {
               junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
            }
         }
      }
   }

   post {
      always {
         archiveArtifacts artifacts: '**/target/surefire-reports/*.txt, **/target/surefire-reports/*.log', fingerprint: true
         junit '**/target/surefire-reports/*.xml'
      }
   }
}
