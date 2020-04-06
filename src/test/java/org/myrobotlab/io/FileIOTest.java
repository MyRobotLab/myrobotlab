package org.myrobotlab.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.io.FileIO.FileComparisonException;
import org.myrobotlab.test.AbstractTest;

public class FileIOTest extends AbstractTest {

  static String tempDir = null;
  static String t1;
  static String t2;
  static String t3;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    Path dir = Files.createTempDirectory("test");
    tempDir = dir.toString();
    File mkdir = new File(tempDir);
    mkdir.mkdirs();
    t1 = FileIO.gluePaths(tempDir, "test1.tx");
    t2 = FileIO.gluePaths(tempDir, "test2.tx");
    t3 = FileIO.gluePaths(tempDir, "test3.tx");
  }

  @AfterClass
  public static void setUpAfterClass() throws Exception {
    FileIO.rmDir(new File("test"), null);
  }

  @Test
  public void testCompareFiles() throws IOException, FileComparisonException {
    boolean correctlyThrew = false;
    FileIO.toFile(t1, "This is test data\n did it work?");
    FileIO.toFile(t2, "This is test data\n did it work?");
    FileIO.compareFiles(t1, t2);
    try {
      FileIO.toFile(t2, "This should not work");
      FileIO.compareFiles(t1, t2);

    } catch (FileComparisonException e) {
      correctlyThrew = true;
    }
    if (!correctlyThrew) {
      throw new IOException("comparison did not throw on different files");
    }

  }

  @Test
  public void testCopyStringString() throws IOException {
    String data = "This is test data\n did it work?";
    FileIO.toFile(t1, data);
    FileIO.copy(t1, t3);
    String data2 = FileIO.toString(t3);
    assertEquals(data, data2);
  }

  @Test
  public void testGetCfgDir() {
    String cfgdir = FileIO.getCfgDir();
    cfgdir.contains(File.separator);
    assertEquals(FileIO.gluePathsForwardSlash("data/.myrobotlab", "blah"), "data/.myrobotlab/blah");
  }

  @Test
  public void testGetRoot() {
    String ret = FileIO.getRoot();
    assertNotNull(ret);
  }

  @Test
  public void testGetServiceList() throws IOException {
    List<String> ret = FileIO.getServiceList();
    if (ret.size() == 0) {
      throw new IOException("serivce list is 0");
    }
  }

  @Test
  public void testGluePaths() {
    String path1 = "/abc/";
    String path2 = "/def/";
    String ret = FileIO.gluePathsForwardSlash(path1, path2);
    assertEquals("/abc/def/", ret);

    path1 = "/abc";
    path2 = "def/";
    ret = FileIO.gluePathsForwardSlash(path1, path2);
    assertEquals("/abc/def/", ret);

    path1 = "\\abc\\";
    path2 = "def\\";
    ret = FileIO.gluePathsForwardSlash(path1, path2);
    assertEquals("/abc/def/", ret);
  }

  @Test
  public void testIsJar() {
    assertFalse(FileIO.isJar());
  }

  @Test
  public void testGetFileListString() throws IOException {
    String dir = FileIO.gluePaths(tempDir, "testGetFileListString");
    File f = new File(dir);
    f.delete();
    f.mkdirs();
    FileIO.toFile(FileIO.gluePaths(dir, "file1.txt"), "file 1");
    FileIO.toFile(FileIO.gluePaths(dir, "file2.txt"), "file 2");
    FileIO.toFile(FileIO.gluePaths(dir, "file3.txt"), "file 3");
    List<File> files = FileIO.getFileList(dir);
    assertEquals(3, files.size());
  }

  @Test
  public void testRmString() throws IOException {
    String f = FileIO.gluePaths(tempDir, "testRmString.txt");
    FileIO.toFile(f, "data");
    FileIO.rm(f);
    File test = new File(f);
    assertFalse(test.exists());
  }

  @Test
  public void testToFileStringByteArray() throws IOException {
    String f = FileIO.gluePaths(tempDir, "testToFileStringByteArray.txt");
    FileIO.toFile(f, "data");
    byte[] data = FileIO.toByteArray(new File(f));
    assertEquals("data", new String(data));
  }

  @Test
  public void testLoadPropertiesAsMap() throws FileNotFoundException, IOException {
    String f = FileIO.gluePaths(tempDir, "test.properties");
    Properties p = new Properties();
    p.put("key1", "value1");
    p.put("key2", "value2");
    p.put("key3", "value3");
    p.store(new FileOutputStream(f), "comments");
    Map<String, String> map = FileIO.loadPropertiesAsMap(f);
    assertEquals(3, map.size());
    assertEquals("value1", map.get("key1"));
  }

  @Test
  public void testToInputStreamString() throws IOException {
    InputStream ios = FileIO.toInputStream("This is some data that got turned into a stream");
    String data = FileIO.toString(ios);
    assertEquals("This is some data that got turned into a stream", data);
  }

  @Test
  public void testCleanFileName() {
    String ret = FileIO.cleanFileName("?FD*)(3kj249fd0sf0873724tkgasjkvxc fdsa0?~~@#()*#$^^&#");
    assertEquals("_FD___3kj249fd0sf0873724tkgasjkvxc_fdsa0______________", ret);
    ret = FileIO.cleanFileName("blah.txt");
    assertEquals("blah.txt", ret);
  }

  @Test
  public void testToSafeString() {
    String ret = FileIO.toSafeString("file does not exist");
    assertNull(ret);
  }

}
