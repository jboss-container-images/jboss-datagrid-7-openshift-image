package org.infinispan.online.service.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Authentication;
import org.eclipse.jetty.client.api.AuthenticationStore;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.infinispan.online.service.utils.TrustStore;

import io.fabric8.openshift.client.OpenShiftClient;

public class RESTTester implements EndpointTester {

   private static String DEFAULT_CACHE = "default";

   private TrustStore trustStore;

   public RESTTester(String serviceName, OpenShiftClient client) {
      trustStore = new TrustStore(client, serviceName);
   }

   public void testBasicEndpointCapabilities(URL urlToService) {
      String url = createCacheUrl(urlToService, DEFAULT_CACHE, "should_default_cache_be_accessible_via_REST");
      post(url, "test", 200, true);
      delete(url); // Prevents 409 Conflict between subsequent posts to the same service+cache+key
   }

   public void testCacheAvailability(URL urlToService, String cache, boolean shouldBeAvailable) {
      int expectedCode = shouldBeAvailable ? 200 : 404;
      post(urlToService, cache, "should_cache_be_accessible_via_REST", "test", expectedCode, true);
   }

   public void testIfEndpointIsProtected(URL urlToService) {
      post(urlToService, DEFAULT_CACHE, "isEndpointAuthenticated", "test", 401, false);
   }

   public void putGetRemoveTest(URL urlToService) {
      String url = createCacheUrl(urlToService, DEFAULT_CACHE, "putGetRemoveTest");
      putGetTest(url);
      removeTest(url);
   }

   private void removeTest(String stringUrl) {
      //given
      put(stringUrl, "value");
      //when
      delete(stringUrl);
      //then
      get(stringUrl, 404);
   }

   private void putGetTest(String stringUrl) {
      String value = "value";
      String newValue = "newValue";
      //when
      put(stringUrl, value);
      //then
      String restValue = get(stringUrl, 200);
      assertEquals(value, restValue);

      //when
      put(stringUrl, newValue);
      //then
      restValue = get(stringUrl, 200);
      assertEquals(newValue, restValue);
   }

   private void delete(String url) {
      HttpClient client;
      try {
         client = httpClient(url);
         Request req = client.newRequest(url).method(HttpMethod.DELETE);
         ContentResponse rsp = req.send();
         assertResponseCodeEquals(rsp, 200);
      } catch (Exception e) {
         throw new IllegalStateException(e);
      }
   }

   private String get(String url, int expectedCode) {
      HttpClient client = null;
      try {
         client = httpClient(url);
         Request req = client.newRequest(url).method(HttpMethod.GET);
         ContentResponse rsp = req.send();
         assertResponseCodeEquals(rsp, expectedCode);
         return rsp.getContentAsString();
      } catch (Exception e) {
         throw new IllegalStateException(e);
      } finally {
         stop(client);
      }
   }

   private void post(URL urlServerAddress, String key, String value) {
      post(urlServerAddress, DEFAULT_CACHE, key, value, 200, true);
   }

   private void post(URL urlServerAddress, String cache, String key, String body, int expectedCode, boolean authenticate) {
      String url = createCacheUrl(urlServerAddress, cache, key);
      post(url, body, expectedCode, authenticate);
   }

   private void post(String url, String body, int expectedCode, boolean authenticate) {
      HttpClient client;
      try {
         client = httpClient(url, authenticate);
         Request req = client.POST(url);
         req.header(HttpHeader.ACCEPT, "text/plain");
         req.header(HttpHeader.CONTENT_TYPE, "text/plain");
         req.content(new StringContentProvider(body));
         req.timeout(60, TimeUnit.SECONDS);
         ContentResponse rsp = req.send();
         assertResponseCodeEquals(rsp, expectedCode);
      } catch (Exception e) {
         throw new IllegalStateException(e);
      }
   }

   private void put(String url, String body) {
      HttpClient client;
      try {
         client = httpClient(url);
         Request req = client.newRequest(url).method(HttpMethod.PUT);
         req.header(HttpHeader.ACCEPT, "text/plain");
         req.header(HttpHeader.CONTENT_TYPE, "text/plain");
         req.content(new StringContentProvider(body));
         ContentResponse rsp = req.send();
         assertResponseCodeEquals(rsp, 200);
      } catch (Exception e) {
         throw new IllegalStateException(e);
      }
   }

   private HttpClient httpClient(String url) throws Exception {
      return httpClient(url, true);
   }

   private HttpClient httpClient(String url, boolean authenticate) throws Exception {
      HttpClient httpClient;
      SslContextFactory sslContextFactory = new SslContextFactory();
      sslContextFactory.setTrustStorePath(trustStore.getPath());
      sslContextFactory.setTrustStorePassword(new String(TrustStore.TRUSTSTORE_PASSWORD));
      sslContextFactory.setKeyStorePath(trustStore.getPath());
      sslContextFactory.setKeyStorePassword(new String(TrustStore.TRUSTSTORE_PASSWORD));
      httpClient = new HttpClient(sslContextFactory);
      httpClient.setConnectTimeout(TimeUnit.SECONDS.toMillis(60));
      httpClient.start();

      URI uri = URI.create(url);
      AuthenticationStore authStore = httpClient.getAuthenticationStore();
      if (authenticate) {
         authStore.addAuthenticationResult(new BasicAuthentication.BasicResult(uri, "test", "test"));
      } else {
         Authentication.Result authResult = authStore.findAuthenticationResult(uri);
         if (authResult != null)
            authStore.removeAuthenticationResult(authResult);
      }
      return httpClient;
   }

   private void assertResponseCodeEquals(ContentResponse rsp, int expectedCode) throws Exception {
      assertThat(rsp.getStatus()).isEqualTo(expectedCode).withFailMessage("Rsp: %s", rsp);
   }

   private String createCacheUrl(URL urlServerAddress, String cache, String key) {
      return String.format("%srest/%s/%s", urlServerAddress.toString(), cache, key);
   }

   private void stop(HttpClient client) {
      if (client == null)
         return;

      try {
         client.stop();
      } catch (Exception ignore) {
      }
   }
}
