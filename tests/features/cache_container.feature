 @jboss-datagrid-7
 Feature: Openshift JDG cache

  Scenario: jdg cache container eager start
    When container is started with env
       | variable                                          | value                            |
       | CACHE_CONTAINER_START                             | EAGER                            |
    Then XML file /opt/datagrid/standalone/configuration/clustered-openshift.xml should contain value EAGER on XPath //*[local-name()='cache-container']/@start

  Scenario: jdg cache container lazy start
    When container is started with env
       | variable                                          | value                           |
       | CACHE_CONTAINER_START                             | LAZY                            |
    Then XML file /opt/datagrid/standalone/configuration/clustered-openshift.xml should contain value LAZY on XPath //*[local-name()='cache-container']/@start

  Scenario: jdg cache statistics enabled
    When container is started with env
       | variable                                          | value                            |
       | CACHE_CONTAINER_STATISTICS                        | true                             |
    Then XML file /opt/datagrid/standalone/configuration/clustered-openshift.xml should contain value true on XPath //*[local-name()='cache-container']/@statistics

  Scenario: jdg cache statistics disabled
    When container is started with env
       | variable                                          | value                            |
       | CACHE_CONTAINER_STATISTICS                        | false                            |
    Then XML file /opt/datagrid/standalone/configuration/clustered-openshift.xml should contain value false on XPath //*[local-name()='cache-container']/@statistics

  Scenario: jdg cache partition handling enabled
    When container is started with env
       | variable                                          | value                            |
       | CACHE_NAMES                                       | MYAPPCACHE                       |
       | MYAPPCACHE_CACHE_PARTITION_HANDLING_ENABLED       | true                            |
    Then XML file /opt/datagrid/standalone/configuration/clustered-openshift.xml should contain value DENY_READ_WRITES on XPath //*[local-name()='partition-handling']/@when-split
    And container log should contain WARN Deprecated paramater 'MYAPPCACHE_CACHE_PARTITION_HANDLING_ENABLED' since 7.2, please use 'MYAPPCACHE_CACHE_PARTITION_HANDLING_WHEN_SPLIT' and 'MYAPPCACHE_CACHE_PARTITION_MERGE_POLICY' instead
