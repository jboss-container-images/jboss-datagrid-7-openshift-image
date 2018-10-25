package org.infinispan.online.service.endpoint;

import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.openshift.client.OpenShiftClient;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.SaslQop;
import org.infinispan.online.service.utils.OpenShiftClientCreator;
import org.infinispan.online.service.utils.TrustStore;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Function;

public class HotRodConfiguration {

   private static final OpenShiftClient OPENSHIFT_CLIENT = OpenShiftClientCreator.getClient();
   private static final String HTTP_PROTOCOL = "https";

   private HotRodConfiguration() {
   }

   public static Function<String, ConfigurationBuilder> secured() {
      return svcName ->
         encrypted()
            .andThen(withAuthentication(svcName))
            .apply(svcName);
   }

   public static Function<String, ConfigurationBuilder> encrypted() {
      return svcName -> {
         final ConfigurationBuilder config = new ConfigurationBuilder();

         final URL url = getServiceWithName(svcName + "-hotrod");
         TrustStore trustStore = new TrustStore(svcName);

         config
            .addServer()
               .host(url.getHost())
               .port(url.getPort())
            .security().ssl()
               .enable()
               .trustStoreFileName(trustStore.getPath())
               .trustStorePassword(TrustStore.TRUSTSTORE_PASSWORD);

         return config;
      };
   }

   private static Function<ConfigurationBuilder, ConfigurationBuilder> withAuthentication(String svcName) {
      return config -> {
         config.security().authentication()
            .enable()
            .username("test")
            .password("test")
            .realm("ApplicationRealm")
            .saslMechanism("DIGEST-MD5")
            .saslQop(SaslQop.AUTH)
            .serverName(svcName);

         return config;
      };
   }

   private static URL getServiceWithName(String resourceName) {
      ServiceSpec spec = OPENSHIFT_CLIENT.services().withName(resourceName).get().getSpec();

      if (spec != null) {
         String host;
         if (spec.getExternalName() != null && !spec.getExternalName().isEmpty()) {
            host = spec.getExternalName();
         } else {
            host = spec.getClusterIP();
         }
         //use HTTP as the protocol is unimportant and we don't want to register custom handlers
         return newUrl(spec, host);
      }
      return null;
   }

   private static URL newUrl(ServiceSpec spec, String host) {
      try {
         return new URL(HTTP_PROTOCOL, host, spec.getPorts().get(0).getPort(), "/");
      } catch (MalformedURLException e) {
         throw new AssertionError(e);
      }
   }

}
