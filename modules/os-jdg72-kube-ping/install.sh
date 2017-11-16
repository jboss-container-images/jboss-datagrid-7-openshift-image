#!/bin/sh

set -e

SCRIPT_DIR=$(dirname $0)
ADDED_DIR=${SCRIPT_DIR}/added

# Since KUBE_PING is now provided by the server distribution, we need to switch layer ordering
# (in other words, favor those jars that are provided by our modules (not CE).
# See https://issues.jboss.org/browse/ISPN-8452 for more details.
echo "layers=base,openshift" > /opt/datagrid/modules/layers.conf

# The next step - replace the configuration name
sed -i -e 's/openshift.KUBE_PING/kubernetes.KUBE_PING/g' /opt/datagrid/standalone/configuration/clustered-openshift.xml

# Finally, use new variable names
echo 'export KUBERNETES_NAMESPACE=${OPENSHIFT_KUBE_PING_NAMESPACE}' >> /opt/datagrid/bin/launch/configure.sh
echo 'export KUBERNETES_LABELS=${OPENSHIFT_KUBE_PING_LABELS}' >> /opt/datagrid/bin/launch/configure.sh