package org.myrobotlab.net;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

/**
 * A generalized class to help with x509 certificates to support TLS
 */
public class SslUtil {

  /**
   * Returns a SSLSocketFactory if provided with a x509 cert, and key file. The
   * caCrtFile is the certificate authority root and inpassword is the protected
   * password of the cert "if suppied". If unprotected leave null or empty "".
   * 
   * @param caCrtFile
   *          - ca root
   * @param crtFile
   *          - certificate file (pem)
   * @param keyFile
   *          - private key
   * @param inpassword
   *          - password if key/cert are protected by a password
   * @return the ssl socket factory
   * @throws Exception
   *           boom
   */
  public static SSLSocketFactory getSocketFactory(final String caCrtFile, final String crtFile, final String keyFile, final String inpassword) throws Exception {
    Security.addProvider(new BouncyCastleProvider());

    // load CA certificate
    X509Certificate caCert = null;

    FileInputStream fis = new FileInputStream(caCrtFile);
    BufferedInputStream bis = new BufferedInputStream(fis);
    CertificateFactory cf = CertificateFactory.getInstance("X.509");

    while (bis.available() > 0) {
      caCert = (X509Certificate) cf.generateCertificate(bis);
      // System.out.println(caCert.toString());
    }

    // load client certificate
    bis = new BufferedInputStream(new FileInputStream(crtFile));
    X509Certificate cert = null;
    while (bis.available() > 0) {
      cert = (X509Certificate) cf.generateCertificate(bis);
      // System.out.println(caCert.toString());
    }

    // load client private key
    PEMParser pemParser = new PEMParser(new FileReader(keyFile));
    Object object = pemParser.readObject();
    String password = inpassword;
    if (password == null) {
      password = "";
    }
    PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(password.toCharArray());
    JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
    KeyPair key;
    if (object instanceof PEMEncryptedKeyPair) {
      System.out.println("Encrypted key - we will use provided password");
      key = converter.getKeyPair(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv));
    } else {
      System.out.println("Unencrypted key - no password needed");
      key = converter.getKeyPair((PEMKeyPair) object);
    }
    pemParser.close();

    // CA certificate is used to authenticate server
    KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
    caKs.load(null, null);
    caKs.setCertificateEntry("ca-certificate", caCert);
    TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
    tmf.init(caKs);

    // client key and certificates are sent to server so it can authenticate
    // us
    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
    ks.load(null, null);
    ks.setCertificateEntry("certificate", cert);
    ks.setKeyEntry("private-key", key.getPrivate(), password.toCharArray(), new java.security.cert.Certificate[] { cert });
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(ks, password.toCharArray());

    // finally, create SSL socket factory
    SSLContext context = SSLContext.getInstance("TLSv1.2");
    context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

    return context.getSocketFactory();
  }
}
