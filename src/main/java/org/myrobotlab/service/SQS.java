package org.myrobotlab.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.MessageAttributeValue;

/**
 *  AWS SQS Topic listener
 */
public class SQS extends Service {

  private static final long serialVersionUID = 1L;
  private String accessKey = null;
  private String secretKey = null;
  String queueUrl = null;
  public SQS(String name) {
    super(name);
  }
  
  public void sendMessage(String body, Map<String, MessageAttributeValue> attributes) {
    // String queueUrl = sqs.getQueueUrl(QUEUE_NAME).getQueueUrl();
    AmazonSQS sqs = AmazonSQSClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(
        new BasicAWSCredentials(accessKey, secretKey))).
          withRegion("us-east-1").build();
    
    SendMessageRequest send_msg_request = new SendMessageRequest()
        .withQueueUrl(queueUrl)
        .withMessageBody("hello world")
        .withMessageAttributes(attributes)
        .withMessageGroupId("test")
        .withMessageDeduplicationId("test");
sqs.sendMessage(send_msg_request);
  }
  
  public void receiveMessages() {
   // String queueUrl = sqs.getQueueUrl(QUEUE_NAME).getQueueUrl();
    AmazonSQS sqs = AmazonSQSClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(
        new BasicAWSCredentials(accessKey, secretKey))).
          withRegion("us-east-1").build();
    // receive messages from the queue
    List<Message> messages = sqs.receiveMessage(queueUrl).getMessages();
    
    for (Message m : messages) {
      System.out.println(m);
    }
  }
  
  public static ServiceType getMetaData() {
    ServiceType meta = new ServiceType(SQS.class.getCanonicalName());
    meta.addDescription("Provides a publisher and consumer for AWS SQS topics.");
    meta.addCategory("connectivity", "cloud");
    meta.addDependency("com.amazonaws", "aws-java-sdk-sqs", "1.11.637");
    return meta;
  }

  public static void main(String[] args) {
    
    LoggingFactory.init(Level.INFO);
    
    SQS sqs = new SQS("mysqs");
   String body = "yo, this is harry.";
    Map<String, MessageAttributeValue> attributes = new HashMap<String, MessageAttributeValue>();
    
    MessageAttributeValue val = new MessageAttributeValue();
   
    attributes.put("robot_name", new MessageAttributeValue().withStringValue("Harry").withDataType("String"));
    attributes.put("utterance", new MessageAttributeValue().withStringValue("I pitty the foo.").withDataType("String"));
    
    sqs.sendMessage(body, attributes);
    
    //
    sqs.receiveMessages();
    
    System.exit(0);
    
  }
}
