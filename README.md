# lab02Part2-kafka-EyeTracking

- This lab is based on Lab02Part1, which is also a prerequisite for this lab
- Note that only the new concepts are described in this lab
- The procedure to run the code is similar to Lab02. We recommend importing the project to  IntelliJ and let the IDE handle everything

## Use case

- This lab simulates a system where eye-tracking data coming from two eye-trackers and user clicks are streamed
- The eye-tracking data captures the gazes of two developers doing pair programming
- We use Kafka producers and consumers to simulate this system

## Overview

This lab consists of two parts.

In the first part, we create two producers (ClickStream-Producer and EyeTrackers-Producer)
- _"ClickStream-Producer"_ Module: produces click stream data
- _"EyeTrackers-Producer"_ Module: produces gaze data


In the second part, we consume the messages of the producers using consumers with different configurations. All the consumers are available in the "consumer" Module, within the Package "com.examples"
- _"ConsumerForAllEvents"_: consumes the events coming from both "ClickStream-Producer" and "EyeTrackers-Producer"
- _"ConsumerForGazeEventsForSingleEyeTracker"_: consumes the events coming from a single eye-tracker
- _"ConsumerForGazeEventsForSingleEyeTrackerCustomOffset"_: consumes the events coming from a single eye-tracker and starts from a specific user-defined offset
- _"reblancingExample.*"_: two classes that demonstrate how Kafka does rebalancing when a new consumer is added (within the same group)
- _"singleAcessToPartitionAndRebalancingExample.*"_: two classes demonstrating how Kafka allows only one consumer to read from a partition and how rebalancing occurs when a running consumer is out
- _"customCommit.singleAcessToPartitionAndRebalancingExample.*"_: two classes demonstrating in the context of manual offset commit (occuring after every n consumer polls) the impact of rebalancing on events duplication and loss
- _"customCommit.commitLargerOffset.*"_: two classes demonstrating events loss when a manual offset that is larger than the offset of the latest processed events in set

## Producers

### "ClickStream-Producer"

- Main class: com.examples.ClicksProducer
  - This producer produces clicks objects (see com.data.Clicks) at a random rate in range [500ms, 5000ms] and send them as events through the "click-events" topic
  - We use a custom serializer to serialize the events before sending them (see com.utils.JavaSerializer)
    - The custom serializer is specified in resources/producer.properties (value.serializer=com.utils.JavaSerializer)
  - We use a single partition inside the "click-events" topic so that all the events will be stored into that unique partition of the "click-events" topic
    - Note: for simulation purpose, every time you run "ClicksProducer", the existing "click-events" topic is deleted and a new topic with the same name is created.


- Main class: com.examples.DescribeTopics
  - The class provides an overview on the existing Kafka topics and the underlying partitions. It is based on the Kafka AdminClient Class (for documentation see: https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/admin/AdminClient.html)

#### Instruction

- Explore the different classes within "ClickStream-Producer" and check the control-flow within the class "com.examples.ClicksProducer".

### "EyeTrackers-Producer"

- Main class: com.examples.EyeTrackersProducer
  - This producer produces gaze objects (see com.data.Gaze) at a specific rate (8ms) and send them as events through the "gaze-events" topic
  - We use a custom serializer to serialize the events before sending them (see com.utils.JavaSerializer)
    - The custom serializer is specified in resources/producer.properties (value.serializer=com.utils.JavaSerializer)
  - Each gaze object is assigned an event key (deviceID: 0 or 1) referring to whether it comes from the first eye-tracker or the second one
  - We use a custom partitioner, so that the events coming from each eye-tracker are always stored into the same distinct partition (see com.utils.CustomPartitioner)
    - Reason: with the default partitioner, Kafka guarantees that events with the same key will go to the same partition, but not the other way around i.e., events with different keys will go always to different partitions. Knowing that events are assigned to partitions as follows: "partitionID = hash(key)%num_partitions", with a low partition number (e.g., 2), it is very likely that 2 events with different keys will still go to the same partition.
    - The custom partitioner is specified in resources/producer.properties (com.utils.CustomPartitioner)
  - Note: for simulation purpose, every time you run "EyeTrackersProducer", the existing "gaze-events" topic is deleted and a new topic with the same name is created.


- Main class: com.examples.DescribeTopics
  - The class provides an overview on the existing Kafka topics and the underlying partitions. It is based on the Kafka AdminClient Class (for documentation see: https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/admin/AdminClient.html)

#### Instruction

- Explore the different classes within "EyeTrackers-Producer" and check the control-flow within the class "com.examples.EyeTrackersProducer".

## Consumers

### "ConsumerForAllEvents" 

- Prerequisite for running: both "EyeTrackers-Producer" (Class "com.examples.EyeTrackersProducer") and "ClickStream-Producer"  (Class "com.examples.ClicksProducer") are running


- Main class: com.examples.ConsumerForAllEvents 
    - This consumer consumes the events coming from both "ClickStream-Producer" and "EyeTrackers-Producer"
    - We use a custom deserializer to deserialize the events when received (see com.utils.JavaSerializer)
      - The custom deserializer is specified in ressources/consumer.properties (value.deserializer=com.utils.JavaDeserializer)
    - The consumer subscribe to two topics: "gaze-events" and "click-events"
    - The events in the "gaze-events" topic come from two partitions, while the events in the "click-events" come from one partition only

#### Instruction

- Check the control-flow within the class "com.examples.ConsumerForAllEvents".

### "ConsumerForGazeEventsForSingleEyeTracker": 

- Prerequisite for running: "EyeTrackers-Producer" (Class "com.examples.EyeTrackersProducer") is running


- Main class: com.examples.ConsumerForGazeEventsForSingleEyeTracker
  - This consumer consumes the events coming from a single eye-tracker (deviceID: 0) (These events were stored in partition "0" within the "gaze-events" topic)
    - This is specified using the following code fragment
    ```Java
        // Read specific topic and partition
        TopicPartition topicPartition = new TopicPartition("gaze-events", 0);
        consumer.assign(Arrays.asList(topicPartition));
       ```

#### Instruction

- Check the control-flow within the class "com.examples.ConsumerForGazeEventsForSingleEyeTracker".

### "ConsumerForGazeEventsForSingleEyeTrackerCustomOffset":

- Prerequisite for running: "EyeTrackers-Producer" (Class "com.examples.EyeTrackersProducer") is running


- Main class: com.examples.ConsumerForGazeEventsForSingleEyeTrackerCustomOffset
  - This consumer consumes the events coming from a single eye-tracker (deviceID: 0) (These events were stored in partition "0" within the "gaze-events" topic)
  - In addition, the consumer starts reading events from a specific user-defined offset (i.e., int offsetToReadFrom)
    - This is specified using the following code fragment
    ```Java
        // get consumer latest offset
        long latestoffset = consumer.position(topicPartition);
        System.out.println("latest offset: "+latestoffset);

        // seek to offsetToReadFrom
        int offsetToReadFrom = 10;
        // check that offsetToReadFrom<latestoffset
        if(offsetToReadFrom<latestoffset) {
            consumer.seek(topicPartition, offsetToReadFrom);
        }
       else {
            System.err.println("offsetToReadFrom ("+offsetToReadFrom+") not yet reached");
        }
       ```

#### Instruction

- Check the control-flow within the class "com.examples.ConsumerForGazeEventsForSingleEyeTrackerCustomOffset". Try out different values in offsetToReadFrom


### "reblancingExample.*": 

- Prerequisite for running: "EyeTrackers-Producer" (Class "com.examples.EyeTrackersProducer") is running


- The package contains two (main) classes that demonstrate how Kafka does rebalancing when a new consumer is added (within the same group)
- For the sake of simulation, the classes "reblancingExample.ConsumerForGazeEventsForEyeTrackerParitionsRebalancing1" and "ReblancingExample.ConsumerForGazeEventsForEyeTrackerParitionsRebalancing2" have duplicate code.
- In both classes, a Kafka consumer is created, subscribed to the topic "gaze-events" and print the received events to the console
- Remember that the topic "gaze-events" has two partitions (referring to the two eye-trackers)

#### Instructions

- Ensure that "EyeTrackers-Producer" (Class "com.examples.EyeTrackersProducer") is running (prerequisite)
- Run "reblancingExample.ConsumerForGazeEventsForEyeTrackerParitionsRebalancing1"
  - Check that the consumer consumes and prints to the console the events belonging to both Partition 0 and Partition 1 of the "gaze-events" topic
- Run "reblancingExample.ConsumerForGazeEventsForEyeTrackerParitionsRebalancing2"
  - Check that "reblancingExample.ConsumerForGazeEventsForEyeTrackerParitionsRebalancing1" will start consuming and printing to the console the events of a single partition only
  - Check that "reblancingExample.ConsumerForGazeEventsForEyeTrackerParitionsRebalancing2" will start consuming and printing to the console the events of the other partition


### "singleAcessToPartitionAndRebalancingExample.*"

- Prerequisite for running: "ClickStream-Producer" (Class "com.examples.ClicksProducer") is running 


- The package contains two (main) classes demonstrating how Kafka allows only one consumer (within a consumer group) to read from a partition and how rebalancing occurs when a running consumer is out
- For the sake of simulation, the classes "singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly1" and "singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly2" have duplicate code.
- In both classes, a Kafka consumer is created, subscribed to the topic "click-events" and print the received events to the console
- Remember that the topic "click-events" has only 1 partition and so this partition can be read by only one consumer within a consumer group

#### Instructions

- Ensure that "ClickStream-Producer" (Class "com.examples.ClicksProducer") is running (prerequisite)
- Run "singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly1"
  - Check that the consumer consumes and prints to the console the events of the "click-events" topic.
- Run "singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly2"
  - Remember that the topic "click-events" has only 1 partition and so this partition can be read by only one consumer within a consumer group
  - Check that only one of the two consumers will keep consuming the events while the other consumer will stay idle
- Stop the non-idle consumer i.e., the one still consuming events
  - Check that the other consumer will take over and start consuming events
- Assuming that the click events have an incremental eventID, by comparing the output consoles of "singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly1" and "singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly2", check whether any of the events are duplicated or lost


### "customCommit.singleAcessToPartitionAndRebalancingExample.*"


- Prerequisite for running: "ClickStream-Producer" (Class "com.examples.ClicksProducer") is running


- The package contains two (main) classes demonstrating in the context of manual offset commit (occuring after every n consumer polls) the impact of rebalancing on events duplication and loss
- For the sake of simulation, the classes "customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly1" and "customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly2" have duplicate code.
- In both classes, a Kafka consumer is created, subscribed to the topic "click-events" and print the received events to the console
- In this example, we use a different configuration for the consumer properties which is available in "resources/consumerCustomCommit.properties"
  - What is different in this configuration is that we set "enable.auto.commit=false" to disable automatic offset commits and so try out the manual offset commits
- In both Classes "customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly1" and "customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly2", every time consumer.poll() is called and the records are read, a manual synchronized commit is done using the following code fragment
  -  ```Java
            try {
                consumer.commitSync();
                System.out.println("commit sync done");
            } catch (CommitFailedException e) {
               System.err.println("commit failed"+ e);
            }
       ```
- Remember that the topic "click-events" has only 1 partition and so this partition can be read by only one consumer within a consumer group

#### Instructions

- Ensure that "ClickStream-Producer" (Class "com.examples.ClicksProducer") is running (prerequisite)
- Run "customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly1"
  - Check that the consumer consumes and prints to the console the events of the "click-events" topic.
- Run "customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly2"
  - Remember that the topic "click-events" has only 1 partition and so this partition can be read by only one consumer within a consumer group
  - Check that only one of the two consumers will keep consuming the events while the other consumer will stay idle
- Stop the non-idle consumer i.e., the one still consuming events
  - Check that the other consumer will take over and start consuming events
- Assuming that the click events have an incremental eventID, by comparing the output consoles of "customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly1 and "customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly2", check whether any of the events are duplicated or lost
- Edit the code in both  "customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly1 and "customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly2" so that the manual offset commit is executed after each 10 consumer polls, repeat the previous instructions, check whether any of the events are duplicated or lost



### "customCommit.commitLargerOffset.*"

- Prerequisite for running: "ClickStream-Producer" (Class "com.examples.ClicksProducer") is running


- The package contains two (main) classes two classes demonstrating events loss when a manual offset that is larger than the offset of the latest processed events in set
- For the sake of simulation, the classes "customCommit.commitLargerOffset.ConsumerForClickEventsOnly1" and "customCommit.commitLargerOffset.ConsumerForClickEventsOnly2" have duplicate code.
- In both classes, a Kafka consumer is created, subscribed to the topic "click-events" and print the received events to the console
- In this example, we use the configuration in "resources/consumerCustomCommit.properties"
  - This configuration sets "enable.auto.commit=false" to disable automatic offset commits and so try out the manual offset commits
- In both Classes "customCommit.commitLargerOffset.ConsumerForClickEventsOnly1" and "customCommit.commitLargerOffset.ConsumerForClickEventsOnly2", every time a record is read, a manual synchronized commit is done using the following code fragment
  - ```Java
            currentOffsets.put(
                        new TopicPartition(record.topic(), record.partition()),
                        new OffsetAndMetadata(record.offset() + 10, "no metadata"));

                consumer.commitSync(currentOffsets);
       ```
  - This code fragments, commits an offset which correspond to the current record.offset() + 10
  
- Remember that the topic "click-events" has only 1 partition and so this partition can be read by only one consumer within a consumer group

#### Instructions

- Ensure that "ClickStream-Producer" (Class "com.examples.ClicksProducer") is running (prerequisite)
- Run "customCommit.commitLargerOffset.ConsumerForClickEventsOnly1"
  - Check that the consumer consumes and prints to the console the events of the "click-events" topic.
- Run "customCommit.commitLargerOffset.ConsumerForClickEventsOnly2"
  - Remember that the topic "click-events" has only 1 partition and so this partition can be read by only one consumer within a consumer group
  - Check that only one of the two consumers will keep consuming the events while the other consumer will stay idle
- Stop the non-idle consumer i.e., the one still consuming events
  - Check that the other consumer will take over and start consuming events
- Assuming that the click events have an incremental eventID, by comparing the output consoles of "customCommit.commitLargerOffset.ConsumerForClickEventsOnly1 and "customCommit.commitLargerOffset.ConsumerForClickEventsOnly2", check whether some events are lost









