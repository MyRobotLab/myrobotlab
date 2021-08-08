import static org.junit.Assert.assertNotNull;

import javax.net.ssl.SSLException;

import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.nettosphere.Handler;
import org.atmosphere.nettosphere.Nettosphere;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.atmosphere.nettosphere.Config;

public class NettoSphereTest {
  
  protected Nettosphere server;
  protected int port = 8889;
  
  public void startSSL() throws Exception {
    //final SslContext sslClientContext = SslContextBuilder.forClient().build();
    SelfSignedCertificate ssc = new SelfSignedCertificate();
    SslContext sslServer = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
    Config config = new Config.Builder()
            .port(port)
            .host("127.0.0.1")
            .sslContext(sslServer)
            //.initParam(null, null)
            .enabledCipherSuites(sslServer.cipherSuites().toArray(new String[]{}))
            .resource(new Handler() {

                @Override
                public void handle(AtmosphereResource r) {
                    r.getResponse().write("Hello World from Nettosphere").closeStreamOrWriter();
                }
            }).build();

    server = new Nettosphere.Builder().config(config).build();
    assertNotNull(server);
    server.start();
//    server = new Nettosphere.Builder().config(config).build();
//    assertNotNull(server);
//    server.start();
  }
  
public static void main(String[] args) {
  try {
    NettoSphereTest test = new NettoSphereTest();
    test.startSSL();
  } catch(Exception e) {
    e.printStackTrace();
  }

}
}
