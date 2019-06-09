#!/bin/bash
set -e

JBOSS_HOME=/opt/datagrid

ARTIFACTS_DIR=/tmp/artifacts
DISTRIBUTION_ZIP="jboss-datagrid-7.3.2-server.zip"
DATAGRID_VERSION="7.3.2"
DEV_SERVER_NAME='infinispan-server-8.5.*-redhat-SNAPSHOT'
DEV_SERVER_NAME_ZIP="${DEV_SERVER_NAME}-*-bin.zip"
INTERMEDIATE_SERVER_NAME_ZIP='jboss-datagrid-7.3.2.*-server.zip'

# This executes only for dev builds
if [ -f $ARTIFACTS_DIR/$DEV_SERVER_NAME_ZIP ]; then
  mv $ARTIFACTS_DIR/$DEV_SERVER_NAME_ZIP $ARTIFACTS_DIR/$DISTRIBUTION_ZIP
fi

# This executes only for intermediate builds like ERs
if [ -f $ARTIFACTS_DIR/$INTERMEDIATE_SERVER_NAME_ZIP ]; then
  mv $ARTIFACTS_DIR/$INTERMEDIATE_SERVER_NAME_ZIP $ARTIFACTS_DIR/$DISTRIBUTION_ZIP
fi

unzip -q $ARTIFACTS_DIR/$DISTRIBUTION_ZIP

# This also executes only on dev builds
if [ -d $DEV_SERVER_NAME ]; then
  mv $DEV_SERVER_NAME jboss-datagrid-$DATAGRID_VERSION-server
fi
mv jboss-datagrid-$DATAGRID_VERSION-server $JBOSS_HOME

chown -R jboss:root $JBOSS_HOME
chmod -R g+rwX $JBOSS_HOME

# CLOUD-2424 remove redudant .exe files
rm -rf $JBOSS_HOME/docs/contrib/scripts/service

# Enhance standalone.sh to make remote JAVA debugging possible by specifying
# DEBUG=true environment variable
sed -i 's|DEBUG_MODE=false|DEBUG_MODE="${DEBUG:-false}"|' $JBOSS_HOME/bin/standalone.sh
sed -i 's|DEBUG_PORT="8787"|DEBUG_PORT="${DEBUG_PORT:-8787}"|' $JBOSS_HOME/bin/standalone.sh
