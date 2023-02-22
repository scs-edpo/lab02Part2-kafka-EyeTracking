# Lab02Part2-kafka-EyeTracking

- This lab is based on the first part of Lab02Part1 (https://github.com/scs-edpo/lab02Part1-kafka-producer-consumer), which is also a prerequisite for this lab
- The procedure to run the code is similar to Lab02. We recommend importing the project to  IntelliJ and let the IDE handle everything
- Note that only the new procedures and concepts are described in this lab

## Use Case

- This lab simulates a system where user clicks and eye-tracking data coming from two eye-trackers are streamed
- The eye-tracking data captures the gazes of two developers doing pair programming
- We use Kafka producers and consumers to simulate this system

## Objectives

- Experimenting several producers and consumers with different configurations for topics and partitions
- Hands-on a custom serializer and partitioner
- Experimenting Kafka rebalancing and how it affects the distribution of partitions among consumers
- Experimenting offsets and manual offset commits

## Overview

This lab consists of two parts.

In the first part, we create two producers (ClickStream-Producer and EyeTrackers-Producer)
-  [_ClickStream-Producer_ Module](ClickStream-Producer/): produces click stream data
-  [_EyeTrackers-Producer_ Module](EyeTrackers-Producer/): produces gaze data

In the second part, we consume the messages of the producers using consumers with different configurations. All the consumers are available in the ["consumer"](consumer/) Module, within the Package ["com.examples"](consumer/src/main/java/com/examples)
- [_ConsumerForAllEvents_](consumer/src/main/java/com/examples/ConsumerForAllEvents.java): consumes the events coming from both [ClickStream-Producer](ClickStream-Producer/) and [EyeTrackers-Producer](EyeTrackers-Producer/)
- [_ConsumerForGazeEventsForSingleEyeTracker_](consumer/src/main/java/com/examples/ConsumerForGazeEventsForSingleEyeTracker.java): consumes the events coming from a single eye-tracker
- [_ConsumerCustomOffset_](consumer/src/main/java/com/examples/ConsumerCustomOffset.java): consumes the events coming from [ClickStream-Producer](ClickStream-Producer/) starting from a specific user-defined offset
- [_rebalancingExample.*_](consumer/src/main/java/com/examples/rebalancingExample/): two classes that demonstrate how Kafka does rebalancing when a new consumer is added (within the same group)
- [_singleAcessToPartitionAndRebalancingExample.*_](consumer/src/main/java/com/examples/singleAcessToPartitionAndRebalancingExample/): two classes demonstrating how Kafka allows only one consumer to read from a partition and how rebalancing occurs when a running consumer is out
- [_customCommit.singleAcessToPartitionAndRebalancingExample.*_](consumer/src/main/java/com/examples/customCommit/singleAcessToPartitionAndRebalancingExample/): two classes demonstrating the use of manual offset commit (occuring after every n consumer polls) and the impact of rebalancing on events duplication and loss
- [_customCommit.commitLargerOffset.*_](consumer/src/main/java/com/examples/customCommit/commitLargerOffset/): two classes demonstrating events loss when a manual offset that is larger than the offset of the latest processed event is set


## Running the Docker image

1. Open a terminal in the directory: docker/.
2. Start the Kafka and Zookeeper processes using Docker Compose:

    ```
    $ docker-compose up
    ```

## Producers

### ClickStream-Producer

- Main Class:  [com.examples.ClicksProducer](ClickStream-Producer/src/main/java/com/examples/ClicksProducer.java)

  - **Overview:** This producer produces click events and sends them through the _"click-events"_ topic.
  - **Procedure (#P1):**
    - Specify topic
      ```Java
          // Specify Topic
          String topic = "click-events";
         ```
    - Read Kafka properties file
       ```Java
          // Read Kafka properties file
          Properties properties;
          try (InputStream props = Resources.getResource("producer.properties").openStream()) {
              properties = new Properties();
              properties.load(props);
          }
         ```
      - The following is the content of the used properties file [producer.properties](ClickStream-Producer/src/main/resources/producer.properties)
        ```properties
           acks=all
           retries=0
           bootstrap.servers=localhost:9092
           key.serializer=org.apache.kafka.common.serialization.StringSerializer
           value.serializer=com.utils.JavaSerializer
           ```
      - Notice that we use a custom (value) serializer (see [com.utils.JavaSerializer](ClickStream-Producer/src/main/java/com/utils/JavaSerializer.java)) to serialize Java Objects before sending them 
        - The custom serializer is specified in [producer.properties](ClickStream-Producer/src/main/resources/producer.properties) with: _value.serializer=com.utils.JavaSerializer_
        
    - Create Kafka producer with the loaded properties
       ```Java
        // Create Kafka producer
        KafkaProducer<String, Clicks> producer = producer = new KafkaProducer<>(properties);
       ```  
    - For the sake of simulation,  delete any existing topic with the same _topic_ name (i.e., _click-events_) and create a new topic with 1 partition. **Note that, we use a single partition inside the "click-events" topic so that all the events will be stored into that unique partition of the "click-events" topic**
       ```Java
         /// delete existing topic with the same name
          deleteTopic(topic, properties);

          // create new topic with 1 partition
          createTopic(topic, 1, properties);
         ```
    - Define  a counter which will be used as an eventID
       ```Java
          // define a counter which will be used as an eventID
          int counter = 0;
         ```        
    - At each random time interval in range [500ms, 5000ms]
      - Generate a random click event using constructor  _Clicks(int eventID, long timestamp, int xPosition, int yPosition, String clickedElement)_ (see [com.data.Clicks](ClickStream-Producer/src/main/java/com/data/Clicks.java)). Note that the counter is used as an eventID
      ```Java
      // generate a random click event using constructor  Clicks(int eventID, long timestamp, int xPosition, int yPosition, String clickedElement)
        Clicks clickEvent = new Clicks(counter,System.nanoTime(), getRandomNumber(0, 1920), getRandomNumber(0, 1080), "EL"+getRandomNumber(1, 20));
       ``` 
        
      - Send the click event and print the event to the producer console
      ```Java
              // send the click event
              producer.send(new ProducerRecord<String, Clicks>(
                      topic, // topic
                      clickEvent  // value
              ));

              // print to console
              System.out.println("clickEvent sent: "+clickEvent.toString());         ```         
      ``` 
      - Increment the counter (i.e., the eventID) for future use
      ```Java
      // increment counter i.e., eventID
         counter++;
       ```         
      

#### Instruction

- Explore the different classes within [ClickStream-Producer](ClickStream-Producer/src/main/) and examine the control-flow within the class ["com.examples.ClicksProducer"](ClickStream-Producer/src/main/java/com/examples/ClicksProducer.java)

### EyeTrackers-Producer

- Main Class: [com.examples.EyeTrackersProducer](EyeTrackers-Producer/src/main/java/com/examples/EyeTrackersProducer.java)
  - **Overview:** This producer produces gaze events and sends them through the _"gaze-events"_ topic.
  - **Procedure (#P2):**
    - Specify topic
        ```Java
            // Specify Topic
            String topic = "gaze-events";
        ```
    - Read Kafka properties file (similar to Procedure [P1](#P1))
       - The following is the content of the used properties file [producer.properties](EyeTrackers-Producer/src/main/resources/producer.properties)
         ```properties
            acks=all
            retries=0
            bootstrap.servers=localhost:9092
            key.serializer=org.apache.kafka.common.serialization.StringSerializer
            value.serializer=com.utils.JavaSerializer
            partitioner.class = com.utils.CustomPartitioner
            ```
         - Remember that in our use-case, **we have 2 eye-trackers**, and we would like to **store the data from each eye-tracker into a distinct partition**. Therefore, we use a custom partitioner (see [com.utils.CustomPartitioner](EyeTrackers-Producer/src/main/java/com/utils/CustomPartitioner.java)) to ensure that **the events coming from each eye-tracker are always stored into the same distinct partition** 
           - _Reason:_ with the default partitioner, Kafka guarantees that events with the same key will go to the same partition, but not the other way around i.e., events with different keys will go always to different partitions. Knowing that events are assigned to partitions as follows: _"partitionID = hash(key)%num_partitions"_, with a low partition number (e.g., _num_partitions=2_), it is very likely that 2 events with different keys will still go to the same partition.
           - The custom partitioner is specified in [resources/producer.properties](EyeTrackers-Producer/src/main/resources/producer.properties) with: _partitioner.class=com.utils.CustomPartitioner_
         - Similar to [P1](#P1), we use a custom  (value) serializer (see [com.utils.JavaSerializer](EyeTrackers-Producer/src/main/java/com/utils/JavaSerializer.java)) to serialize Java Objects before sending them 
  
    - Create Kafka producer with the loaded properties (similar to [P1](#P1))
    - For the sake of simulation,  delete any existing topic with the same _topic_ name (i.e., _gaze-events_) and **create a new topic with 2 partitions** _(i.e., corresponding to two eye-trackers)_
      ```Java
        /// delete existing topic with the same name
         deleteTopic(topic, properties);

         // create new topic with 2 partitions
         createTopic(topic, 2, properties);
        ```
    - Define a counter which will be used as an eventID
      ```Java
         // define a counter which will be used as an eventID
         int counter = 0;
        ```        
    - At each 8ms
      - Select a deviceID corresponding to a random eye-tracker (among the two available eye-trackers)
        ```Java
            // select random device
            int deviceID =getRandomNumber(0, deviceIDs.length);
        ```
      - Generate a random gaze event using the constructor _Gaze(int eventID, long timestamp, int xPosition, int yPosition, int pupilSize)_ (see [com.data.Gaze](EyeTrackers-Producer/src/main/java/com/data/Gaze.java)). Note that the counter is used as an eventID
        ```Java
           // generate a random gaze event using constructor  Gaze(int eventID, long timestamp, int xPosition, int yPosition, int pupilSize)
           Gaze gazeEvent = new Gaze(counter,System.nanoTime(), getRandomNumber(0, 1920), getRandomNumber(0, 1080), getRandomNumber(3, 4));         
        ``` 
          
      - Send the gaze event and print the event to the producer console. **Notice that we use the deviceID as a key in the send method. This deviceID will be mapped to the corresponding partition in the _"gaze-events"_ topic**. 
          ```Java
                // send the gaze event
                producer.send(new ProducerRecord<String, Gaze>(
                        topic, // topic
                        String.valueOf(deviceID), // key
                        gazeEvent  // value
                ));   
                // print to console
                System.out.println("gazeEvent sent: "+gazeEvent.toString()+" from deviceID: "+deviceID);
                          
        ```       
      - Increment the counter (i.e., the eventID) for future use (Similar to [P1](#P1))

  
#### Instruction

- Explore the different classes within [EyeTrackers-Producer](EyeTrackers-Producer/src/main/) and examine the control-flow within the class [com.examples.EyeTrackersProducer](EyeTrackers-Producer/src/main/java/com/examples/EyeTrackersProducer.java)

## Consumers

### ConsumerForAllEvents

- Prerequisite for running [ConsumerForAllEvents](consumer/src/main/java/com/examples/ConsumerForAllEvents.java): 
  - Stop all previously running producers and consumers
  - (Re)Run [EyeTrackers-Producer](EyeTrackers-Producer/) (Main Class [com.examples.EyeTrackersProducer](EyeTrackers-Producer/src/main/java/com/examples/EyeTrackersProducer.java)) and [ClickStream-Producer](ClickStream-Producer/)  (Main Class [com.examples.ClicksProducer](ClickStream-Producer/src/main/java/com/examples/ClicksProducer.java)) 


- Main Class: [com.examples.ConsumerForAllEvents](consumer/src/main/java/com/examples/ConsumerForAllEvents.java) 

  - **Overview:** this consumer consumes the events coming from both [ClickStream-Producer](ClickStream-Producer/) and [EyeTrackers-Producer](EyeTrackers-Producer/)
  - **Procedure (#P3):**
    - Read Kafka properties file and create Kafka consumer with the given properties
         ```Java
          // Read Kafka properties file and create Kafka consumer with the given properties
          KafkaConsumer<String, Object> consumer;
          try (InputStream props = Resources.getResource("consumer.properties").openStream()) {
              Properties properties = new Properties();
              properties.load(props);
              consumer = new KafkaConsumer<>(properties);
          }
      ```
      - The following is the content of the used properties file [consumer.properties](consumer/src/main/resources/consumer.properties)
           ```properties
                bootstrap.servers=localhost:9092
                key.deserializer=org.apache.kafka.common.serialization.StringDeserializer
                value.deserializer=com.utils.JavaDeserializer
                group.id=grp1
                auto.offset.reset=earliest
           ```
      - Notice that we use a custom (value) deserializer (see [com.utils.JavaDeserializer](consumer/src/main/java/com/utils/JavaDeserializer.java)) to deserialize Java Objects
           - The custom deserializer is specified in [com.utils.JavaDeserializer](consumer/src/main/java/com/utils/JavaDeserializer.java) with: _value.deserializer=com.utils.JavaDeserializer_
        
    - Subscribe to two topics: _"gaze-events"_ and _"click-events"_. The events in the _"gaze-events"_ topic come from **two partitions**, while the events in the _"click-events"_ come from **one partition only**
    ```Java
      // Subscribe to relevant topics
      consumer.subscribe(Arrays.asList("gaze-events","click-events"));
      ```
    
    - Poll new events at specific rate and process consumer records
    ```Java
            // pool new data
            ConsumerRecords<String, Object> records = consumer.poll(Duration.ofMillis(8));

            // process consumer records depending on record.topic() and record.value()
            for (ConsumerRecord<String, Object> record : records) {
                // switch/case
                switch (record.topic()) {
                    //note: record.value() is a linkedHashMap (see utils.JavaDeserializer), use can use the following syntax to access specific attributes ((LinkedHashMap) record.value()).get("ATTRIBUTENAME").toString(); The object can be also reconstructed as Gaze object
                    case "gaze-events":
                        String value =   record.value().toString();
                        System.out.println("Received gaze-events - key: " + record.key() +"- value: " + value + "- partition: "+record.partition());
                        break;

                    case "click-events":
                        System.out.println("Received click-events - value: " + record.value()+ "- partition: "+record.partition());

                        break;

                    default:
                        throw new IllegalStateException("Shouldn't be possible to get message on topic " + record.topic());
                }
            }
     ```

#### Instruction

- Examine the control-flow within the class [ConsumerForAllEvents](consumer/src/main/java/com/examples/ConsumerForAllEvents.java)

### ConsumerForGazeEventsForSingleEyeTracker

- Prerequisite for running [ConsumerForGazeEventsForSingleEyeTracker](consumer/src/main/java/com/examples/ConsumerForGazeEventsForSingleEyeTracker.java): 
  - Stop all previously running producers and consumers
  - (Re)Run [EyeTrackers-Producer](EyeTrackers-Producer/) (Main Class [com.examples.EyeTrackersProducer](EyeTrackers-Producer/src/main/java/com/examples/EyeTrackersProducer.java))


- Main Class: [com.examples.ConsumerForGazeEventsForSingleEyeTracker](consumer/src/main/java/com/examples/ConsumerForGazeEventsForSingleEyeTracker.java)
  - **Overview:** consumes the events coming from a single eye-tracker
  - **Procedure (#P4)**:
    - The procedure is similar to Procedure [P3](#P3), with the following difference:
      - This consumer consumes the events coming from a **single** eye-tracker _(deviceID: 0)_ (These events were stored in _partition "0"_ within the _"gaze-events"_ topic)
        - This is specified using the following code fragment
         ```Java
            // Read specific topic and partition
            TopicPartition topicPartition = new TopicPartition("gaze-events", 0);
            consumer.assign(Arrays.asList(topicPartition));
            ```

#### Instruction

- Examine the control-flow within the class [com.examples.ConsumerForGazeEventsForSingleEyeTracker](consumer/src/main/java/com/examples/ConsumerForGazeEventsForSingleEyeTracker.java)

### ConsumerCustomOffset

- Prerequisite for running [ConsumerCustomOffset](consumer/src/main/java/com/examples/ConsumerCustomOffset.java): 
  - Stop all previously running producers and consumers
  - (Re)Run [ClickStream-Producer](ClickStream-Producer/) (Main Class [com.examples.ClicksProducer](ClickStream-Producer/src/main/java/com/examples/ClicksProducer.java))


- Main Class: [com.examples.ConsumerCustomOffset](consumer/src/main/java/com/examples/ConsumerCustomOffset.java)
  - **Overview:** consumes the events coming from [ClickStream-Producer](ClickStream-Producer/) starting from a specific user-defined offset
  - **Procedure (#P5):**
    - The procedure is similar to Procedure [P4](#P4), with the following difference:
      - The consumer is subscribed to the topic _"click-events"_
      - The consumer starts reading events from a _specific user-defined offset_ (i.e., _int offsetToReadFrom_)
        - This is specified using the following code fragment
        ```Java
        // reading from a specific user defined offset
        int offsetToReadFrom = 5;
        consumer.seek(topicPartition, offsetToReadFrom);
           ```

#### Instruction

- Examine the control-flow within the class [com.examples.ConsumerCustomOffset](consumer/src/main/java/com/examples/ConsumerCustomOffset.java). 
- Stop all previously running producers and consumers and (re)run [ClickStream-Producer](ClickStream-Producer/) (Main Class [com.examples.ClicksProducer](ClickStream-Producer/src/main/java/com/examples/ClicksProducer.java)) (prerequisite)
- Wait for the producer to produce more than 5 events (e.g., 10 events)
- Run [ConsumerCustomOffset](consumer/src/main/java/com/examples/ConsumerCustomOffset.java)
- Check the console of [ConsumerCustomOffset](consumer/src/main/java/com/examples/ConsumerCustomOffset.java), what do you notice?

### rebalancingExample.*

- Prerequisite for running the classes within the package [rebalancingExample](consumer/src/main/java/com/examples/rebalancingExample/): 
  - Stop all previously running producers and consumers
  - (Re)Run [EyeTrackers-Producer](EyeTrackers-Producer/) (Main Class [com.examples.EyeTrackersProducer](EyeTrackers-Producer/src/main/java/com/examples/EyeTrackersProducer.java))


- **Overview:** The package contains two (main) classes [ConsumerForGazeEventsForEyeTrackerParitionsRebalancing1](consumer/src/main/java/com/examples/rebalancingExample/ConsumerForGazeEventsForEyeTrackerParitionsRebalancing1.java) and [ConsumerForGazeEventsForEyeTrackerParitionsRebalancing2](consumer/src/main/java/com/examples/rebalancingExample/ConsumerForGazeEventsForEyeTrackerParitionsRebalancing2.java). These classes demonstrate how Kafka does rebalancing when a new consumer is added (within the same consumer group)
- For the sake of simulation, the classes [ConsumerForGazeEventsForEyeTrackerParitionsRebalancing1](consumer/src/main/java/com/examples/rebalancingExample/ConsumerForGazeEventsForEyeTrackerParitionsRebalancing1.java) and [ConsumerForGazeEventsForEyeTrackerParitionsRebalancing2](consumer/src/main/java/com/examples/rebalancingExample/ConsumerForGazeEventsForEyeTrackerParitionsRebalancing2.java) have duplicate code.
- **Procedure (#P6):** In both classes, a Kafka consumer is created with the properties in [consumer.properties](consumer/src/main/resources/consumer.properties), subscribed to the topic _"gaze-events"_ and prints the received events to the console
- Remember that the topic _"gaze-events"_ has **two partitions** (referring to the two eye-trackers)

#### Instructions

- Stop all previously running producers and consumers and (re)run [EyeTrackers-Producer](EyeTrackers-Producer/) (Main Class [com.examples.EyeTrackersProducer](EyeTrackers-Producer/src/main/java/com/examples/EyeTrackersProducer.java)) (prerequisite)
- Run [ConsumerForGazeEventsForEyeTrackerParitionsRebalancing1](consumer/src/main/java/com/examples/rebalancingExample/ConsumerForGazeEventsForEyeTrackerParitionsRebalancing1.java)
  - Check that the consumer consumes and prints to the console the events belonging to both Partition 0 and Partition 1 of the _"gaze-events"_ topic
- Run [ConsumerForGazeEventsForEyeTrackerParitionsRebalancing2](consumer/src/main/java/com/examples/rebalancingExample/ConsumerForGazeEventsForEyeTrackerParitionsRebalancing2.java)
  - Check that each consumer will start consuming and printing to the console the events of a single partition only
  


### singleAcessToPartitionAndRebalancingExample.*


- Prerequisite for running the classes within the package [singleAcessToPartitionAndRebalancingExample](consumer/src/main/java/com/examples/singleAcessToPartitionAndRebalancingExample/): 
  - Stop all previously running producers and consumers
  - (Re)Run [ClickStream-Producer](ClickStream-Producer/) (Main Class [com.examples.ClicksProducer](ClickStream-Producer/src/main/java/com/examples/ClicksProducer.java))


- **Overview:** The package contains two (main) classes [singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly1](consumer/src/main/java/com/examples/singleAcessToPartitionAndRebalancingExample/ConsumerForClickEventsOnly1.java) and [singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly2](consumer/src/main/java/com/examples/singleAcessToPartitionAndRebalancingExample/ConsumerForClickEventsOnly2.java). These classes demonstrate how Kafka allows only one consumer (within a consumer group) to read from a partition and how rebalancing occurs when a running consumer is out
- For the sake of simulation, the classes [singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly1](consumer/src/main/java/com/examples/singleAcessToPartitionAndRebalancingExample/ConsumerForClickEventsOnly1.java) and [singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly2](consumer/src/main/java/com/examples/singleAcessToPartitionAndRebalancingExample/ConsumerForClickEventsOnly2.java) have duplicate code.
- **Procedure (#P7):** In both classes, a Kafka consumer is created, subscribed to the topic _"click-events"_ and prints the received events to the console
- Remember that the topic _"click-events"_ has **1 partition only** and so this partition can be read by only one consumer within a consumer group

#### Instructions

- Stop all previously running producers and consumers and (re)run [ClickStream-Producer](ClickStream-Producer/) (Main Class [com.examples.ClicksProducer](ClickStream-Producer/src/main/java/com/examples/ClicksProducer.java)) (prerequisite)
- Run [singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly1](consumer/src/main/java/com/examples/singleAcessToPartitionAndRebalancingExample/ConsumerForClickEventsOnly1.java)
  - Check that the consumer consumes and prints to the console the events of the _"click-events"_ topic.
- Run [singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly2](consumer/src/main/java/com/examples/singleAcessToPartitionAndRebalancingExample/ConsumerForClickEventsOnly2.java)
  - Remember that the topic _"click-events"_ has **1 partition only** and so this partition can be read by only one consumer within a consumer group
  - Check that only one of the two consumers will keep consuming the events while the other consumer will stay idle
- Stop the non-idle consumer _i.e., the one still consuming events_
  - Check that the other consumer will take over and start consuming events after a while
- Assuming that the click events have an incremental eventID, by comparing the output consoles of [singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly1](consumer/src/main/java/com/examples/singleAcessToPartitionAndRebalancingExample/ConsumerForClickEventsOnly1.java) and [singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly2](consumer/src/main/java/com/examples/singleAcessToPartitionAndRebalancingExample/ConsumerForClickEventsOnly2.java), check whether any of the events are duplicated or lost


### customCommit.singleAcessToPartitionAndRebalancingExample.*


- Prerequisite for running the classes within the package [customCommit.singleAcessToPartitionAndRebalancingExample](consumer/src/main/java/com/examples/customCommit/singleAcessToPartitionAndRebalancingExample): 
  - Stop all previously running producers and consumers
  - (Re)Run [ClickStream-Producer](ClickStream-Producer/) (Main Class [com.examples.ClicksProducer](ClickStream-Producer/src/main/java/com/examples/ClicksProducer.java))


- **Overview:** The package contains two (main) classes [customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly1](consumer/src/main/java/com/examples/customCommit/singleAcessToPartitionAndRebalancingExample/ConsumerForClickEventsOnly1.java) and [customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly2](consumer/src/main/java/com/examples/customCommit/singleAcessToPartitionAndRebalancingExample/ConsumerForClickEventsOnly2.java). These classes demonstrate in the context of manual offset commit (occuring after every n consumer polls) the impact of rebalancing on events duplication and loss
- For the sake of simulation, the classes [customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly1](consumer/src/main/java/com/examples/customCommit/singleAcessToPartitionAndRebalancingExample/ConsumerForClickEventsOnly1.java) and [customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly2](consumer/src/main/java/com/examples/customCommit/singleAcessToPartitionAndRebalancingExample/ConsumerForClickEventsOnly2.java) have duplicate code.
- **Procedure (#P8):** 
  - In this example, we use a different configuration for the consumer properties which is available in [resources/consumerCustomCommit.properties](consumer/src/main/resources/consumerCustomCommit.properties)
    - What is different in this configuration is that we set _"enable.auto.commit=false"_ to disable automatic offset commits and so allow trying out the manual offset commits
  - In both classes, a Kafka consumer is created, subscribed to the topic _"click-events"_ and prints the received events to the console
  - In both Classes [customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly1](consumer/src/main/java/com/examples/customCommit/singleAcessToPartitionAndRebalancingExample/ConsumerForClickEventsOnly1.java) and [customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly2](consumer/src/main/java/com/examples/customCommit/singleAcessToPartitionAndRebalancingExample/ConsumerForClickEventsOnly2.java), everytime consumer.poll() is called and the records are processed, a manual synchronized commit is done using the following code fragment
    ```Java
              try {
                  consumer.commitSync();
                  System.out.println("commit sync done");
              } catch (CommitFailedException e) {
                 System.err.println("commit failed"+ e);
              }
         ```
  - Remember that the topic _"click-events"_ has **only 1 partition** and so this partition **can be read by only one consumer** within a consumer group

#### Instructions

- Stop all previously running producers and consumers and (re)run [ClickStream-Producer](ClickStream-Producer/) (Main Class [com.examples.ClicksProducer](ClickStream-Producer/src/main/java/com/examples/ClicksProducer.java)) (prerequisite)
- Run  [customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly1](consumer/src/main/java/com/examples/customCommit/singleAcessToPartitionAndRebalancingExample/ConsumerForClickEventsOnly1.java)
  - Check that the consumer consumes and prints to the console the events of the _"click-events"_ topic.
- Run  [customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly2](consumer/src/main/java/com/examples/customCommit/singleAcessToPartitionAndRebalancingExample/ConsumerForClickEventsOnly2.java)
  - Remember that the topic _"click-events"_ has **only 1 partition** and so this partition **can be read by only one consumer** within a consumer group
  - Check that only one of the two consumers will keep consuming the events while the other consumer will stay idle
- Stop the non-idle consumer _i.e., the one still consuming events_
  - Check that the other consumer will take over and start consuming events after a while
- Assuming that the click events have an incremental eventID, by comparing the output consoles of [customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly1](consumer/src/main/java/com/examples/customCommit/singleAcessToPartitionAndRebalancingExample/ConsumerForClickEventsOnly1.java) and [customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly2](consumer/src/main/java/com/examples/customCommit/singleAcessToPartitionAndRebalancingExample/ConsumerForClickEventsOnly2.java), check whether any of the events are duplicated or lost
- Edit the code in both  [customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly1](consumer/src/main/java/com/examples/customCommit/singleAcessToPartitionAndRebalancingExample/ConsumerForClickEventsOnly1.java) and [customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly2](consumer/src/main/java/com/examples/customCommit/singleAcessToPartitionAndRebalancingExample/ConsumerForClickEventsOnly2.java) so that the manual offset commit is executed after each 40 consumer polls, repeat the previous instructions, check whether any of the events are duplicated or lost



### customCommit.commitLargerOffset.*


- Prerequisite for running the classes within the package [customCommit.commitLargerOffset](consumer/src/main/java/com/examples/customCommit/commitLargerOffset): 
  - Stop all previously running producers and consumers
  - (Re)Run [ClickStream-Producer](ClickStream-Producer/) (Main Class [com.examples.ClicksProducer](ClickStream-Producer/src/main/java/com/examples/ClicksProducer.java)) 
  

- **Overview:** The package contains two (main) classes two classes [customCommit.commitLargerOffset.ConsumerForClickEventsOnly1](consumer/src/main/java/com/examples/customCommit/commitLargerOffset/ConsumerForClickEventsOnly1.java) and [customCommit.commitLargerOffset.ConsumerForClickEventsOnly2](consumer/src/main/java/com/examples/customCommit/commitLargerOffset/ConsumerForClickEventsOnly2.java). These classes demonstrate events loss when a manual offset that is larger than the offset of the latest processed event is set
- For the sake of simulation, the classes [customCommit.commitLargerOffset.ConsumerForClickEventsOnly1](consumer/src/main/java/com/examples/customCommit/commitLargerOffset/ConsumerForClickEventsOnly1.java) and [customCommit.commitLargerOffset.ConsumerForClickEventsOnly2](consumer/src/main/java/com/examples/customCommit/commitLargerOffset/ConsumerForClickEventsOnly2.java) have duplicate code.
- **Procedure (#P09):**
    - Similar to Procedure [P8](#P8), in this example, we use the configuration in [resources/consumerCustomCommit.properties](consumer/src/main/resources/consumerCustomCommit.properties)
      - This configuration sets _"enable.auto.commit=false"_ to disable automatic offset commits and so allow trying out the manual offset commits
    - In both classes, a Kafka consumer is created, subscribed to the topic _"click-events"_ and prints the received events to the console
    - In both Classes [customCommit.commitLargerOffset.ConsumerForClickEventsOnly1](consumer/src/main/java/com/examples/customCommit/commitLargerOffset/ConsumerForClickEventsOnly1.java) and [customCommit.commitLargerOffset.ConsumerForClickEventsOnly2](consumer/src/main/java/com/examples/customCommit/commitLargerOffset/ConsumerForClickEventsOnly2.java), everytime a record is processed, a manual synchronized commit is done using the following code fragment. This code fragment, commits an offset which correspond to the current _record.offset() + 10_
      ```Java
         currentOffsets.put(
             new TopicPartition(record.topic(), record.partition()),
             new OffsetAndMetadata(record.offset() + 10, "no metadata"));

         consumer.commitSync(currentOffsets);
           ```
    - Remember that the topic _"click-events"_ has only **1 partition** and so this partition **can be read by only one consumer** within a consumer group

#### Instructions

- Stop all previously running producers and consumers and (re)run [ClickStream-Producer](ClickStream-Producer/) (Main Class [com.examples.ClicksProducer](ClickStream-Producer/src/main/java/com/examples/ClicksProducer.java)) (prerequisite)
- Run [customCommit.commitLargerOffset.ConsumerForClickEventsOnly1](consumer/src/main/java/com/examples/customCommit/commitLargerOffset/ConsumerForClickEventsOnly1.java)
  - Check that the consumer consumes and prints to the console the events of the _"click-events"_ topic.
- Run [customCommit.commitLargerOffset.ConsumerForClickEventsOnly2](consumer/src/main/java/com/examples/customCommit/commitLargerOffset/ConsumerForClickEventsOnly2.java)
  - Remember that the topic _"click-events"_ has **only 1 partition** and so this partition **can be read by only one consumer** within a consumer group
  - Check that only one of the two consumers will keep consuming the events while the other consumer will stay idle
- Stop the non-idle consumer _i.e., the one still consuming events_
  - Check that the other consumer will take over and start consuming events after a while
- Assuming that the click events have an incremental eventID, by comparing the output consoles of [customCommit.commitLargerOffset.ConsumerForClickEventsOnly1](consumer/src/main/java/com/examples/customCommit/commitLargerOffset/ConsumerForClickEventsOnly1.java) and [customCommit.commitLargerOffset.ConsumerForClickEventsOnly2](consumer/src/main/java/com/examples/customCommit/commitLargerOffset/ConsumerForClickEventsOnly2.java), check whether some events are lost. What do you notice?









