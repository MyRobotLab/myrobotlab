package org.myrobotlab.gpt;

import java.util.ArrayList;

public class Logprobs {
  public ArrayList<String> tokens;
  public ArrayList<Integer> token_logprobs;
  public ArrayList<TopLogprob> top_logprobs;
  public ArrayList<Integer> text_offset;

}
