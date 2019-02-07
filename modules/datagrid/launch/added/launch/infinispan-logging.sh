function prepareEnv() {
  unset loggers
  unset LOGGING_CATEGORIES
}


function configure() {
  if [ -n "$LOGGING_CATEGORIES" ]; then
      IFS=',' read -a loggerlist <<< "$(find_env "LOGGING_CATEGORIES")"
      if [ "${#loggerlist[@]}" -ne "0" ]; then
        loggercount=0
        while [ $loggercount -lt ${#loggerlist[@]} ]; do
          logger="${loggerlist[$loggercount]}"

          category=${logger%=*}
          level=${logger#*=}
          loggers+="\n            <logger category=\"$category\"><level name=\"$level\"/></logger>"
          loggercount=$((loggercount+1))
        done
      fi
  else
    # then use default loggers
    loggers+="\n            <logger category=\"com.arjuna\"><level name=\"WARN\"/></logger>"	 
    loggers+="\n            <logger category=\"sun.rmi\"><level name=\"WARN\"/></logger>" 
    loggers+="\n            <logger category=\"org.jboss.as.config\"><level name=\"DEBUG\"/></logger>" 
  fi
  sed -i "s|<!-- ##LOGGER_CONFIGURATION## -->|$loggers|" "$CONFIG_FILE"
}

