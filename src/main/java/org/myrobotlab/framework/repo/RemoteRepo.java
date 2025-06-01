package org.myrobotlab.framework.repo;

public class RemoteRepo {

  public String id;
  // public String name;
  public String url;
  public String comment;

  public RemoteRepo(String id, String url, String comment) {
    this.id = id;
    this.url = url;
    this.comment = comment;
  }

  public RemoteRepo(String id, String url) {
    this(id, url, null);
  }

}
