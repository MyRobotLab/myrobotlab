package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.Status;

public interface RepoInstallListener {

  public void onInstallProgress(final Status status);

}
