package com.examples.customCommit.singleAcessToPartitionAndRebalancingExample;


import com.google.common.io.Resources;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;




public class ConsumerForClickEventsOnly2 {
    public static void main(String[] args) throws IOException, ParseException {

        // Read Kafka properties file and create Kafka consumer with the given properties
        KafkaConsumer<String, Object> consumer;
        try (InputStream props = Resources.getResource("consumerCustomCommit.properties").openStream()) {
            Properties properties = new Properties();
            properties.load(props);
            consumer = new KafkaConsumer<>(properties);
        }

        // subscribe to relevant topics
        consumer.subscribe(Arrays.asList("click-events"));

        int counter=0;

        while (true) {


            // pool new data
            ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(500));
            System.out.println("poll");

            // process consumer records depending on record.topic() and record.value()
            for (ConsumerRecord<String, Object> record : records) {
                // switch/case
                switch (record.topic()) {


                    case "click-events":
                        System.out.println("Received click-events - value: " + record.value());

                        break;

                    default:
                        throw new IllegalStateException("Shouldn't be possible to get message on topic " + record.topic());
                }
            }

             //if(counter%40==0) //uncomment to commit after every 40 polls
            // commit after every poll
            try {
                consumer.commitSync();
                System.out.println("commit sync done");
            } catch (CommitFailedException e) {
               System.err.println("commit failed"+ e);
            }


            counter++;

        }
    }

}

