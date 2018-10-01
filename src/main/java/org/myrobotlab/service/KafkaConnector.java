package org.myrobotlab.service;

import java.util.ArrayList;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;

/**
 * A kafka connector that can subscribe to a string/string kafka stopic and
 * publish records as they arrive.
 * 
 * TODO: this isn't very configurable yet.. and doesn't really support much.
 * it's really here for a reference and we can expand upon it if anyone cares to
 * use it.
 * 
 * @author kwatters
 *
 */
public class KafkaConnector extends Service {

  public String bootstrapServers = "localhost:9092";
  public String groupId = "test";
  public String keyDeserializer = "org.apache.kafka.common.serialization.StringDeserializer";
  public String valueDeserializer = "org.apache.kafka.common.serialization.StringDeserializer";
  private int pollInterval = 100;
  // TODO: how do templetize.. need to have generics here i think..
  private transient KafkaConsumer<String, String> consumer;
  // TODO: mark volitile?

  public KafkaConnector(String name) {
    super(name);
  }

  public void connect() {
    // TODO: expose more properties. or let them just pass through to the
    // brigade config.
    Properties props = new Properties();
    props.put("bootstrap.servers", bootstrapServers);
    props.put("group.id", groupId);
    props.put("enable.auto.commit", "false");
    props.put("key.deserializer", keyDeserializer);
    props.put("value.deserializer", valueDeserializer);
    consumer = new KafkaConsumer<>(props);
  }

  public void subscribeToTopic(String topic) {
    ArrayList<String> topics = new ArrayList<String>();
    topics.add(topic);
    consumer.subscribe(topics);
    while (true) {
      ConsumerRecords<String, String> records = consumer.poll(pollInterval);
      // TODO: handle proper serialization for kafka , we have a key and a value
      // we probably want to support generic mrl messages as the topic in kafka.
      for (ConsumerRecord<String, String> record : records) {
        invoke("publishRecord", record);
      }
    }
  }

  public ConsumerRecord<String, String> publishRecord(ConsumerRecord<String, String> record) {
    return record;
  }

  public static ServiceType getMetaData() {
    ServiceType meta = new ServiceType(KafkaConnector.class.getCanonicalName());
    meta.addDescription("Provides a string/string consumer for a kafka topic.");
    meta.addCategory("connectivity", "cloud");
    meta.addDependency("org.apache.kafka", "kafka-clients", "1.0.1");
    return meta;
  }

  public static void main(String[] args) {
    // TODO Auto-generated method stub

  }

}
