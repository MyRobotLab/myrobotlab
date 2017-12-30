/**
 * @author SwedaKonsult
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 */
package org.myrobotlab.ui.autocomplete;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.Completion;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.reflection.Locator;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.swing.widget.JavaCompletionProvider;
import org.slf4j.Logger;

/**
 * @author SwedaKonsult
 * 
 */
public class MRLCompletionProvider extends JavaCompletionProvider {
  /**
   * Logger for this guy.
   */
  public final static Logger log = LoggerFactory.getLogger(SwingGui.class.getCanonicalName());

  /**
   * Create the modifiers HTML string that will be used in the SwingGui.
   * 
   * @param modifiers
   * @return empty string if there are no modifiers that meet our criteria
   */
  private CharSequence buildModifiersDescriptionForGUI(int modifiers) {
    if (modifiers <= 0) {
      return "";
    }
    StringBuffer modifiersDescription = new StringBuffer();
    if (Modifier.isStatic(modifiers)) {
      modifiersDescription.append("<li>").append("static").append("</li>");
    }
    if (Modifier.isSynchronized(modifiers)) {
      modifiersDescription.append("<li>").append("synchronized").append("</li>");
    }

    if (modifiersDescription.length() == 0) {
      return "";
    }
    modifiersDescription.insert(0, "<br><br><b><i>Modifiers:</i></b><ul>").append("</ul>");
    return modifiersDescription;
  }

  /**
   * Get the class name part of the full descriptor.
   * 
   * @param fullClassName
   *          the class name including package name.
   * @return the class name parsed out of the full name.
   */
  private CharSequence getClassName(String fullClassName) {
    return fullClassName.subSequence(fullClassName.lastIndexOf('.') + 1, fullClassName.length());
  }

  /**
   * Load all constants/static fields available and set them up with the class
   * name prefixing the variable name in order to be able to suggest when the
   * class name is typed in.
   * 
   * @param implementation
   */
  private void loadClassFields(Class<?> implementation) {
    if (implementation == null) {
      return;
    }
    Field[] fields = implementation.getDeclaredFields();
    if (fields == null || fields.length == 0) {
      return;
    }
    Completion completer;
    StringBuffer genericsString = new StringBuffer();
    String fieldTypeName;
    String fullClassName = implementation.getName();
    String className = getClassName(fullClassName).toString();
    // log.error(className);

    if (!(className.contains("$"))) {
      for (Field f : fields) {
        if (f.getName() == "main" || !Modifier.isPublic(f.getModifiers())) {// ||
          // !Modifier.isStatic(f.getModifiers()))
          // {
          continue;
        }
        // TODO: grab the generics for this field
        genericsString.delete(0, genericsString.length());

        fieldTypeName = f.getType().getName();
        completer = new BasicCompletion(this, String.format("%s.%s", className, f.getName()), getClassName(fieldTypeName).toString(),
            String.format("<html><body>" + "<b>%1$s %2$s.%3$s" + "%4$s" + "</b> %5$s</body></html>", fieldTypeName, className, f.getName(), genericsString,
                buildModifiersDescriptionForGUI(f.getModifiers())));
        addCompletion(completer);
      }
    }
    completer = null;
  }

  /**
   * Helper method that recurses implementation to find all public methods
   * declared.
   * 
   * @param implementation
   *          the class to analyze
   */
  private void loadClassMethods(Class<?> implementation) {
    if (implementation == null) {
      return;
    }
    Method[] methods = implementation.getDeclaredMethods();
    if (methods == null || methods.length == 0) {
      return;
    }
    Completion completer;
    int arrayLength = 0;
    int loop = 0;
    Class<?>[] params;
    StringBuffer paramsString = new StringBuffer();
    StringBuffer shortParamsString = new StringBuffer();
    StringBuffer genericsString = new StringBuffer();
    String matchString, extraString;
    String fullClassName = implementation.getName();
    String className = getClassName(fullClassName).toString();
    boolean isStatic;
    if (!(className.contains("$"))) {

      for (Method m : methods) {
        if (m.getName() == "main" || !Modifier.isPublic(m.getModifiers())) {
          continue;
        }
        paramsString.delete(0, paramsString.length());
        params = m.getParameterTypes();
        arrayLength = params.length;
        isStatic = Modifier.isStatic(m.getModifiers());
        if (isStatic) {
          shortParamsString.delete(0, shortParamsString.length());
        }
        if (arrayLength > 0) {
          for (loop = 0; loop < arrayLength; loop++) {
            if (loop > 0) {
              paramsString.append(",");
            }
            if (isStatic) {
              shortParamsString.append(getClassName(params[loop].getName()));
            }
            paramsString.append(params[loop].getName());
            // TODO: should grab the generics for each parameter
          }
        }
        genericsString.delete(0, genericsString.length());

        // if (isStatic) {
        matchString = String.format("%s.%s(", getClassName(fullClassName), m.getName());
        extraString = shortParamsString.toString();
        /*
         * } else { matchString = String.format(".%s(", m.getName());
         * extraString = m.getName(); }
         */
        // TODO: grab the generics for this method
        completer = new BasicCompletion(this, matchString, extraString,
            String.format("<html><body>" + "<b>%1$s %2$s.%3$s" + "%5$s(%4$s)" + "</b> %6$s <br> <a href=\"http://myrobotlab.org/service/%2$s\">more help</a> </body></html>",
                m.getReturnType().getName(), className, m.getName(), paramsString, genericsString, buildModifiersDescriptionForGUI(m.getModifiers())));
        addCompletion(completer);
      }
    }
    completer = null;
  }

  /**
   * Overriding base class declaration in order to load methods that should be
   * easy to find and use in Python. Still calls out the base class in order to
   * load the Java keywords.
   */
  @Override
  protected void loadCompletions() {
    super.loadCompletions();

    try {
      loadInformationFromClasses(Locator.getClasses("org.myrobotlab.service"));
    } catch (IOException e) {
      log.error("Could not load MRLCompletions because of I/O issues.", e);
    }
  }

  /**
   * Load all information we want for the UI from the class.
   * 
   * @param implementation
   */
  private void loadInformationFromClass(Class<?> implementation) {
    loadClassMethods(implementation);
    loadClassFields(implementation);
  }

  /**
   * Load everything from the classes in the list.
   * 
   * @param classList
   */
  private void loadInformationFromClasses(List<Class<?>> classList) {
    if (classList == null || classList.size() == 0) {
      return;
    }
    for (Class<?> c : classList) {
      loadInformationFromClass(c);
    }
  }
}
