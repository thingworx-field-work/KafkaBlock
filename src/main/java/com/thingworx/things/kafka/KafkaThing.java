/*
 * Copyright (c) 2018.  PTC Inc. and/or Its Subsidiary Companies. All Rights Reserved.
 * Copyright for PTC software products is with PTC Inc. and its subsidiary companies (collectively “PTC”), and their respective licensors. This software is provided under written license agreement, contains valuable trade secrets and proprietary information, and is protected by the copyright laws of the United States and other countries. It may not be copied or distributed in any form or medium, disclosed to third parties, or used in any manner not provided for in the software license agreement except with written prior approval from PTC.
 */

package com.thingworx.things.kafka;

import ch.qos.logback.classic.Logger;
import com.thingworx.logging.LogUtilities;
import com.thingworx.metadata.annotations.*;
import com.thingworx.things.Thing;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@ThingworxConfigurationTableDefinitions(
        tables = {@ThingworxConfigurationTableDefinition(
                name = "ConnectionInfo",
                description = "Connection Settings",
                isMultiRow = false,
                dataShape = @ThingworxDataShapeDefinition(
                        fields = {@ThingworxFieldDefinition(
                                name = "serverName",
                                description = "KafkaServer:port",
                                baseType = "STRING",
                                aspects = {"defaultValue:localhost:9092"}
                        ), @ThingworxFieldDefinition(
                                name = "clientID",
                                description = "Client Name",
                                baseType = "STRING",
                                aspects = {"defaultValue:client1"}
                        ), @ThingworxFieldDefinition(
                                name = "topicName",
                                description = "Topic Name",
                                baseType = "STRING",
                                aspects = {"defaultValue:demo"}
                        ), @ThingworxFieldDefinition(
                                name = "groupID",
                                description = "Consumer Group Name",
                                baseType = "STRING",
                                aspects = {"defaultValue:consumerGroup1"}
                        ), @ThingworxFieldDefinition(
                                name = "timeout",
                                description = "Max No Message Found",
                                baseType = "NUMBER",
                                aspects = {"defaultValue:100"}
                        )}
                )
        )}
)

public class KafkaThing
        extends Thing {
    protected static final Logger _Logger = LogUtilities.getInstance().getApplicationLogger(KafkaThing.class);

    //twx conf page
    private String _serverName = "localhost:9092";
    private String _clientID = "client1";
    private String _topicName = "demo";
    private String _groupID = "consumerGroup1";
    private Integer _timeout = 100;

    //default config
    public String KAFKA_BROKERS = this._serverName;
    public static Integer MESSAGE_COUNT = 1000;
    public static String CLIENT_ID = "client1";
    public static String TOPIC_NAME = "demo";
    public String GROUP_ID_CONFIG = this._groupID;
    public static Integer MAX_NO_MESSAGE_FOUND_COUNT = 100;
    public static String OFFSET_RESET_LATEST = "latest";
    public static String OFFSET_RESET_EARLIER = "earliest";
    public static Integer MAX_POLL_RECORDS = 1;



    protected void initializeThing()
            throws Exception {
        this._serverName = (String) this.getConfigurationSetting("ConnectionInfo", "serverName");
        this._clientID = ((String) this.getConfigurationSetting("ConnectionInfo", "clientID"));
        this._topicName = (String) this.getConfigurationSetting("ConnectionInfo", "topicName");
        this._groupID = (String) this.getConfigurationSetting("ConnectionInfo", "groupID");
        this._timeout = ((Number) this.getConfigurationSetting("ConnectionInfo", "timeout")).intValue();
    }

    public KafkaThing() {
        _Logger.info("Started the Kafka Extension for Thingworx");
    }

    @ThingworxServiceDefinition(name = "sendReceiveMessage", description = "")
    @ThingworxServiceResult(name = "result", description = "", baseType = "STRING")
    public String sendReceiveMessage(@ThingworxServiceParameter(name = "Topic name", description = "at least one field must be defined as string", baseType = "STRING") String topic1)
            throws Exception {
        _Logger.info("Start auto");

        //force set
        GROUP_ID_CONFIG = this._groupID;
        KAFKA_BROKERS = this._serverName;

        ContainerProperties containerProps = new ContainerProperties("topic1", "topic2");
        containerProps.setClientId(_clientID);
        containerProps.setGroupId(_groupID);
        containerProps.setMissingTopicsFatal(false);

        final CountDownLatch latch = new CountDownLatch(4);
        containerProps.setMessageListener(new MessageListener<Integer, String>() {

            @Override
            public void onMessage(ConsumerRecord<Integer, String> message) {
                _Logger.info("received: " + message);
                latch.countDown();
            }

        });
        KafkaMessageListenerContainer<Integer, String> container = createContainer(containerProps);
        container.setBeanName("testAuto");
        container.start();
        Thread.sleep(1000); // wait a bit for the container to start
        KafkaTemplate<Integer, String> template = createTemplate();
        template.setDefaultTopic(topic1);
        template.sendDefault(0, "foo");
        template.sendDefault(2, "bar");
        template.sendDefault(0, "baz");
        template.sendDefault(2, "qux");
        template.flush();
        //assertTrue(latch.await(60, TimeUnit.SECONDS));
        latch.await(60, TimeUnit.SECONDS);
        container.stop();
        _Logger.info("Stop auto");
        return "Test Complete";
    }

    private KafkaMessageListenerContainer<Integer, String> createContainer(
            ContainerProperties containerProps) {
        Map<String, Object> props = consumerProps();
        DefaultKafkaConsumerFactory<Integer, String> cf =
                new DefaultKafkaConsumerFactory<Integer, String>(props);
        KafkaMessageListenerContainer<Integer, String> container =
                new KafkaMessageListenerContainer<>(cf, containerProps);
        return container;
    }

    private KafkaTemplate<Integer, String> createTemplate() {
        Map<String, Object> senderProps = senderProps();
        ProducerFactory<Integer, String> pf =
                new DefaultKafkaProducerFactory<Integer, String>(senderProps);
        KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf);
        return template;
    }

    private Map<String, Object> consumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID_CONFIG);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "100");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "15000");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return props;
    }

    private Map<String, Object> senderProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKERS);
        props.put(ProducerConfig.RETRIES_CONFIG, 0);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return props;
    }

    protected static class ConfigConstants {
        public static final String ServerName = "serverName";
        public static final String ClientID = "clientID";
        public static final String TopicName = "topicName";
        public static final String GroupID = "groupID";
        public static final String Timeout = "timeout";

        protected ConfigConstants() {
        }
    }

}

