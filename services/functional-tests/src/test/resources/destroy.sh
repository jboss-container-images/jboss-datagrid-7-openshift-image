#!/bin/bash

echo "---- Printing out test resources ----"
oc get all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts

echo "---- Describe Pods ----"
oc describe pods

echo "---- Docker PS ----"
docker ps

echo "---- Caching Service logs ----"
oc logs caching-service-0

echo "---- Test Runner logs ----"
oc logs testrunner
oc logs testrunner -c pem-to-truststore

echo "---- EAP Testrunner logs  ----"
oc logs testrunner

echo "---- Clearing up test resources ---"
oc delete all,secrets,sa,templates,configmaps,daemonsets,clusterroles,rolebindings,serviceaccounts --selector=template=caching-service || true
oc delete template caching-service || true
oc delete service testrunner || true
oc delete route testrunner || true


