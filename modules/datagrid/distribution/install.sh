#!/bin/bash
set -e

JBOSS_HOME=/opt/datagrid

ARTIFACTS_DIR=/tmp/artifacts
DISTRIBUTION_ZIP="jboss-datagrid-server.zip"

unzip -q $ARTIFACTS_DIR/$DISTRIBUTION_ZIP
mv jboss-datagrid-*-server $JBOSS_HOME

chown -R jboss:root $JBOSS_HOME
chmod -R g+rwX $JBOSS_HOME

# CLOUD-2424 remove redudant .exe files
rm -rf $JBOSS_HOME/docs/contrib/scripts/service

# Enhance standalone.sh to make remote JAVA debugging possible by specifying
# DEBUG=true environment variable
sed -i 's|DEBUG_MODE=false|DEBUG_MODE="${DEBUG:-false}"|' $JBOSS_HOME/bin/standalone.sh
sed -i 's|DEBUG_PORT="8787"|DEBUG_PORT="${DEBUG_PORT:-8787}"|' $JBOSS_HOME/bin/standalone.sh
