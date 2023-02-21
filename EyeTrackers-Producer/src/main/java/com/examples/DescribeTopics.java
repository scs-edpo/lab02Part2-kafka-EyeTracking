package com.examples;

import com.google.common.io.Resources;
import org.apache.kafka.clients.admin.AdminClient;
import org.json.simple.parser.ParseException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class DescribeTopics {

    public static void main(String[] args) throws IOException, ParseException, ExecutionException, InterruptedException {

        // initial settings
        AdminClient admin;
        try (InputStream props = Resources.getResource("producer.properties").openStream()) {
            Properties properties = new Properties();
            properties.load(props);
            admin = AdminClient.create(properties);
        }

        //describe topics
        describeTopics(admin);



        admin.close();

    }

    public static void describeTopics(AdminClient admin) throws ExecutionException, InterruptedException {
        System.out.println("-- describing topic --");
        admin.describeTopics(new ArrayList<>() {{
            add("gaze-events");
            add("click-events");
        }
        }).all().get().forEach((topic, desc) -> {
            System.out.println("Topic: " + topic);
            System.out.printf("Partitions: %s%n partition ids: %s%n partitions details:%n%s %n", desc.partitions().size(),
                    desc.partitions()
                            .stream()
                            .map(p -> Integer.toString(p.partition()))
                            .collect(Collectors.joining(",")),
                    desc.partitions()
                            .stream()
                            .map(p -> p.toString())
                            .collect(Collectors.joining("\n"))
            );
        });

    }

}
