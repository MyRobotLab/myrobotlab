package org.myrobotlab.process;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Platform;
import org.myrobotlab.net.Http;

public class GitHub {

  public static final String BRANCHES = "https://api.github.com/repos/MyRobotLab/myrobotlab/branches";

  /*
   * public static String getPyRobotLabScript(String serviceType) { Platform p =
   * Platform.getLocalInstance(); return getPyRobotLabScript(p.getBranch(),
   * serviceType); }
   */
  public static String getPyRobotLabScript(String branch, String serviceType) {
    byte[] script = Http.get(String.format("https://raw.githubusercontent.com/MyRobotLab/pyrobotlab/%s/service/%s.py", branch, serviceType));
    if (script != null) {
      return new String(script);
    }
    return null;
  }

  public static Set<String> getServiceScriptNames() throws Exception {
    Platform p = Platform.getLocalInstance();
    return getServiceScriptNames(p.getBranch());
  }

  public static Set<String> getServiceScriptNames(String branch) throws Exception {
    HashSet<String> list = new HashSet<String>();
    byte[] data = Http.get(String.format("https://api.github.com/repos/MyRobotLab/pyrobotlab/contents/service?ref=%s", branch));
    if (data != null) {
      Object[] files = CodecUtils.decodeArray(new String(data));
      for (int i = 0; i < files.length; ++i) {
        Map<String, String> file = (Map<String, String>) files[i];
        String name = file.get("name");
        list.add(name);
      }
    }
    return list;
  }

}
