package com.examples;


import com.data.Gaze;
import com.google.common.io.Resources;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;


public class EyeTrackersProducer {

    public static void main(String[] args) throws IOException {

        // Specify Topic
        String topic = "gaze-events";

        // deviceIDs with a counter of the generated events
        HashMap<Integer,Integer> deviceIDs = new HashMap<>(){
            {
                // device id (correspond to topic's  partition id), counter of generated events
                put(0, 0);
                put(1, 0);
            }};

        // create Kafka producer, set properties settings, delete existing topic, create new topic
        KafkaProducer<String, Gaze> producer;
        try (InputStream props = Resources.getResource("producer.properties").openStream()) {
            // set properties
            Properties properties = new Properties();
            properties.load(props);
            // init producer
            producer = new KafkaProducer<>(properties);
            /// delete existing topic with the same name
            deleteTopic(topic, properties);
            // create new topic with 2 partitions
            try {
                createTopic(topic, 2, properties);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        try {

            while(true) {
                // sleep for 8 ms
                try {
                    Thread.sleep(8);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // select random device
                int deviceID =getRandomNumber(0, deviceIDs.size());

                // get the corresponding events counter
                int deviceEventsCounter = deviceIDs.get(deviceID);

                // generate a random gaze event using constructor  Gaze(int eventID, long timestamp, int xPosition, int yPosition, int pupilSize)
                Gaze gazeEvent = new Gaze(deviceEventsCounter,System.nanoTime(), getRandomNumber(0, 1920), getRandomNumber(0, 1080), getRandomNumber(3, 4));

                // send the gaze event
                producer.send(new ProducerRecord<String, Gaze>(
                        topic, // topic
                        String.valueOf(deviceID), // key
                        gazeEvent  // value
                ));

                // print to console
                System.out.println("gazeEvent sent: "+gazeEvent.toString()+" from deviceID: "+deviceID);

                // increment events counter for the specific device
                deviceIDs.put(deviceID, deviceEventsCounter+1) ;
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
