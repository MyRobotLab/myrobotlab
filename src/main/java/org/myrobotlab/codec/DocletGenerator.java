package org.myrobotlab.codec;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doclet;
import com.sun.javadoc.MemberDoc;
import com.sun.javadoc.RootDoc;

/**
 * c
 * @author GroG
 *
 */
public class DocletGenerator extends Doclet {
  public static boolean start(RootDoc root) {
    ClassDoc[] classes = root.classes();

    for (ClassDoc cd : classes) {
      System.out.println("Class [" + cd + "] has the following methods");

      for (MemberDoc md : cd.methods()) {
        System.out.println("  " + md);
      }
    }
    return true;
  }

}
