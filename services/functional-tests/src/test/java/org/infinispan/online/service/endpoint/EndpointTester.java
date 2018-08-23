package org.infinispan.online.service.endpoint;

import java.net.URL;

public interface EndpointTester {

   void testBasicEndpointCapabilities(URL urlToService);

   void testIfEndpointIsProtected(URL urlToService);
}
