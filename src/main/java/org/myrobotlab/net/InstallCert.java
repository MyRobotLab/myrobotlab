package org.myrobotlab.net;

/*
 * Copyright 2006 Sun Microsystems, Inc.  All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Sun Microsystems nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/**
 * http://blogs.sun.com/andreas/resource/InstallCert.java
 * Use:
 * java InstallCert hostname
 * Example:
 *% java InstallCert ecc.fedora.redhat.com
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class InstallCert {

  public final static Logger log = LoggerFactory.getLogger(InstallCert.class);

  private static class SavingTrustManager implements X509TrustManager {

    private final X509TrustManager tm;
    private X509Certificate[] chain;

    SavingTrustManager(final X509TrustManager tm) {
      this.tm = tm;
    }

    @Override
    public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
      throw new UnsupportedOperationException();
    }

    @Override
    public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
      this.chain = chain;
      this.tm.checkServerTrusted(chain, authType);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
      // throw new UnsupportedOperationException();
    }
  }

  private static final char[] HEXDIGITS = "0123456789abcdef".toCharArray();

  public static void main(final String[] args) throws KeyManagementException, NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
    String host;
    int port;
    if ((args.length == 1) || (args.length == 2)) {
      final String[] c = args[0].split(":");
      host = c[0];
      port = (c.length == 1) ? 443 : Integer.parseInt(c[1]);
      final String pass = (args.length == 1) ? "changeit" : args[1];
      install(host, port, pass);
    } else {
      log.error("Usage: java InstallCert <host>[:port] [passphrase]");
      return;
    }
  }

  public static void install(String urlstr) throws KeyManagementException, NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
    install(urlstr, null);
  }

  public static void install(String urlstr, String pass) throws KeyManagementException, NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
    URL url = new URL(urlstr);
    install(url.getHost(), url.getPort(), pass);
  }

  public static void install(String host, String inport, String pass)
      throws KeyManagementException, NumberFormatException, NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
    String port = (inport != null) ? "443" : inport;
    install(host, Integer.parseInt(port), pass);
  }

  public static void install(String host, Integer inport, String pass)
      throws NoSuchAlgorithmException, IOException, CertificateException, KeyStoreException, KeyManagementException {

    Integer port = (inport == null || inport == -1) ? 443 : inport;

    char[] passphrase;

    final String p = (pass == null) ? "changeit" : pass;
    passphrase = p.toCharArray();

    File file = new File("jssecacerts");
    if (file.isFile() == false) {
      final char SEP = File.separatorChar;
      final File dir = new File(System.getProperty("java.home") + SEP + "lib" + SEP + "security");
      file = new File(dir, "jssecacerts");
      if (file.isFile() == false) {
        file = new File(dir, "cacerts");
      }
    }

    log.info("Loading KeyStore " + file + "...");
    final InputStream in = new FileInputStream(file);
    final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
    ks.load(in, passphrase);
    in.close();

    final SSLContext context = SSLContext.getInstance("TLS");
    final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(ks);
    final X509TrustManager defaultTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];
    final SavingTrustManager tm = new SavingTrustManager(defaultTrustManager);
    context.init(null, new TrustManager[] { tm }, null);
    final SSLSocketFactory factory = context.getSocketFactory();

    log.info("Opening connection to " + host + ":" + port + "...");
    final SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
    socket.setSoTimeout(10000);
    try {
      log.info("Starting SSL handshake...");
      socket.startHandshake();
      socket.close();
      log.info("No errors, certificate is already trusted");
    } catch (final SSLException e) {
      log.info("Exception: ", e);
    }

    final X509Certificate[] chain = tm.chain;
    if (chain == null) {
      log.info("Could not obtain server certificate chain");
      return;
    }

    final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    log.info("Server sent " + chain.length + " certificate(s):");

    final MessageDigest sha1 = MessageDigest.getInstance("SHA1");
    final MessageDigest md5 = MessageDigest.getInstance("MD5");
    for (int i = 0; i < chain.length; i++) {
      final X509Certificate cert = chain[i];
      log.info(" " + (i + 1) + " Subject " + cert.getSubjectDN());
      log.info("   Issuer  " + cert.getIssuerDN());
      sha1.update(cert.getEncoded());
      log.info("   sha1    " + toHexString(sha1.digest()));
      md5.update(cert.getEncoded());
      log.info("   md5     " + toHexString(md5.digest()));
    }

    log.info("Enter certificate to add to trusted keystore" + " or 'q' to quit: [1]");
    final String line = reader.readLine().trim();
    int k;
    try {
      k = (line.length() == 0) ? 0 : Integer.parseInt(line) - 1;
    } catch (final NumberFormatException e) {
      log.info("KeyStore not changed");
      return;
    }

    final X509Certificate cert = chain[k];
    final String alias = host + "-" + (k + 1);
    ks.setCertificateEntry(alias, cert);

    final OutputStream out = new FileOutputStream(file);
    ks.store(out, passphrase);
    out.close();

    log.info("Cert: {}", cert);
    log.info("Added certificate to keystore 'cacerts' using alias '" + alias + "'");
  }

  private static String toHexString(final byte[] bytes) {
    final StringBuilder sb = new StringBuilder(bytes.length * 3);
    for (int b : bytes) {
      b &= 0xff;
      sb.append(HEXDIGITS[b >> 4]);
      sb.append(HEXDIGITS[b & 15]);
      sb.append(' ');
    }
    return sb.toString();
  }

}
