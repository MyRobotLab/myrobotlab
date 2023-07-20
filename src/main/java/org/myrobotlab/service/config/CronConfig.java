package org.myrobotlab.service.config;

import java.util.LinkedHashMap;

import org.myrobotlab.service.Cron.Task;

public class CronConfig extends ServiceConfig {

  public LinkedHashMap<String, Task> tasks = new LinkedHashMap<>();

}
