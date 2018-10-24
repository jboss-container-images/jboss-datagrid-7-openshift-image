package org.infinispan.online.service.utils;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;

public class XmlUtils {

   public static String prettyXml(String input) {
      return prettyFormat(input, 2);
   }

   private static String prettyFormat(String input, int indent) {
      try {
         Source xmlInput = new StreamSource(new StringReader(input));
         StringWriter stringWriter = new StringWriter();
         StreamResult xmlOutput = new StreamResult(stringWriter);
         TransformerFactory transformerFactory = TransformerFactory.newInstance();
         Transformer transformer = transformerFactory.newTransformer();
         transformer.setOutputProperty(OutputKeys.INDENT, "yes");
         transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
         transformer.transform(xmlInput, xmlOutput);
         return xmlOutput.getWriter().toString();
      } catch (Exception e) {
         throw new RuntimeException(e); // simple exception handling, please review it
      }
   }

}
