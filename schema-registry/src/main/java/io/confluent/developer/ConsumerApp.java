package io.confluent.developer;


import io.confluent.developer.avro.Purchase;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConsumerApp {

     public void consumePurchaseEvents() {
         Properties properties = loadProperties();
         Map<String, Object> consumerConfigs = new HashMap<>();
         properties.forEach((key, value) -> consumerConfigs.put((String) key, value));
         consumerConfigs.put(ConsumerConfig.GROUP_ID_CONFIG, "schema-registry-course-consumer");
         consumerConfigs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

         consumerConfigs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
         consumerConfigs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
         consumerConfigs.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, Purchase.class );
         
        // Duplication of configs loaded from confluent.properties to emphasize what's needed to use SchemaRegistry
         consumerConfigs.put("schema.registry.url", "https://psrc-qjmzd.us-east-2.aws.confluent.cloud");
         consumerConfigs.put("basic.auth.credentials.source", "USER_INFO");
         consumerConfigs.put("basic.auth.user.info", "Z7RY3FUYM5TQXPOO:4A1liHlkFdQvAR7b9Hxub/lOpqXqhRqBbD+zZhFQI2g/RBvtBUWR6Ht1YYgAJuK9");

         try(Consumer<String, Purchase> consumer = new KafkaConsumer<>(consumerConfigs)){
             consumer.subscribe(Collections.singletonList("avro-purchase"));
             while (true) {
                 ConsumerRecords<String, Purchase> consumerRecords = consumer.poll(Duration.ofSeconds(2));
                 consumerRecords.forEach(consumerRecord -> {
                     Purchase purchase = consumerRecord.value();
                     System.out.print("Purchase details { ");
                     System.out.printf("Customer: %s, ", purchase.getCustomerId());
                     System.out.printf("Total Cost: %f, ", purchase.getTotalCost());
                     System.out.printf("Item: %s } %n", purchase.getItem());
                 });
             }
         }
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
        ConsumerApp consumerApp = new ConsumerApp();
        consumerApp.consumePurchaseEvents();
    }
}
