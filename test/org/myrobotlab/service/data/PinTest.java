package org.myrobotlab.service.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class PinTest {

  @Test
  public void testPin() {
    Pin p = new Pin();
    assertNotNull(p);

    Pin p2 = new Pin(1, 2, 3, "foo");

    assertEquals(1, p2.pin);
    assertEquals(2, p2.type);
    assertEquals(3, p2.value);
    assertEquals("foo", p2.source);

    Pin p3 = new Pin(p2);

    p2.pin = 4;
    assertEquals(p3.pin, 1);

  }

}
