package org.myrobotlab.mqtt;

import org.checkerframework.checker.interning.qual.EqualsMethod;

import java.util.Objects;

public class MqttMsg {
  /**
   * CONNECT(1), CONNACK(2), PUBLISH(3), PUBACK(4), PUBREC(5), PUBREL(6),
   * PUBCOMP(7), SUBSCRIBE(8), SUBACK(9), UNSUBSCRIBE(10), UNSUBACK(11),
   * PINGREQ(12), PINGRESP(13), DISCONNECT(14);
   */
  protected String messageType;

  protected long ts = 0;

  protected boolean dupe;
  /**
   * AT_MOST_ONCE(0), AT_LEAST_ONCE(1), EXACTLY_ONCE(2), FAILURE(0x80);
   */
  protected int qosLevel;

  protected boolean retain;

  protected int remainingLength;

  protected String clientId;

  protected String username;

  protected String topicName;

  protected int packetId;

  // FIXME - convert to byte[]
  String payload;

  public MqttMsg() {
    ts = System.currentTimeMillis();
  }

  public String getMessageType() {
    return messageType;
  }

  public void setMessageType(String messageType) {
    this.messageType = messageType;
  }

  public boolean isDup() {
    return dupe;
  }

  public void setDup(boolean isDup) {
    this.dupe = isDup;
  }

  public int getQosLevel() {
    return qosLevel;
  }

  public void setQosLevel(int qosLevel) {
    this.qosLevel = qosLevel;
  }

  public boolean isRetain() {
    return retain;
  }

  public void setRetain(boolean isRetain) {
    this.retain = isRetain;
  }

  public int getRemainingLength() {
    return remainingLength;
  }

  public void setRemainingLength(int remainingLength) {
    this.remainingLength = remainingLength;
  }

  public String getClientID() {
    return clientId;
  }

  public void setClientID(String clientID) {
    this.clientId = clientID;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getTopicName() {
    return topicName;
  }

  public void setTopicName(String topicName) {
    this.topicName = topicName;
  }

  public int getPacketId() {
    return packetId;
  }

  public void setPacketId(int packetId) {
    this.packetId = packetId;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append(String.format("packetId:%d ", packetId));

    if (username != null) {
      sb.append(String.format("username:%s ", username));
    }
    if (topicName != null) {
      sb.append(String.format("topicName:%s ", topicName));
    }

    if (messageType != null) {
      sb.append(String.format("type:%s ", messageType));
    }

    if (payload != null) {
      sb.append(String.format("payload:%s ", payload));
    }

    if (retain) {
      sb.append("retain");
    }
    return sb.toString();

  }

  @Override
  public final int hashCode() {
    return Objects.hash(messageType, ts, dupe, qosLevel, retain, remainingLength, clientId, username, topicName, packetId, payload);
  }

  @EqualsMethod
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    MqttMsg mqttMsg = (MqttMsg) o;

    if (ts != mqttMsg.ts) return false;
    if (dupe != mqttMsg.dupe) return false;
    if (qosLevel != mqttMsg.qosLevel) return false;
    if (retain != mqttMsg.retain) return false;
    if (remainingLength != mqttMsg.remainingLength) return false;
    if (packetId != mqttMsg.packetId) return false;
    if (!Objects.equals(messageType, mqttMsg.messageType)) return false;
    if (!Objects.equals(clientId, mqttMsg.clientId)) return false;
    if (!Objects.equals(username, mqttMsg.username)) return false;
    if (!Objects.equals(topicName, mqttMsg.topicName)) return false;
    return Objects.equals(payload, mqttMsg.payload);
  }
}
