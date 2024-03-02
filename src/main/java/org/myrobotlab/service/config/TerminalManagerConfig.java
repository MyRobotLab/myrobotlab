package org.myrobotlab.service.config;

import java.util.Map;
import java.util.TreeMap;

import org.myrobotlab.service.TerminalManager.TerminalStartupConfig;

public class TerminalManagerConfig extends ServiceConfig {

  Map<String, TerminalStartupConfig> terminals = new TreeMap<>();

}
