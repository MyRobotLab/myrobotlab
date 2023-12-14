package org.myrobotlab.net;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * A generalized class to help with x509 certificates to support TLS
 */
public class SslUtil {

  /**
   * Returns a SSLSocketFactory if provided with a x509 cert, and key file. The
   * caCrtFile is the certificate authority root and inpassword is the protected
   * password of the cert "if suppied". If unprotected leave null or empty "".
   * @param sslCaFilePath - ca root
   * @param sslCertFilePath - certificate file (pem)
   * @param sslKeyFilePath - private key
   * @param sslPassword - password if key/cert are protected by a password
   * 
   * @return the ssl socket factory
   * @throws Exception
   * 
   */

  public static SSLSocketFactory getSocketFactory(String sslCaFilePath, String sslCertFilePath, String sslKeyFilePath, String sslPassword) throws Exception {
    // Load the SSL certificate authority file
    KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    InputStream caInputStream = new FileInputStream(sslCaFilePath);
    caKeyStore.load(caInputStream, null);
    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(caKeyStore);

    // Load the SSL certificate and key file
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    InputStream inputStream = new FileInputStream(sslCertFilePath);
    keyStore.load(inputStream, sslPassword.toCharArray());
    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(keyStore, sslPassword.toCharArray());

    // Set up the SSL context
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());

    // Create the SSL socket factory
    SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

    return sslSocketFactory;
  }

}
