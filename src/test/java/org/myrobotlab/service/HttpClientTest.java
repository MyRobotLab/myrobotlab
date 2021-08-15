package org.myrobotlab.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.myrobotlab.framework.Message;
import org.myrobotlab.service.data.HttpData;
import org.myrobotlab.test.AbstractTest;

public class HttpClientTest
    extends AbstractTest /* implements HttpDataListener - not useful .. "yet" */ {

  @Test
  public void testService() throws Exception {

    if (hasInternet()) {
      TestCatcher catcher = (TestCatcher) Runtime.start("catcher", "TestCatcher");
      HttpClient http = (HttpClient) Runtime.start("http", "HttpClient");

      catcher.subscribe(http.getName(), "publishHttpData");

      catcher.clear();
      String data = http.get("https://postman-echo.com/get?foo1=bar1&foo2=bar2");
      log.info(data);
      Message msg = catcher.getMsg(1000);
      assertNotNull("msg from get is null", msg);
      assertTrue("didn't get HttpData", HttpData.class.equals(msg.data[0].getClass()));
      assertTrue("response code != 200", ((HttpData) (msg.data[0])).responseCode == 200);

      // curl --location --request POST "https://postman-echo.com/post" --data
      // "foo1=bar1&foo2=bar2"
      String test = http.postForm("https://postman-echo.com/post", "foo1", "bar1", "foo2", "bar2", "foo3", "bar with spaces");
      assertNotNull(test);

      // TODO - getByteArray(...)
      byte[] bytes = http.getBytes("https://www.cs.tut.fi/~jkorpela/forms/testing.html");
      assertNotNull(bytes);

      String response = http.post("http://www.cs.tut.fi/cgi-bin/run/~jkorpela/echo.cgi");
      assertNotNull(response);
      log.info(response);

    }

  }

}