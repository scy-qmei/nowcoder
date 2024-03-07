package com.nowcoder.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@SpringBootTest
public class KafkaTest {

    public static void main(String[] args) throws InterruptedException {
        Producer producer = new Producer();
        producer.sendMessage("test","hello");
        producer.sendMessage("test","hello1");
        Thread.sleep(5000);
    }
}
@Component
class Producer{
    @Autowired
    private KafkaTemplate kafkaTemplate;

    public void sendMessage(String topic, String content) {
        kafkaTemplate.send(topic,content);
    }
}
@Component
class Consumer{

    @KafkaListener(topics = {"test"})
    public void getMessage(ConsumerRecord record) {
        System.out.println(record.value());
    }
}
