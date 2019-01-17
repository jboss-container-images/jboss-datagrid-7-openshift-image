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
            withCredentials([
                    string(credentialsId: 'krb5.user', variable: 'USER'),
                    file(credentialsId: 'krb5.keytab', variable: 'FILE')
            ]) {
               sh "kinit $USER -k -t $FILE"
            }

            // Workaround for JENKINS-47230
            script {
               env.MAVEN_HOME = tool('Maven')

               sh 'make stop-openshift'
               // See https://github.com/openshift/origin/issues/15038#issuecomment-345252400
               sh 'sudo rm -rf /usr/share/rhel/secrets'

               sh 'make clean-docker clean-maven MVN_COMMAND="$MAVEN_HOME/bin/mvn -s services/functional-tests/maven-settings.xml"'
               sh 'make build-image'
               // Make sure openshift is definitely still not running
               sh 'oc cluster status'
               sh 'make start-openshift-with-catalog login-to-openshift prepare-openshift-project push-image-to-local-openshift'
            }

            checkout scm
         }
      }

      stage('Functional tests') {
         steps {
            script {
                try {
                    sh 'make test-functional MVN_COMMAND="$MAVEN_HOME/bin/mvn -s services/functional-tests/maven-settings.xml"'
                } finally {
                    sh 'tail -n +1 -- services/functional-tests/target/surefire-reports/*.txt'
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
         archiveArtifacts allowEmptyArchive: true, artifacts: '**/target/surefire-reports/*.txt, **/target/surefire-reports/*.log', fingerprint: true
         junit '**/target/surefire-reports/*.xml'
      }

      failure {
         sh 'oc cluster status || true'
         sh 'oc describe pods -n default || true'
         sh 'mkdir logs'
         sh 'sudo docker ps -a --format \'{{.ID}} {{.Image}}\' | grep openshift | awk \'{print $1}\' | xargs -r docker inspect --format=\'{{.Name}} {{.LogPath}}\' |  xargs -l bash -c \'sudo cp $1 logs$0.log\''
         archiveArtifacts allowEmptyArchive: true, artifacts: 'logs/*.log', fingerprint: true
      }

      cleanup {
         sh 'make stop-openshift clean-docker'
         sh 'git clean -qfdx || echo "git clean failed, exit code $?"'
      }
   }
}
