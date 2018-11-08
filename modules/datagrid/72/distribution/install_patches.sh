#!/bin/sh

set -e

ARTIFACTS_DIR=/tmp/artifacts
# Add patches to the array below and they will be applied in order
PATCHES=(
  "jboss-datagrid-7.2.3-server-patch.zip"
)

# Usage: apply_patches <patches array> <artifacts dir>
function apply_patches() {
  patches_to_apply=("$@")

  # bash will consider all parameters to be part of the array
  # The last element in the array will be the artifacts dir; extract it
  ((last_index=${#patches_to_apply[@]} - 1))
  patches_dir=${patches_to_apply[last_index]}
  unset patches_to_apply[last_index]

  for each_patch in "${patches_to_apply[@]}"; do
    $JBOSS_HOME/bin/cli.sh -Dorg.wildfly.patching.jar.invalidation=true --command="patch apply $patches_dir/$each_patch"
  done
}

function aggregate_patched_modules {
  export JBOSS_PIDFILE=/tmp/jboss.pid
  cp -r $JBOSS_HOME/standalone /tmp/

  $JBOSS_HOME/bin/standalone.sh --admin-only -Djboss.server.base.dir=/tmp/standalone &

  start=$(date +%s)
  end=$((start + 120))
  until $JBOSS_HOME/bin/cli.sh --command="connect" || [ $(date +%s) -ge "$end" ]; do
    sleep 5
  done

  timeout 30 $JBOSS_HOME/bin/cli.sh --connect --command="/core-service=patching:ageout-history"
  timeout 30 $JBOSS_HOME/bin/cli.sh --connect --command="shutdown"

  # Give it a moment to settle
  for i in $(seq 1 10); do
    test -e "$JBOSS_PIDFILE" || break
    sleep 1
  done

  # Data Grid still running? Something is not right...
  if test -e "$JBOSS_PIDFILE"; then
    echo "Data Grid instance still running; aborting" >&2
    exit 1
  fi

  rm -rf /tmp/standalone
}

function remove_scrapped_jars {
  find $JBOSS_HOME -name \*.jar.patched -printf "%h\n" | sort | uniq | xargs rm -rv
}

function update_permissions {
  chown -R jboss:root $JBOSS_HOME
  chmod -R g+rwX $JBOSS_HOME
}

# Temporarily unset JBOSS_MODULES_SYSTEM_PKGS to prevent java.lang.ClassNotFoundException: org.jboss.logmanager.LogManager
JBOSS_MODULES=$JBOSS_MODULES_SYSTEM_PKGS
unset JBOSS_MODULES_SYSTEM_PKGS

apply_patches "${PATCHES[@]}" "$ARTIFACTS_DIR"
aggregate_patched_modules
remove_scrapped_jars
update_permissions

export JBOSS_MODULES_SYSTEM_PKGS=$JBOSS_MODULES
