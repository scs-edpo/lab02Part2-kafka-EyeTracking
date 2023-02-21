package com.examples;


import com.data.Clicks;
import com.google.common.io.Resources;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;


public class ClicksProducer {

    public static void main(String[] args) throws IOException {

        // Specify Topic
        String topic = "click-events";

        // create Kafka producer, set properties settings, delete existing topic, create new topic
        KafkaProducer<String, Clicks> producer;
        try (InputStream props = Resources.getResource("producer.properties").openStream()) {
            // set properties
            Properties properties = new Properties();
            properties.load(props);
            // init producer
            producer = new KafkaProducer<>(properties);
            /// delete existing topic with the same name
            deleteTopic(topic, properties);
            // create new topic with 1 partition
            try {
                createTopic(topic, 1, properties);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        try {

            // counter is used as an event id
            int counter = 0;

            while(true) {
                // sleep for a random time interval between 500 ms and 5000 ms
                try {
                    Thread.sleep(getRandomNumber(500, 5000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // generate a random click event using constructor  Clicks(int eventID, long timestamp, int xPosition, int yPosition, String clickedElement)
                Clicks clickEvent = new Clicks(counter,System.nanoTime(), getRandomNumber(0, 1920), getRandomNumber(0, 1080), "EL"+getRandomNumber(1, 20));

                // send the click event
                producer.send(new ProducerRecord<String, Clicks>(
                        topic, // topic
                        clickEvent  // value
                ));

                // print to console
                System.out.println("clickEvent sent: "+clickEvent.toString());

                // increment counter
                counter++;

            }

        } catch (Throwable throwable) {
            System.out.println(throwable.getStackTrace());
        } finally {
            producer.close();
        }


    }

    /*
    Generate a random nunber
    */
    private static int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    /*
    Create topic
     */
    private static void createTopic(String topicName, int numPartitions, Properties properties) throws Exception {

        AdminClient admin = AdminClient.create(properties);

        //checking if topic already exists
        boolean alreadyExists = admin.listTopics().names().get().stream()
                .anyMatch(existingTopicName -> existingTopicName.equals(topicName));
        if (alreadyExists) {
            System.out.printf("topic already exits: %s%n", topicName);
        } else {
            //creating new topic
            System.out.printf("creating topic: %s%n", topicName);
            NewTopic newTopic = new NewTopic(topicName, numPartitions, (short) 1);
            admin.createTopics(Collections.singleton(newTopic)).all().get();
        }
    }

    /*
    Delete topic
     */
    private static void deleteTopic(String topicName, Properties properties) {
        try (AdminClient client = AdminClient.create(properties)) {
            client.deleteTopics(Collections.singleton(topicName));
        }


    }

}
