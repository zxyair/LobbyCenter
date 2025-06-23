package org.example.lobbycenter.event;

import com.alibaba.fastjson.JSONObject;
import org.example.lobbycenter.pojo.MatchEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class KafkaMatchProducer {

    @Resource
    private KafkaTemplate kafkaTemplate;

    public void publishEvent(MatchEvent event) {
        // 将事件发布到指定的主题
        kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
    }

}
