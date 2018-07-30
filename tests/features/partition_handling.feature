 @jboss-datagrid-7/datagrid72-openshift
 Feature: Openshift JDG cache

  Scenario: JDG cache partition handling enabled
    When container is started with env
       | variable                                          | value                            |
       | CACHE_NAMES                                       | MYAPPCACHE                       |
       | MYAPPCACHE_CACHE_PARTITION_HANDLING_ENABLED       | true                             |
    Then XML file /opt/datagrid/standalone/configuration/clustered-openshift.xml should contain value DENY_READ_WRITES on XPath //*[local-name()='partition-handling']/@when-split

  Scenario: JDG cache partition handling enabled and when-split set
    When container is started with env
       | variable                                          | value                            |
       | CACHE_NAMES                                       | MYAPPCACHE                       |
       | MYAPPCACHE_CACHE_PARTITION_HANDLING_ENABLED       | true                             |
       | MYAPPCACHE_CACHE_PARTITION_HANDLING_WHEN_SPLIT    | ALLOW_READ_WRITES                 |
    Then XML file /opt/datagrid/standalone/configuration/clustered-openshift.xml should contain value ALLOW_READ_WRITES on XPath //*[local-name()='partition-handling']/@when-split

  Scenario: JDC cache partition handling correct configuration
    When container is started with env
       | variable                                          | value                            |
       | CACHE_NAMES                                       | MYAPPCACHE                       |
       | MYAPPCACHE_CACHE_PARTITION_HANDLING_WHEN_SPLIT    | DENY_READ_WRITES                 |
       | MYAPPCACHE_CACHE_PARTITION_HANDLING_MERGE_POLICY  | REMOVE_ALL                       |
    Then XML file /opt/datagrid/standalone/configuration/clustered-openshift.xml should contain value DENY_READ_WRITES on XPath //*[local-name()='partition-handling']/@when-split
    Then XML file /opt/datagrid/standalone/configuration/clustered-openshift.xml should contain value REMOVE_ALL on XPath //*[local-name()='partition-handling']/@merge-policy
