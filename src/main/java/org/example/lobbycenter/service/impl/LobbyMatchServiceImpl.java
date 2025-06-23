package org.example.lobbycenter.service.impl;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.example.lobbycenter.pojo.MatchEvent;
import org.example.lobbycenter.pojo.Result;
import org.example.lobbycenter.service.ILobbyMatchService;
import org.springframework.stereotype.Service;
import org.springframework.kafka.core.KafkaTemplate;

import javax.annotation.Resource;

import static org.example.lobbycenter.utils.KafkaConstants.MATCH_QUEUE_TOPIC;
@Slf4j
@Service
public class LobbyMatchServiceImpl implements ILobbyMatchService {

    //1、加入匹配队列
    //2、与游戏中心建立连接，接受推送消息：可以通过gRPC双向流RPC/websockert/异步监听/函数回调
    //3、超时处理
    //4、取消匹配
    //5、进入游戏，游戏服务中心与用户建立websocket连接

    //事件驱动+消息队列实现匹配机制
    // 匹配机制我准备采用消息队列加事件驱动完成，用户发起匹配请求，发送用户id到tocpic：waiting_match_userIds
    // kafka消费者监听该topic，创建房间，每个房间内安排6个用户，更新用户信息和房间信息，完成后通过websocket通知用户匹配成功，返回房间号
    @Resource
    private KafkaTemplate kafkaTemplate;

    @Override
    public Result joinMatch(String userId) {
        MatchEvent matchEvent = new MatchEvent().setUserId(userId).setTopic(MATCH_QUEUE_TOPIC);

        // 发送匹配事件到Kafka队列
        kafkaTemplate.send(matchEvent.getTopic(), JSONObject.toJSONString(matchEvent));
        log.info("发送匹配事件到Kafka队列:{}", matchEvent);
        // 返回匹配中的响应信息
        return Result.ok("匹配中，请等待...");
    }

    //1、移除匹配队列
    //2、与游戏中心断开连接
    @Override
    public Result cancelMatch(String userId) {
        return null;
    }

    //1、获取匹配队列状态
    //感觉可以作为信息接受通道
    @Override
    public Result getMatchQueueStatus() {
        return null;
    }
}
