#!/bin/bash
# Add custom launch script and helpers
# NOTE: this overrides the openshift-launch.sh script in os-eap64-launch
set -e

SCRIPT_DIR=$(dirname $0)
ADDED_DIR=${SCRIPT_DIR}/added

# Add custom launch script and dependent scripts/libraries/snippets
cp -p ${ADDED_DIR}/openshift-launch.sh ${ADDED_DIR}/openshift-migrate.sh ${JBOSS_HOME}/bin/
mkdir -p ${JBOSS_HOME}/bin/launch
cp -r ${ADDED_DIR}/launch/* ${JBOSS_HOME}/bin/launch

# users should migrate to $JBOSS_CONTAINER_MAVEN_DEFAULT_MODULE/maven-repos.sh
ln -s /opt/jboss/container/maven/default/maven-repos.sh ${JBOSS_HOME}/bin/launch/maven-repos.sh
chown -h jboss:root ${JBOSS_HOME}/bin/launch/maven-repos.sh

# Append Prometheus agent to standalone.conf
cat ${ADDED_DIR}/launch/prometheus.conf >> $JBOSS_HOME/bin/standalone.conf
