package org.myrobotlab.gpt;

import java.util.ArrayList;

public class ChatCompletions {
  public String id;
  public String object;
  public int created;
  public String model;
  public ArrayList<Choice> choices;
  public Usage usage;
}
