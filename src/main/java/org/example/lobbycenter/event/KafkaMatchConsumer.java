package org.example.lobbycenter.event;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.example.lobbycenter.pojo.MatchEvent;
import org.example.lobbycenter.pojo.Result;
import org.example.lobbycenter.service.ILobbyRoomService;
import org.example.lobbycenter.websocket.WebSocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.kafka.annotation.KafkaListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import javax.annotation.Resource;

import static org.example.lobbycenter.utils.KafkaConstants.MATCH_QUEUE_TOPIC;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//todo 可以采用线程池的方式进行处理，可以批量拉取
//todo kafka可以集群部署，并且配置多个消费者
//todo 还需要考虑死信队列、容灾、持久化这些东西

@Component
@Slf4j
public class KafkaMatchConsumer {
    @Qualifier("lobbyRoomServiceImpl")
    @Autowired
    private ILobbyRoomService lobbyRoomService;

    @Resource
    private WebSocket webSocket;

    // 批量消费数量
    private static final int BATCH_SIZE = 4;
    // 暂存用户ID的列表
    private final Set<String> userIds = new HashSet<>(BATCH_SIZE);



    @KafkaListener(topics = {MATCH_QUEUE_TOPIC}, containerFactory = "kafkaListenerContainerFactory")
    public void handleCreateOrder(List<ConsumerRecord> records) {
        if (records == null || records.isEmpty()) {
            log.warn("收到空消息批次");
            return;
        }

        // 1. 收集用户ID
        for (ConsumerRecord record : records) {
            if (record.value() == null) {
                log.error("消息内容为空, 跳过处理");
                continue;
            }

            MatchEvent event = JSONObject.parseObject(record.value().toString(), MatchEvent.class);
            if (event == null || event.getUserId() == null) {
                log.error("消息格式错误或缺少用户ID");
                continue;
            }

            userIds.add(event.getUserId());
            log.info("收集到用户ID: {}", event.getUserId());

            // 2. 当收集满6个用户时创建房间
            if (userIds.size() >= BATCH_SIZE) {
                try {
                    // 随机取一个成员作为房主
                    // 随机取一个成员作为房主（使用HashSet的迭代器第一个元素）
                    String ownerId = userIds.iterator().next();
                    log.info("随机取一个成员作为房主: {}", ownerId);
                    Set<String> members = new HashSet<>(userIds);
                    members.remove(ownerId);
                    log.info("房间成员: {}", members);
                    // 3. 创建房间
                    Result result = lobbyRoomService.createRoom(ownerId, Integer.valueOf(BATCH_SIZE));
                    String roomId = result.getData().toString();
                    log.info("成功创建房间: {}, 房主: {}, 成员: {}", roomId, ownerId, members);
                    for (String member : members) {
                        // 5. 将成员添加到房间
                        lobbyRoomService.joinRoom(member, roomId);
                        log.info("将用户{}添加到房间: {}", member, roomId);
                    }
                    // 4. 向所有用户发送房间信息
                    webSocket.sendMessageToUsers(roomId, userIds);
                } catch (Exception e) {
                    log.error("创建房间或通知用户失败", e);
                } finally {
                    // 清空列表准备下一批处理
                    userIds.clear();
                }
            }
        }
    }
}
