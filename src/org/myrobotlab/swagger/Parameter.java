package org.myrobotlab.swagger;

public class Parameter {
      public String name; // I wish I could get this !!!
      public String in = "path"; // path | formData | body
      public String description;
      public boolean required = true;
      public String type;
      public String format;
}
