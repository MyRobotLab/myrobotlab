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

      // this is how a listener might subscribe
      // TODO - put dynamically subscribing into framework
      // with interface inspection ??

      // FIXME - add make the attach !
      http.attach(catcher);

      catcher.clear();
      String data = http.get("https://postman-echo.com/get?foo1=bar1&foo2=bar2");
      log.info(data);
      Message msg = catcher.getMsg(1000);
      assertNotNull("msg from get is null", msg);
      assertTrue("didn't get HttpData", HttpData.class.equals(msg.data[0].getClass()));
      assertTrue("response code != 200", ((HttpData) (msg.data[0])).responseCode == 200);

      // curl --location --request POST "https://postman-echo.com/post" --data
      // "foo1=bar1&foo2=bar2"
      http.postForm("https://postman-echo.com/post", "foo1", "bar1", "foo2", "bar2", "foo3", "bar with spaces");
      log.info(data);

      // TODO - getByteArray(...)
      data = http.get("https://www.cs.tut.fi/~jkorpela/forms/testing.html");
      log.info(data);

      String response = http.post("http://www.cs.tut.fi/cgi-bin/run/~jkorpela/echo.cgi");

      log.info(response);

      response = http.post("http://www.cs.tut.fi/cgi-bin/run/~jkorpela/echo.cgi");
      log.info(response);

      response = http.get("http://www.google.com/search?hl=en&q=myrobotlab&btnG=Google+Search&aq=f&oq=");
      log.info(response);

      // <host>[:port] [passphrase]
      // InstallCert.main(new String[] { "searx.laquadrature.net:443" });

      // http.installCert("https://searx.laquadrature.net");
      // String json = http.get("https://searx.laquadrature.net/?q=cat&format=json");
      // log.info(json);

      http.detach(catcher);
      // FIXME test if successfully detached

      // FIXME - test false isAttached

      // FIXME - implement http.isAttached(this);
    }

  }

}