package com.utils;


import com.data.Gaze;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

/* Java Serializer adapted from https://www.baeldung.com/kafka-custom-serializer */

public class JavaSerializer implements Serializer<Gaze> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, Gaze data) {
        try {
            if (data == null){
                System.out.println("Null received at serializing");
                return null;
            }
            ///System.out.println("Serializing...");
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new SerializationException("Error when serializing Gaze to byte[]");
        }
    }

    @Override
    public void close() {
    }

    }
