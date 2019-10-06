package org.myrobotlab.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.TextListener;
import org.myrobotlab.service.interfaces.TextPublisher;

import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

/**
 *  AWS SQS Topic listener
 */
public class SQS extends Service implements TextPublisher , TextListener {

  private static final long serialVersionUID = 1L;
  public String accessKey = null;
  public String secretKey = null;
  public String queueUrl = null;
  public String sqsRegion = "us-east-1";
  public String messageGroupId = "test";
  public String messageDeduplicationId = "test";

  public Map<String, MessageAttributeValue> attributes = null;
  private AmazonSQS sqs = null;
  
  public SQS(String name) {
    super(name);
  }

  public void connectToSQS() {    
    sqs = AmazonSQSClientBuilder.standard()
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
        .withRegion(sqsRegion).build();
  }
  
  public void sendMessage(String body, Map<String, MessageAttributeValue> attributes) {
    // TODO: what to use the message group id / dedup id for?
    SendMessageRequest send_msg_request = new SendMessageRequest()
        .withQueueUrl(queueUrl)
        .withMessageBody(body)
        .withMessageAttributes(attributes)
        .withMessageGroupId(messageGroupId)
        .withMessageDeduplicationId(messageDeduplicationId);
    sqs.sendMessage(send_msg_request);
  }

  public void receiveMessages() {
    ReceiveMessageResult res = sqs.receiveMessage(queueUrl);
    List<Message> messages = res.getMessages();
    
    for (Message m : messages) {
      System.out.println(m);
      // publish the text from this message...  programAB can listen to this i guess?
      invoke("publishText", m.getBody());
      // TODO: assuming we've received this message, i suppose we should acknolege it somehow?
      
      
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
    sqs.connectToSQS();

    String body = "yo, this is harry.";
    Map<String, MessageAttributeValue> attributes = new HashMap<String, MessageAttributeValue>();
  //  MessageAttributeValue val = new MessageAttributeValue();
    attributes.put("robot_name", new MessageAttributeValue().withStringValue("Harry").withDataType("String"));
    attributes.put("utterance", new MessageAttributeValue().withStringValue("I pitty the foo.").withDataType("String"));

    sqs.sendMessage(body, attributes);

    //
    sqs.receiveMessages();

    System.exit(0);

  }

  @Override
  public String publishText(String text) {
    // publish the text that is retrieved by the sqs message i guess.
    return text;
  }

  @Override
  public void addTextListener(TextListener service) {
      addListener("publishText", service.getName(), "onText");
  }

  @Override
  public void onText(String text) {
    // when we receive text, we should publish it to SQS...  (with some envelope info i guess?)
    // something about what attributes to apply here?
    sendMessage(text, attributes);
    
    
  }
}
