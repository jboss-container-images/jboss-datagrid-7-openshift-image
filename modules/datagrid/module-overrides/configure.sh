#!/bin/bash
set -e

JBOSS_HOME=/opt/datagrid

SCRIPT_DIR=$(dirname $0)
ADDED_DIR=${SCRIPT_DIR}/added

# Add new "openshift" layer
# includes module definitions for OpenShift PING and OAuth
# (also includes overridden JGroups, AS Clustering Common/JGroups, and EE for OpenShift PING)
# Remove any existing destination files first (which might be symlinks)
cp -rp --remove-destination "$ADDED_DIR/modules" "$JBOSS_HOME/"

# Add files.sh for retrieving module jar filenames
cp -p ${ADDED_DIR}/launch/files.sh $JBOSS_HOME/bin/launch

chown -R jboss:root $JBOSS_HOME
chmod -R g+rwX $JBOSS_HOME
