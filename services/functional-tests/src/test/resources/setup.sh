#!/bin/bash

# This name is hardcoded in Makefile. We need a fixed name to push it to local OpenShift registry
IMAGE_NAME=${image:-jboss-datagrid-7/datagrid72-openshift}

echo "---- Clearing up (any potential) leftovers ----"
oc delete all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts --selector=template=caching-service || true
oc delete all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts --selector=template=datagrid-service || true
oc delete template caching-service || true
oc delete template datagrid-service || true

echo "---- Creating Caching Service for test ----"
echo "Current dir $PWD"
echo "Using image $IMAGE_NAME"

oc create -f ../caching-service.json

oc process caching-service -p IMAGE=${IMAGE_NAME} -p APPLICATION_USER=test -p APPLICATION_USER_PASSWORD=test | oc create -f -

echo "---- Creating Datagrid Service for test ----"
echo "Current dir $PWD"
echo "Using image $IMAGE_NAME"

oc create -f ../datagrid-service.json

oc process datagrid-service -p IMAGE=${IMAGE_NAME} -p APPLICATION_USER=test -p APPLICATION_USER_PASSWORD=test | oc create -f -
