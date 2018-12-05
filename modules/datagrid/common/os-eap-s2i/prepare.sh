#!/bin/bash
# Add s2i scripts and make the image S2I-enabled
set -e

SCRIPT_DIR=$(dirname $0)
ADDED_DIR=${SCRIPT_DIR}/added

chown -R jboss:root $SCRIPT_DIR
chmod -R g+rwX $SCRIPT_DIR

cp -r ${ADDED_DIR}/s2i /usr/local
chmod ug+x /usr/local/s2i/*
