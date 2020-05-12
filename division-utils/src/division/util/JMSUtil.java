package division.util;

import conf.P;
import java.io.Serializable;
import java.util.Map;
import java.util.Properties;
import javax.jms.*;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;

public  class JMSUtil {
  private static TopicConnection topicConnection;
  private static QueueConnection queueConnection;
  
  public static void dispose() {
    try{topicConnection.close();}catch(Exception ex){}
    try{queueConnection.close();}catch(Exception ex){}
  }
  
  public static void sendQueneMessage(String queneName, Message message) {
    try {
      QueueSession session = queueConnection.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
      javax.jms.Queue queue = session.createQueue(queneName);
      QueueSender sender = session.createSender(queue);
      sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      sender.send(message);
    }catch(Exception ex) {
      Logger.getLogger(JMSUtil.class).warn(ex);
      queueConnection = createQueueConnection();
      sendQueneMessage(queneName, message);
    }
  }
  
  public static void sendTopicMessage(String topicName, String type, Serializable obj) {
    sendTopicMessage(topicName, type, obj, null);
  }
  
  public static void sendTopicMessage(String topicName, String type, Serializable obj, Map<String, Object> prop) {
    try {
      TopicSession session = topicConnection.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
      Message message = session.createObjectMessage(obj);
      message.setJMSType(type);
      message.setStringProperty("topic.name",  topicName);
      if(prop != null)
        for(String key:prop.keySet())
          message.setObjectProperty(key, prop.get(key));
      
      Topic topic = session.createTopic(message.getStringProperty("topic.name"));
      TopicPublisher publisher = session.createPublisher(topic);
      publisher.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      publisher.publish(message);
      publisher.close();
    }catch(Exception ex) {
      Logger.getLogger(JMSUtil.class).warn(ex);
      topicConnection = createTopicConnection();
      sendTopicMessage(topicName, type, obj, prop);
    }
  }
  
  public static void sendTopicMessage(Message message) {
    try {
      TopicSession session = topicConnection.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
      Topic topic = session.createTopic(message.getStringProperty("topic.name"));
      TopicPublisher publisher = session.createPublisher(topic);
      publisher.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      publisher.publish(message);
      publisher.close();
    }catch(Exception ex) {
      Logger.getLogger(JMSUtil.class).warn(ex);
      topicConnection = createTopicConnection();
      sendTopicMessage(message);
    }
  }
  
  public static void sendTopicMessage(String topicName, Serializable message) {
    try {
      TopicSession session = topicConnection.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
      Topic topic = session.createTopic(topicName);
      TopicPublisher publisher = session.createPublisher(topic);
      publisher.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      publisher.publish(session.createObjectMessage(message));
      publisher.close();
    }catch(Exception ex) {
      Logger.getLogger(JMSUtil.class).warn(ex);
      topicConnection = createTopicConnection();
      sendTopicMessage(topicName, message);
    }
  }
  
  public static void sendTopicMessage(String topicName, String text) {
    try {
      TopicSession session = topicConnection.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
      Topic topic = session.createTopic(topicName);
      TopicPublisher publisher = session.createPublisher(topic);
      publisher.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
      publisher.publish(session.createTextMessage(text));
      publisher.close();
    }catch(Exception ex) {
      Logger.getLogger(JMSUtil.class).warn(ex);
      topicConnection = createTopicConnection();
      sendTopicMessage(topicName, text);
    }
  }
  
  public static void createQueue(String source, MessageListener target) {
    try {
      QueueSession session = queueConnection.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
      Queue queue = session.createQueue(source);
      QueueReceiver receiver = session.createReceiver(queue);
      receiver.setMessageListener(target);
    }catch(Exception ex) {
      Logger.getLogger(JMSUtil.class).error(ex);
      queueConnection = createQueueConnection();
      createQueue(source, target);
    }
  }
  
  public static TopicSubscriber addTopicSubscriber(String source, MessageListener target) {
    try {
      TopicSession session = topicConnection.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
      Topic topic = session.createTopic(source);
      TopicSubscriber subscriber = session.createSubscriber(topic);
      subscriber.setMessageListener(target);
      return subscriber;
    }catch(Exception ex) {
      Logger.getLogger(JMSUtil.class).error(ex);
      topicConnection = createTopicConnection();
      return addTopicSubscriber(source, target);
    }
  }
  
  public static void removeTopicSubscriber(String name) {
    try {
      TopicSession session = topicConnection.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
      session.unsubscribe(name);
    }catch(Exception ex) {
      Logger.getLogger(JMSUtil.class).error(ex);
      topicConnection = createTopicConnection();
      removeTopicSubscriber(name);
    }
  }
  
  public static TopicConnection createTopicConnection() {
    try {
      String url = ActiveMQConnection.DEFAULT_BROKER_URL;
      if(P.contains("messanger.protocol") && P.contains("messanger.host") && P.contains("messanger.port"))
        url = P.String("messanger.protocol")+"://"+P.String("messanger.host")+":"+P.Integer("messanger.port");
      TopicConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
      TopicConnection connection = connectionFactory.createTopicConnection();
      connection.start();
      return connection;
    }catch(JMSException ex) {
      Logger.getRootLogger().warn(ex);
      return createTopicConnection();
    }
  }
  
  public static QueueConnection createQueueConnection() {
    try {
      String url = ActiveMQConnection.DEFAULT_BROKER_URL;
      Properties p = System.getProperties();
      if(P.contains("messanger.protocol") && P.contains("messanger.host") && P.contains("messanger.port"))
        url = P.String("messanger.protocol")+"://"+P.String("messanger.host")+":"+P.Integer("messanger.port");
      QueueConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
      QueueConnection connection = connectionFactory.createQueueConnection();
      connection.start();
      return connection;
    }catch(JMSException ex) {
      Logger.getRootLogger().warn(ex);
      return createQueueConnection();
    }
  }
}