
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;

import org.apache.log4j.Logger;

import io.netty.handler.ssl.SslHandler;

public class SSLHandlerProvider {
  private static final Logger logger = Logger.getLogger(SSLHandlerProvider.class);

  private static final String PROTOCOL = "TLS";
  private static final String ALGORITHM_SUN_X509 = "SunX509";
  private static final String ALGORITHM = "ssl.KeyManagerFactory.algorithm";
  private static final String KEYSTOREx = "ssl_certs/mysslstore.jks";
  private static final String KEYSTORE_TYPE = "JKS";
  private static final String KEYSTORE_PASSWORD = "changeit";
  private static final String CERT_PASSWORD = "changeit";
  private static SSLContext serverSSLContext = null;

  public static SslHandler getSSLHandler() {
    SSLEngine sslEngine = null;
    if (serverSSLContext == null) {
      logger.error("Server SSL context is null");
      System.exit(-1);
    } else {
      sslEngine = serverSSLContext.createSSLEngine();
      sslEngine.setUseClientMode(false);
      sslEngine.setNeedClientAuth(false);

    }
    return new SslHandler(sslEngine);
  }

  public static void initSSLContext() {

    logger.info("Initiating SSL context");
    String algorithm = Security.getProperty(ALGORITHM);
    if (algorithm == null) {
      algorithm = ALGORITHM_SUN_X509;
    }
    KeyStore ks = null;
    InputStream inputStream = null;
    try {
      inputStream = new FileInputStream("src/main/resources/resource/Security/keys/store.jks");
      ks = KeyStore.getInstance(KEYSTORE_TYPE);
      ks.load(inputStream, KEYSTORE_PASSWORD.toCharArray());
    } catch (IOException e) {
      logger.error("Cannot load the keystore file", e);
    } catch (CertificateException e) {
      logger.error("Cannot get the certificate", e);
    } catch (NoSuchAlgorithmException e) {
      logger.error("Somthing wrong with the SSL algorithm", e);
    } catch (KeyStoreException e) {
      logger.error("Cannot initialize keystore", e);
    } finally {
      try {
        inputStream.close();
      } catch (IOException e) {
        logger.error("Cannot close keystore file stream ", e);
      }
    }
    try {

      // Set up key manager factory to use our key store
      KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
      kmf.init(ks, CERT_PASSWORD.toCharArray());
      KeyManager[] keyManagers = kmf.getKeyManagers();
      // Setting trust store null since we don't need a CA certificate or Mutual
      // Authentication
      TrustManager[] trustManagers = null;

      serverSSLContext = SSLContext.getInstance(PROTOCOL);
      serverSSLContext.init(keyManagers, trustManagers, null);

    } catch (Exception e) {
      logger.error("Failed to initialize the server-side SSLContext", e);
    }

  }

}