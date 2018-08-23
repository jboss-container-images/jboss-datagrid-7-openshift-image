package org.infinispan.online.service.utils;

import java.io.File;

import org.jboss.shrinkwrap.resolver.api.maven.Maven;

public class DeploymentHelper {

   public static File[] testLibs() {
      return Maven.resolver().loadPomFromFile("pom.xml")
            .resolve("org.infinispan:infinispan-client-hotrod",
                  "io.fabric8:openshift-client",
                  "org.assertj:assertj-core",
                  "org.eclipse.jetty:jetty-client")
            .withTransitivity().asFile();
   }
}
