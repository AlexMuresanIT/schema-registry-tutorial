package io.confluent.developer;


import io.confluent.developer.avro.Purchase;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ProducerApp {
    private static final Logger LOG = LoggerFactory.getLogger(ProducerApp.class);
    private final Random random = new Random();
    private final List<String> items = List.of("shoes", "sun-glasses", "t-shirt");

    public void producePurchaseEvents() {
        Purchase.Builder purchaseBuilder = Purchase.newBuilder();
        Properties properties = loadProperties();
        Map<String, Object> producerConfigs = new HashMap<>();
        properties.forEach((key, value) -> producerConfigs.put((String) key, value));

        producerConfigs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerConfigs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        // Setting schema auto-registration to false since we already registered the schema manually following best practice
        producerConfigs.put(AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS, false);

        // Duplication of configs loaded from confluent.properties to emphasize what's needed to use SchemaRegistry
        producerConfigs.put("schema.registry.url", "https://psrc-qjmzd.us-east-2.aws.confluent.cloud");
        producerConfigs.put("basic.auth.credentials.source", "USER_INFO");
        producerConfigs.put("basic.auth.user.info", "Z7RY3FUYM5TQXPOO:4A1liHlkFdQvAR7b9Hxub/lOpqXqhRqBbD+zZhFQI2g/RBvtBUWR6Ht1YYgAJuK9");

        System.out.printf("Producer now configured for using SchemaRegistry %n");
        try (final Producer<String, Purchase> producer = new KafkaProducer<>(producerConfigs)) {
            String topic = "avro-purchase";
            List<Purchase> purchaseEvents = new ArrayList<>();
            Purchase purchase = getPurchaseObject(purchaseBuilder);
            Purchase purchaseII = getPurchaseObject(purchaseBuilder);

            purchaseEvents.add(purchase);
            purchaseEvents.add(purchaseII);

            purchaseEvents.forEach(event -> producer.send(new ProducerRecord<>(topic, event.getCustomerId(), event), ((metadata, exception) -> {
                if (exception != null) {
                    System.err.printf("Producing %s resulted in error %s %n", event, exception);
                } else {
                    System.out.printf("Produced record at offset %s with timestamp %d %n", metadata.offset(), metadata.timestamp());
                }
            })));
        }
    }

    Purchase getPurchaseObject(Purchase.Builder purchaseBuilder) {
        purchaseBuilder.clearItem();
        purchaseBuilder.setCustomerId("vandelay")
                .setTotalCost(random.nextDouble() * random.nextInt(100))
                .setItem(items.get(random.nextInt(3)));
        return purchaseBuilder.build();
    }

    Properties loadProperties() {
        try (InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("confluent.properties")) {
            Properties props = new Properties();
            props.load(inputStream);
            return props;
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static void main(String[] args) {
        ProducerApp producerApp = new ProducerApp();
        producerApp.producePurchaseEvents();
    }
}
