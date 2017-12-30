package org.myrobotlab.programab;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;

public class OOBPayloadTest {

  @Test
  public void basicTestOOBPayload() {
    // test the getters/setters of oob object.
    OOBPayload payload = new OOBPayload();
    payload.setMethodName("foo");
    ArrayList<String> params = new ArrayList<String>();
    payload.setParams(params);
    payload.setServiceName("fooservice");

    assertEquals(payload.getMethodName(), "foo");
    assertEquals(payload.getParams(), params);
    assertEquals(payload.getServiceName(), "fooservice");

    OOBPayload pl2 = new OOBPayload("boo2", "fooo2", params);
    assertEquals(pl2.getMethodName(), "fooo2");
    assertEquals(pl2.getParams(), params);
    assertEquals(pl2.getServiceName(), "boo2");

  }

}
