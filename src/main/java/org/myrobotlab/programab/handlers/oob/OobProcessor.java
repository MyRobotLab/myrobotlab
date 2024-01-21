package org.myrobotlab.programab.handlers.oob;

import java.util.List;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Message;
import org.myrobotlab.programab.models.Mrl;
import org.myrobotlab.programab.models.Oob;
import org.myrobotlab.service.ProgramAB;

public class OobProcessor {

  private transient ProgramAB programab;

  protected int maxBlockTime = 2000;

  public OobProcessor(ProgramAB programab) {
    this.programab = programab;
  }

  public Message toMsg(Mrl mrl) {
    Object[] data = null;
    if (mrl.params != null) {
      data = new Object[mrl.params.size()];
      for (int i = 0; i < data.length; ++i) {
        data[i] = mrl.params.get(i).trim();
      }
    }
    String service = mrl.service == null ? null : mrl.service.trim();
    return Message.createMessage(programab.getName(), service, mrl.method.trim(), data);
  }

  public String process(Oob oob, boolean block) {
    StringBuilder sb = new StringBuilder();

    // FIXME dynamic way of registering oobs
    if (oob != null) {
      // Process <oob><mrl>
      if (oob.mrl != null) {
        List<Mrl> mrls = oob.mrl;
        for (Mrl mrl : mrls) {
          if (!block) {
            // programab.out(toMsg(mrl));
            programab.info("sending without blocking %s", toMsg(mrl));
            programab.send(toMsg(mrl));
          } else {
            try {
              programab.info("sendingBlocking without blocking %s", toMsg(mrl));
              Object o = programab.sendBlocking(toMsg(mrl), maxBlockTime);
              if (o != null) {
                sb.append(o);
              }
            } catch (Exception e) {
              programab.error(e);
            }
          }
        }
      } // for each mrl
    }

    // Process <oob><mrljson>
    if (oob != null && oob.mrljson != null) {

      Message[] msgs = CodecUtils.fromJson(oob.mrljson, Message[].class);
      if (msgs != null) {
        for (Message msg : msgs) {

          if (!block) {
            programab.send(msg);
          } else {
            try {
              Object o = programab.sendBlocking(msg, maxBlockTime);
              if (o != null) {
                sb.append(o);
              }
            } catch (Exception e) {
              programab.error(e);
            }
          }
        } // for each msg
      }
    }

    return sb.toString();
  }
}
