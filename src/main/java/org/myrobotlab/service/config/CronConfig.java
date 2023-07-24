package org.myrobotlab.service.config;

import java.util.ArrayList;
import java.util.List;

import org.myrobotlab.service.Cron.Task;

public class CronConfig extends ServiceConfig {

  public List<Task> tasks = new ArrayList<>();

}
