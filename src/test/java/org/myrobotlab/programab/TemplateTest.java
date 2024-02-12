package org.myrobotlab.programab;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.programab.models.Mrl;
import org.myrobotlab.programab.models.Template;
import org.slf4j.Logger;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class TemplateTest {

  public final static Logger log = LoggerFactory.getLogger(TemplateTest.class);

  @Test
  public void testXmlParsing() {
    try {

      String xml = "<template>XXXX<oob><mrl><service>blah1</service><method>method1</method><param>p1</param><param>p2</param><param>p3</param></mrl><mrl><service>blah2</service><method>method2</method></mrl><mrljson>[\"method\":\"doIt\",\"data\":[\"p1\"]]</mrljson></oob></template>";

      XmlMapper xmlMapper = new XmlMapper();
      Template template = xmlMapper.readValue(xml, Template.class);

      assertNotNull(template);
      assertEquals("XXXX", template.text);

      // Verify Oob parsing
      assertNotNull(template.oob);
      assertEquals(2, template.oob.mrl.size());

      // Verify the first Mrl
      Mrl mrl1 = template.oob.mrl.get(0);
      assertEquals("blah1", mrl1.service);
      assertEquals("method1", mrl1.method);
      assertEquals(3, mrl1.params.size());

      // Verify the second Mrl
      Mrl mrl2 = template.oob.mrl.get(1);
      assertEquals("blah2", mrl2.service);
      assertEquals("method2", mrl2.method);
      assertNull(mrl2.params);

    } catch (Exception e) {
      fail("Exception occurred: " + e.getMessage());
    }
  }
}
