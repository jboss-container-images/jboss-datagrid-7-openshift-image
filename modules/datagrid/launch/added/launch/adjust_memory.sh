#!/bin/sh

FIXED_MEMORY_XMX=200
JVM_NATIVE_MB=220
MAX_RAM=$(expr ${JVM_NATIVE_MB} + ${FIXED_MEMORY_XMX})
JVM_NATIVE_PERCENTAGE_OVERHEAD=1

function prepareEnv() {
   unset EVICTION_TOTAL_MEMORY_B
}

# TODO take into account multiple caches for memory calculation
function configure() {
   NATIVE_MEMORY_PERCENTAGE_OVERHEAD=$(expr ${CONTAINER_MAX_MEMORY} * ${JVM_NATIVE_PERCENTAGE_OVERHEAD} / 100)
   EVICTION_TOTAL_MEMORY_B=$(expr ${CONTAINER_MAX_MEMORY} - ${JVM_NATIVE_MB} * 1024 * 1024 - ${FIXED_MEMORY_XMX} * 1024 * 1024 - ${NATIVE_MEMORY_PERCENTAGE_OVERHEAD})
   if [ "${DEBUG}" == "true" ]; then
      echo "CONTAINER MEM: ${CONTAINER_MAX_MEMORY}"
      echo "EVICTION_TOTAL_MEMORY_B: ${EVICTION_TOTAL_MEMORY_B}"
      echo "NATIVE_MEMORY_PERCENTAGE_OVERHEAD: ${NATIVE_MEMORY_PERCENTAGE_OVERHEAD}"
   fi
   export EVICTION_TOTAL_MEMORY_B

   # Append to standalone.conf provided by cct_modules
   cat ${JBOSS_HOME}/bin/launch/service-memory.conf >> $JBOSS_HOME/bin/standalone.conf
}

function source_java_run_scripts() {
   local java_scripts_dir="/opt/run-java"
   # set CONTAINER_MAX_MEMORY and CONTAINER_CORE_LIMIT
   source "${java_scripts_dir}/container-limits" > /dev/null 2 >&1
   # load java options functions
   source "${java_scripts_dir}/java-default-options" > /dev/null 2 >&1
}

source_java_run_scripts
prepareEnv
configure

# Returns a set of options that are not supported by the current jvm.  The idea
# is that java-default-options always configures settings for the latest jvm.
# That said, it is possible that the configuration won't map to previous
# versions of the jvm.  In those cases, it might be better to have different
# implementations of java-default-options for each version of the jvm (e.g. a
# private implementation that is sourced by java-default-options based on the
# jvm version).  This would allow for the defaults to be tuned for the version
# of the jvm being used.
unsupported_options() {
   echo "(-XX:\+UseParallelOldGC|-XX:\+UnlockExperimentalVMOptions|-XX:\+UseCGroupMemoryLimitForHeap|-XX:\+UseParallelGC|-XX:CICompilerCount=[^ ]*|-XX:GCTimeRatio=[^ ]*|-XX:MaxMetaspaceSize=[^ ]*|-XX:AdaptiveSizePolicyWeight=[^ ]*|-XX:MinHeapFreeRatio=[^ ]*|-XX:MaxHeapFreeRatio=[^ ]*)"
}

additional_options() {
   echo "-Dsun.zip.disableMemoryMapping=true -XX:+UseSerialGC -XX:MinHeapFreeRatio=5 -XX:MaxHeapFreeRatio=10"
}

# Merge default java options into the passed argument
adjust_java_options() {
   local options="$@"
   local remove_xms
   local java_scripts_dir="/opt/run-java"
   local java_options="$("${java_scripts_dir}/java-default-options") $(additional_options)"
   local unsupported="$(unsupported_options)"

   # Off-heap requires a fixed amount of heap memory. The rest is stored off-heap.
   # From our measurements it turned out that 52M is enough.
   java_options=$(echo ${java_options} | sed -e "s/-Xmx[^ ]*/${option}/")
   java_options=$(echo ${java_options} | sed -e "s/-Xms[^ ]*/${option}/")
   java_options="-Xmx${FIXED_MEMORY_XMX}M -Xms${FIXED_MEMORY_XMX}M -XX:MaxRAM=${MAX_RAM}M ${java_options}"

   for option in $java_options; do
      if [[ ${option} == "-Xmx"* ]]; then
         if [[ "$options" == *"-Xmx"* ]]; then
            options=$(echo $options | sed -e "s/-Xmx[^ ]*/${option}/")
         else
            options="${options} ${option}"
         fi
         if [ "x$remove_xms" == "x" ]; then
            remove_xms=1
         fi
      elif [[ ${option} == "-Xms"* ]]; then
         if [[ "$options" == *"-Xms"* ]]; then
            options=$(echo $options | sed -e "s/-Xms[^ ]*/${option}/")
         else
            options="${options} ${option}"
         fi
         remove_xms=0
      elif $(echo "$options" | grep -Eq -- "${option%=*}(=[^ ]*)?(\s|$)"); then
         options=$(echo $options | sed -re "s@${option%=*}(=[^ ]*)?(\s|$)@${option}\2@")
      else
         options="${options} ${option}"
      fi
   done

   if [[ "x$remove_xms" == "x1" ]]; then
      options=$(echo $options | sed -e "s/-Xms[^ ]*/ /")
   fi

   options=$(echo "${options}" | sed -re "s@${unsupported}(\s)?@@g")
   echo "${options}"
}
