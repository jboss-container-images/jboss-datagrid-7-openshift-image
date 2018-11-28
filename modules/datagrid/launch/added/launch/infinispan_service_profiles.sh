#!/bin/sh

KEYSTORE_SECRET_DIR=${KEYSTORE_SECRET_DIR:-/var/run/secrets/java.io/keystores}
SERVICE_ACCOUNT_DIR=${SERVICE_ACCOUNT_DIR:-/var/run/secrets/openshift.io/serviceaccount}

function createKeystoresFromSecrets() {
   local keystorePassword=${SERVICE_NAME}
   local keystore="${KEYSTORE_SECRET_DIR}/${SERVICE_NAME}.p12"
   local keystorePkcs="${KEYSTORE_SECRET_DIR}/keystore.pkcs12"

   rm -f ${keystore} ${keystorePkcs}

   openssl pkcs12 -export -inkey "${SERVICE_ACCOUNT_DIR}/tls.key" -in "${SERVICE_ACCOUNT_DIR}/tls.crt" \
 -out "${keystorePkcs}" -password pass:${keystorePassword}

   # Only execute keytool if openssl executed successfully
   if [ $? -eq 0 ]; then
      keytool -importkeystore -noprompt -srckeystore ${keystorePkcs} -srcstoretype pkcs12 -srcstorepass ${SERVICE_NAME} \
 -destkeystore "${keystore}" -deststoretype pkcs12 -storepass ${SERVICE_NAME}
   fi

   export HTTPS_KEYSTORE="${SERVICE_NAME}.p12"
   export HTTPS_KEYSTORE_DIR=${KEYSTORE_SECRET_DIR}
   export HTTPS_NAME="TODO NOT USED REMOVE FROM infinispan-config.sh"
   export HTTPS_PASSWORD=${SERVICE_NAME}
   export SASL_SERVER_NAME=${SERVICE_NAME}
}

# Called automatically as script is executed in openshift-common.sh as part of CONFIGURE_SCRIPTS
function configure() {
   # If service profile exists, then configure profile
   if [ -n ${SERVICE_PROFILE} ]; then

      # Perform profile specific configuration
      if [ "${SERVICE_PROFILE}" == "cache-service" ]; then
         SERVICE_NAME=${SERVICE_NAME:-cache-service}

         createKeystoresFromSecrets

         # Endpoints
         export INFINISPAN_CONNECTORS="hotrod,rest"
         export HOTROD_AUTHENTICATION="TRUE"
         export HOTROD_ENCRYPTION="TRUE"
         export REST_SECURITY_DOMAIN="ApplicationRealm"

         local evictionStrategyType="REMOVE"
         if [ "${EVICTION_POLICY}" == "reject" ]; then
           evictionStrategyType="EXCEPTION";
           export DEFAULT_TRANSACTION_MODE="NON_DURABLE_XA";
         fi

         # Infinispan
         export DEFAULT_CACHE_OWNERS=${REPLICATION_FACTOR}
         export DEFAULT_CACHE_PARTITION_HANDLING_WHEN_SPLIT="DENY_READ_WRITES"
         export DEFAULT_CACHE_PARTITION_HANDLING_MERGE_POLICY="REMOVE_ALL"
         export DEFAULT_CACHE_MEMORY_STORAGE_TYPE="off-heap"
         export DEFAULT_CACHE_MEMORY_EVICTION_TYPE="MEMORY"
         export DEFAULT_CACHE_MEMORY_EVICTION_STRATEGY=${evictionStrategyType}

         # EVICTION_TOTAL_MEMORY_B exported by adjust_memory.sh
         source ${JBOSS_HOME}/bin/launch/adjust_memory.sh
         export DEFAULT_CACHE_MEMORY_EVICTION_SIZE=${EVICTION_TOTAL_MEMORY_B}
      elif [ "${SERVICE_PROFILE}" == "datagrid-service" ]; then
         SERVICE_NAME=${SERVICE_NAME:-datagrid-service}

         createKeystoresFromSecrets

         # Endpoints
         export INFINISPAN_CONNECTORS="hotrod,rest"
         export HOTROD_AUTHENTICATION="TRUE"
         export HOTROD_ENCRYPTION="TRUE"
         export REST_SECURITY_DOMAIN="ApplicationRealm"

         # Infinispan
         export DEFAULT_CACHE_OWNERS="2"
         export DEFAULT_CACHE_PARTITION_HANDLING_WHEN_SPLIT="DENY_READ_WRITES"
         export DEFAULT_CACHE_PARTITION_HANDLING_MERGE_POLICY="REMOVE_ALL"
         export DEFAULT_CACHE_MEMORY_STORAGE_TYPE="off-heap"
      fi
   fi
}