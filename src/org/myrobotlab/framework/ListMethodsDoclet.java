package org.myrobotlab.framework;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.RootDoc;

public class ListMethodsDoclet {
  public static boolean start(RootDoc root) {
    ClassDoc[] classes = root.classes();
    for (int i = 0; i < classes.length; i++) {
      System.out.println(classes[i]);
      ClassDoc classdoc = classes[i];
      String x = classdoc.getRawCommentText();
      System.out.println(x);
      MethodDoc[] methods = classes[i].methods();
      for (int j = 0; j < methods.length; j++) {
        MethodDoc m = methods[j];
        System.out.println(m.getRawCommentText());
        if (m.isPublic()) {
          System.out.println("\t" + m.name());
          Parameter[] parameters = m.parameters();
          for (int k = 0; k < parameters.length; k++) {
            Parameter p = parameters[k];
            System.out.println("\t\t" + p.name() + ": " + p.type().qualifiedTypeName());
          }
        }
      }
    }
    return true;
  }

  public static void main(String[] args) {
    String[] params = new String[] { "-doclet", ListMethodsDoclet.class.getName(), "-sourcepath", "src", "-subpackages", "org.myrobotlab.service" };
    com.sun.tools.javadoc.Main.execute(params);
  }
}