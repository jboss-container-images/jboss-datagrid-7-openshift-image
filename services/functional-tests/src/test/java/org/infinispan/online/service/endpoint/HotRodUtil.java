package org.infinispan.online.service.endpoint;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;
import org.infinispan.protostream.BaseMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.query.remote.client.MarshallerRegistration;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;

import java.io.IOException;
import java.util.function.Function;

import static org.junit.Assert.fail;

public class HotRodUtil {

   private static final Log log = LogFactory.getLog(HotRodTester.class);

   private HotRodUtil() {
   }

   public static LazyRemoteCacheManager lazyRemoteCacheManager() {
      return new AutoCloseableFunctionImpl();
   }

   public static Function<RemoteCacheManager, RemoteCacheManager> initProtoSchema(
      FileDescriptorSource schemas, BaseMarshaller<?>... marshallers
   ) {
      return remote -> {
         // initialize server-side serialization context
         RemoteCache<String, String> metadataCache = remote.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);

         schemas.getFileDescriptors().forEach(
            (name, schema) -> metadataCache.put(name, new String(schema))
         );
         checkSchemaErrors(metadataCache);

         // initialize client-side serialization context
         try {
            SerializationContext serCtx = ProtoStreamMarshaller.getSerializationContext(remote);
            MarshallerRegistration.registerMarshallers(serCtx);
            serCtx.registerProtoFiles(schemas);

            for (BaseMarshaller<?> marshaller : marshallers)
               serCtx.registerMarshaller(marshaller);
         } catch (IOException e) {
            throw new AssertionError(e);
         }

         return remote;
      };
   }

   /**
    * Logs the Protobuf schema errors (if any) and fails the test if there are schema errors.
    */
   private static void checkSchemaErrors(RemoteCache<String, String> metadataCache) {
      if (metadataCache.containsKey(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX)) {
         // The existence of this key indicates there are errors in some files
         String files = metadataCache.get(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX);
         for (String fname : files.split("\n")) {
            String errorKey = fname + ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX;
            log.errorf(
               "Found errors in Protobuf schema file: %s\n%s\n", fname, metadataCache.get(errorKey)
            );
         }

         fail("There are errors in the following Protobuf schema files:\n" + files);
      }
   }

   private static final class AutoCloseableFunctionImpl implements LazyRemoteCacheManager {

      RemoteCacheManager remoteCacheManager;

      @Override
      public RemoteCacheManager apply(ConfigurationBuilder cfg) {
         this.remoteCacheManager = new RemoteCacheManager(cfg.build());
         return this.remoteCacheManager;
      }

      @Override
      public void close() {
         remoteCacheManager.stop();
      }

   }

   public interface LazyRemoteCacheManager extends Function<ConfigurationBuilder, RemoteCacheManager>, AutoCloseable {

      @Override
      void close();

   }

}
