# Lab02Part2-kafka-EyeTracking

- This lab is based on the first part of Lab02 (https://github.com/scs-edpo/lab02Part1-kafka-producer-consumer), which is also a prerequisite for this lab
- The procedure to run the code is similar to Lab02. We recommend importing the project to  IntelliJ and let the IDE handle everything
- Note that only the new procedures and concepts are described in this lab

## Use case

- This lab simulates a system where eye-tracking data coming from two eye-trackers and user clicks are streamed
- The eye-tracking data captures the gazes of two developers doing pair programming
- We use Kafka producers and consumers to simulate this system

## Overview

This lab consists of two parts.

In the first part, we create two producers (ClickStream-Producer and EyeTrackers-Producer)
-  [_ClickStream-Producer_ Module](ClickStream-Producer/): produces click stream data
-  [_EyeTrackers-Producer_ Module](EyeTrackers-Producer/): produces gaze data

In the second part, we consume the messages of the producers using consumers with different configurations. All the consumers are available in the ["consumer"](consumer/) Module, within the Package ["com.examples"](consumer/src/main/java/com/examples)
- [_ConsumerForAllEvents_](consumer/src/main/java/com/examples/ConsumerForAllEvents.java): consumes the events coming from both [ClickStream-Producer](ClickStream-Producer/) and [EyeTrackers-Producer](EyeTrackers-Producer/)
- [_ConsumerForGazeEventsForSingleEyeTracker_](consumer/src/main/java/com/examples/ConsumerForGazeEventsForSingleEyeTracker.java): consumes the events coming from a single eye-tracker
- [_ConsumerForGazeEventsForSingleEyeTrackerCustomOffset_](consumer/src/main/java/com/examples/ConsumerForGazeEventsForSingleEyeTrackerCustomOffset.java): consumes the events coming from a single eye-tracker and starts from a specific user-defined offset
- [_rebalancingExample.*_](consumer/src/main/java/com/examples/rebalancingExample/): two classes that demonstrate how Kafka does rebalancing when a new consumer is added (within the same group)
- [_singleAcessToPartitionAndRebalancingExample.*_](consumer/src/main/java/com/examples/singleAcessToPartitionAndRebalancingExample/): two classes demonstrating how Kafka allows only one consumer to read from a partition and how rebalancing occurs when a running consumer is out
- [_customCommit.singleAcessToPartitionAndRebalancingExample.*_](consumer/src/main/java/com/examples/customCommit/singleAcessToPartitionAndRebalancingExample/): two classes demonstrating in the context of manual offset commit (occuring after every n consumer polls) the impact of rebalancing on events duplication and loss
- [_customCommit.commitLargerOffset.*_](consumer/src/main/java/com/examples/customCommit/commitLargerOffset/): two classes demonstrating events loss when a manual offset that is larger than the offset of the latest processed events in set

## Producers

### ClickStream-Producer

- Main Class:  [com.examples.ClicksProducer](ClickStream-Producer/src/main/java/com/examples/ClicksProducer.java)
  - This producer produces clicks objects (see [com.data.Clicks](ClickStream-Producer/src/main/java/com/data/Clicks.java)) at a random time interval in range _[500ms, 5000ms]_ and send them as events through the "click-events" topic
  - We use a custom serializer to serialize the events before sending them (see [com.utils.JavaSerializer](ClickStream-Producer/src/main/java/com/utils/JavaSerializer.java))
    - The custom serializer is specified in [resources/producer.properties](ClickStream-Producer/src/main/resources/producer.properties) (_value.serializer=com.utils.JavaSerializer_)
  - We use a **single partition** inside the _"click-events"_ topic so that all the events will be stored into that unique partition of the _"click-events"_ topic
  - _Note:_ for simulation purpose, every time you run [com.examples.ClicksProducer](ClickStream-Producer/src/main/java/com/examples/ClicksProducer.java), the existing _"click-events"_ topic is deleted and a new topic with the same name is created.


- Main Class: [com.examples.DescribeTopics](ClickStream-Producer/src/main/java/com/examples/DescribeTopics.java) 
  - The class provides an overview on the existing Kafka topics and the underlying partitions. It is based on the Kafka AdminClient Class (for documentation see: https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/admin/AdminClient.html)

#### Instruction

- Explore the different classes within [ClickStream-Producer](ClickStream-Producer/src/main/) and check the control-flow within the class ["com.examples.ClicksProducer"](ClickStream-Producer/src/main/java/com/examples/ClicksProducer.java)

### EyeTrackers-Producer

- Main Class: [com.examples.EyeTrackersProducer](EyeTrackers-Producer/src/main/java/com/examples/EyeTrackersProducer.java)
  - This producer produces gaze objects (see [com.data.Gaze](EyeTrackers-Producer/src/main/java/com/data/Gaze.java)) at a specific rate _(8ms)_ and send them as events through the _"gaze-events"_ topic
  - We use a custom serializer to serialize the events before sending them (see [com.utils.JavaSerializer](EyeTrackers-Producer/src/main/java/com/utils/JavaSerializer.java))
    - The custom serializer is specified in [resources/producer.properties](EyeTrackers-Producer/src/main/resources/producer.properties) _(value.serializer=com.utils.JavaSerializer)_
  - Each gaze object is assigned an event key _(deviceID: 0 or 1)_ referring to whether it comes from the first eye-tracker or the second one
  - We use a **custom partitioner**, so that the events coming from each eye-tracker **are always stored into the same distinct partition** (see [com.utils.CustomPartitioner](EyeTrackers-Producer/src/main/java/com/utils/CustomPartitioner.java))
    - _Reason:_ with the default partitioner, Kafka guarantees that events with the same key will go to the same partition, but not the other way around i.e., events with different keys will go always to different partitions. Knowing that events are assigned to partitions as follows: _"partitionID = hash(key)%num_partitions"_, with a low partition number (e.g., _num_partitions=2_), it is very likely that 2 events with different keys will still go to the same partition.
    - The custom partitioner is specified in [resources/producer.properties](EyeTrackers-Producer/src/main/resources/producer.properties) _(partitioner.class=com.utils.CustomPartitioner)_
  - _Note:_ for simulation purpose, every time you run [com.examples.EyeTrackersProducer](EyeTrackers-Producer/src/main/java/com/examples/EyeTrackersProducer.java), the existing _"gaze-events"_ topic is deleted and a new topic with the same name is created.


- Main Class: [com.examples.DescribeTopics](EyeTrackers-Producer/src/main/java/com/examples/DescribeTopics.java)
  - The class provides an overview on the existing Kafka topics and the underlying partitions. It is based on the Kafka AdminClient Class (for documentation see: https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/admin/AdminClient.html)

#### Instruction

- Explore the different classes within [EyeTrackers-Producer](EyeTrackers-Producer/src/main/) and examine the control-flow within the class [com.examples.EyeTrackersProducer](EyeTrackers-Producer/src/main/java/com/examples/EyeTrackersProducer.java)

## Consumers

### ConsumerForAllEvents

- Prerequisite for running [ConsumerForAllEvents](consumer/src/main/java/com/examples/ConsumerForAllEvents.java): both [EyeTrackers-Producer](EyeTrackers-Producer/) (Main Class [com.examples.EyeTrackersProducer](EyeTrackers-Producer/src/main/java/com/examples/EyeTrackersProducer.java)) and [ClickStream-Producer](ClickStream-Producer/)  (Main Class [com.examples.ClicksProducer](ClickStream-Producer/src/main/java/com/examples/ClicksProducer.java)) are running


- Main Class: [com.examples.ConsumerForAllEvents](consumer/src/main/java/com/examples/ConsumerForAllEvents.java) 
    - This consumer consumes the events coming from both [ClickStream-Producer](ClickStream-Producer/) and [EyeTrackers-Producer](EyeTrackers-Producer/)
    - We use a custom deserializer to deserialize the events when received (see [com.utils.JavaDeserializer](consumer/src/main/java/com/utils/JavaDeserializer.java))
      - The custom deserializer is specified in [resources/consumer.properties](consumer/src/main/resources/consumer.properties) (_value.deserializer=com.utils.JavaDeserializer_)
    - The consumer subscribe to two topics: _"gaze-events"_ and _"click-events"_
    - The events in the _"gaze-events"_ topic come from **two partitions**, while the events in the _"click-events"_ come from **one partition only**

#### Instruction

- Examine the control-flow within the class [ConsumerForAllEvents](consumer/src/main/java/com/examples/ConsumerForAllEvents.java)

### ConsumerForGazeEventsForSingleEyeTracker: 

- Prerequisite for running [ConsumerForGazeEventsForSingleEyeTracker](consumer/src/main/java/com/examples/ConsumerForGazeEventsForSingleEyeTracker.java): [EyeTrackers-Producer](EyeTrackers-Producer/) (Main Class [com.examples.EyeTrackersProducer](EyeTrackers-Producer/src/main/java/com/examples/EyeTrackersProducer.java)) is running


- Main Class: [com.examples.ConsumerForGazeEventsForSingleEyeTracker](consumer/src/main/java/com/examples/ConsumerForGazeEventsForSingleEyeTracker.java)
  - This consumer consumes the events coming from a **single** eye-tracker _(deviceID: 0)_ (These events were stored in _partition "0"_ within the _"gaze-events"_ topic)
    - This is specified using the following code fragment
    ```Java
        // Read specific topic and partition
        TopicPartition topicPartition = new TopicPartition("gaze-events", 0);
        consumer.assign(Arrays.asList(topicPartition));
       ```

#### Instruction

- Examine the control-flow within the class [com.examples.ConsumerForGazeEventsForSingleEyeTracker](consumer/src/main/java/com/examples/ConsumerForGazeEventsForSingleEyeTracker.java)

### ConsumerForGazeEventsForSingleEyeTrackerCustomOffset:

- Prerequisite for running [ConsumerForGazeEventsForSingleEyeTrackerCustomOffset](consumer/src/main/java/com/examples/ConsumerForGazeEventsForSingleEyeTrackerCustomOffset.java): [EyeTrackers-Producer](EyeTrackers-Producer/) (Main Class [com.examples.EyeTrackersProducer](EyeTrackers-Producer/src/main/java/com/examples/EyeTrackersProducer.java)) is running


- Main Class: [com.examples.ConsumerForGazeEventsForSingleEyeTrackerCustomOffset](consumer/src/main/java/com/examples/ConsumerForGazeEventsForSingleEyeTrackerCustomOffset.java)
  - This consumer consumes the events coming from a **single** eye-tracker (_deviceID: 0)_ (These events were stored in partition _"0"_ within the _"gaze-events"_ topic)
  - In addition, the consumer starts reading events from a _specific user-defined offset_ (i.e., _int offsetToReadFrom_)
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

- Examine the control-flow within the class [com.examples.ConsumerForGazeEventsForSingleEyeTrackerCustomOffset](consumer/src/main/java/com/examples/ConsumerForGazeEventsForSingleEyeTrackerCustomOffset.java). Try out different values in _offsetToReadFrom_


### "rebalancingExample.*": 

- Prerequisite for running: "EyeTrackers-Producer" (Class "com.examples.EyeTrackersProducer") is running


- The package contains two (main) classes that demonstrate how Kafka does rebalancing when a new consumer is added (within the same group)
- For the sake of simulation, the classes "rebalancingExample.ConsumerForGazeEventsForEyeTrackerParitionsRebalancing1" and "rebalancingExample.ConsumerForGazeEventsForEyeTrackerParitionsRebalancing2" have duplicate code.
- In both classes, a Kafka consumer is created, subscribed to the topic _"gaze-events"_ and print the received events to the console
- Remember that the topic _"gaze-events"_ has two partitions (referring to the two eye-trackers)

#### Instructions

- Ensure that "EyeTrackers-Producer" (Class "com.examples.EyeTrackersProducer") is running (prerequisite)
- Run "rebalancingExample.ConsumerForGazeEventsForEyeTrackerParitionsRebalancing1"
  - Check that the consumer consumes and prints to the console the events belonging to both Partition 0 and Partition 1 of the _"gaze-events"_ topic
- Run "rebalancingExample.ConsumerForGazeEventsForEyeTrackerParitionsRebalancing2"
  - Check that "rebalancingExample.ConsumerForGazeEventsForEyeTrackerParitionsRebalancing1" will start consuming and printing to the console the events of a single partition only
  - Check that "rebalancingExample.ConsumerForGazeEventsForEyeTrackerParitionsRebalancing2" will start consuming and printing to the console the events of the other partition


### "singleAcessToPartitionAndRebalancingExample.*"

- Prerequisite for running: "ClickStream-Producer" (Class "com.examples.ClicksProducer") is running 


- The package contains two (main) classes demonstrating how Kafka allows only one consumer (within a consumer group) to read from a partition and how rebalancing occurs when a running consumer is out
- For the sake of simulation, the classes "singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly1" and "singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly2" have duplicate code.
- In both classes, a Kafka consumer is created, subscribed to the topic _"click-events"_ and print the received events to the console
- Remember that the topic _"click-events"_ has only 1 partition and so this partition can be read by only one consumer within a consumer group

#### Instructions

- Ensure that "ClickStream-Producer" (Class "com.examples.ClicksProducer") is running (prerequisite)
- Run "singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly1"
  - Check that the consumer consumes and prints to the console the events of the _"click-events"_ topic.
- Run "singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly2"
  - Remember that the topic _"click-events"_ has only 1 partition and so this partition can be read by only one consumer within a consumer group
  - Check that only one of the two consumers will keep consuming the events while the other consumer will stay idle
- Stop the non-idle consumer i.e., the one still consuming events
  - Check that the other consumer will take over and start consuming events
- Assuming that the click events have an incremental eventID, by comparing the output consoles of "singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly1" and "singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly2", check whether any of the events are duplicated or lost


### "customCommit.singleAcessToPartitionAndRebalancingExample.*"


- Prerequisite for running: "ClickStream-Producer" (Class "com.examples.ClicksProducer") is running


- The package contains two (main) classes demonstrating in the context of manual offset commit (occuring after every n consumer polls) the impact of rebalancing on events duplication and loss
- For the sake of simulation, the classes "customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly1" and "customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly2" have duplicate code.
- In both classes, a Kafka consumer is created, subscribed to the topic _"click-events"_ and print the received events to the console
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
- Remember that the topic _"click-events"_ has only 1 partition and so this partition can be read by only one consumer within a consumer group

#### Instructions

- Ensure that "ClickStream-Producer" (Class "com.examples.ClicksProducer") is running (prerequisite)
- Run "customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly1"
  - Check that the consumer consumes and prints to the console the events of the _"click-events"_ topic.
- Run "customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly2"
  - Remember that the topic _"click-events"_ has only 1 partition and so this partition can be read by only one consumer within a consumer group
  - Check that only one of the two consumers will keep consuming the events while the other consumer will stay idle
- Stop the non-idle consumer i.e., the one still consuming events
  - Check that the other consumer will take over and start consuming events
- Assuming that the click events have an incremental eventID, by comparing the output consoles of "customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly1 and "customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly2", check whether any of the events are duplicated or lost
- Edit the code in both  "customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly1 and "customCommit.singleAcessToPartitionAndRebalancingExample.ConsumerForClickEventsOnly2" so that the manual offset commit is executed after each 10 consumer polls, repeat the previous instructions, check whether any of the events are duplicated or lost



### "customCommit.commitLargerOffset.*"

- Prerequisite for running: "ClickStream-Producer" (Class "com.examples.ClicksProducer") is running


- The package contains two (main) classes two classes demonstrating events loss when a manual offset that is larger than the offset of the latest processed events in set
- For the sake of simulation, the classes "customCommit.commitLargerOffset.ConsumerForClickEventsOnly1" and "customCommit.commitLargerOffset.ConsumerForClickEventsOnly2" have duplicate code.
- In both classes, a Kafka consumer is created, subscribed to the topic _"click-events"_ and print the received events to the console
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
  
- Remember that the topic _"click-events"_ has only 1 partition and so this partition can be read by only one consumer within a consumer group

#### Instructions

- Ensure that "ClickStream-Producer" (Class "com.examples.ClicksProducer") is running (prerequisite)
- Run "customCommit.commitLargerOffset.ConsumerForClickEventsOnly1"
  - Check that the consumer consumes and prints to the console the events of the _"click-events"_ topic.
- Run "customCommit.commitLargerOffset.ConsumerForClickEventsOnly2"
  - Remember that the topic _"click-events"_ has only 1 partition and so this partition can be read by only one consumer within a consumer group
  - Check that only one of the two consumers will keep consuming the events while the other consumer will stay idle
- Stop the non-idle consumer i.e., the one still consuming events
  - Check that the other consumer will take over and start consuming events
- Assuming that the click events have an incremental eventID, by comparing the output consoles of "customCommit.commitLargerOffset.ConsumerForClickEventsOnly1 and "customCommit.commitLargerOffset.ConsumerForClickEventsOnly2", check whether some events are lost









