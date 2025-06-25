package org.example.lobbycenter.service.impl;

import org.example.common.pojo.Result;
import org.example.lobbycenter.service.ILobbyRoomService;
import org.example.lobbycenter.websocket.WebSocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

@Slf4j
@Service
public class LobbyRoomServiceImpl implements ILobbyRoomService {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private WebSocket webSocket;

    @Override
    public Result createRoom(String ownerId, Integer maxPlayers) {
        /*设计点
        *   playerList 用 Redis List 存储，方便后续玩家加入/离开操作。
            房间信息 一次性 putAll，减少多次网络请求。
            事务 保证多步操作原子性，防止并发下数据不一致。
            参数校验 防止非法请求。
            异常处理 保证服务健壮性。
        * */

        log.info("创建房间");
        //TODO 可以加入唯一性校验，每人只能创建一个房间
        // 生成房间ID
        if (ownerId == null || ownerId.trim().isEmpty()) {
            return Result.fail("房主ID不能为空");
        }
        if (maxPlayers == null || maxPlayers < 2 || maxPlayers > 10) {
            return Result.fail("最大玩家数需在2~10之间");
        }
        String roomId = generateRoomId();
        log.info("创建房间，房主ID：" + ownerId + "，最大玩家数：" + maxPlayers+"，房间ID：" + roomId);
        final Double score;
        Double tempScore = stringRedisTemplate.opsForZSet().score("users_coin_count", ownerId);
        if (tempScore == null) {
            stringRedisTemplate.opsForZSet().add("users_coin_count", ownerId, 100);
            score = 100.0;
        } else {
            score = tempScore;
        }
        try {
            // 使用Redis事务保证原子性
            List<Object> txResults = stringRedisTemplate.execute(new SessionCallback<List<Object>>() {
                @Override
                public List<Object> execute(RedisOperations operations) throws DataAccessException {
                    operations.multi();
//                    房间状态(Integer)：等待0、游戏中1、已解散2
                    // 1. 房间信息（Hash）
                    Map<String, String> roomInfo = new HashMap<>();
                    roomInfo.put("roomId", roomId);
                    roomInfo.put("ownerId", ownerId);
                    roomInfo.put("status", "0");
                    roomInfo.put("maxPlayers", maxPlayers.toString());
                    operations.opsForHash().putAll("room:" + roomId, roomInfo);

                    // 2. 玩家列表（Zset，存字符串,coin作为分数）
//                    operations.opsForList().rightPush("room:" + roomId + ":players", ownerId);
                    operations.opsForZSet().add("room:" + roomId + ":players", ownerId, score);
                    //玩家状态 Unready 0 ，Ready 1  即将开始 2  Playing 3  Offline 4
                    // 3. 房主状态（Hash）
                    Map<String, String> playerStatus = new HashMap<>();
                    playerStatus.put("userId", ownerId);
                    playerStatus.put("seatNo", "0");
                    playerStatus.put("status", "1");
                    playerStatus.put("roomId", roomId);
                    operations.opsForHash().putAll("players_status:" + ownerId, playerStatus);
                    log.info("创建房间成功，房间号：" + roomId);
                    // 4. 房间号加入等待房间列表
                    operations.opsForList().rightPush("ready_room_list", roomId);

                    return operations.exec();
                }
            });

            if (txResults == null || txResults.isEmpty()) {
                return Result.fail("创建房间失败，请重试");
            }
        } catch (Exception e) {

            // 日志记录
            return Result.fail("创建房间失败，请稍后重试，异常信息：" + e.getMessage());
        }
        return Result.ok(roomId);
    }

    //加入房间，1、修改房间信息中的playerList，2、修改玩家状态，
    //TODO 需要考虑房主退出房间，房间解散，房间满员的情况 Down
    //TODO 每个人只能加入一个房间 DOWN
    //TODO 加入房间可以考虑用websocket，对房间内广播，可以在原有逻辑上优化，加一个上线播报接口，利用大厅服务的websocket连接池，根据room：roomId：players 通知到其他玩家

    @Override
    public Result joinRoom(String userId, String roomId) {
        // 检查用户是否已经在其他房间
        if (stringRedisTemplate.hasKey("players_status:" + userId)) {
            log.error("用户：" + userId + "已加入其他房间，请先退出当前房间,房间号为" + stringRedisTemplate.opsForHash().get("players_status:" + userId, "roomId"));
            return Result.fail("您已加入其他房间，请先退出当前房间,房间号为" + stringRedisTemplate.opsForHash().get("players_status:" + userId, "roomId"));
        }
        if (userId == null || userId.trim().isEmpty()) {
            log.error("用户ID不能为空");
            return Result.fail("用户ID不能为空");
        }
        if (roomId == null || roomId.trim().isEmpty()) {
            log.error("房间ID不能为空");
            return Result.fail("房间ID不能为空");
        }
        if (!stringRedisTemplate.hasKey("room:" + roomId)) {
            log.error("房间不存在");
            return Result.fail("房间不存在");
        }

        Map<Object, Object> roomInfo = stringRedisTemplate.opsForHash().entries("room:" + roomId);
        if (roomInfo == null || roomInfo.isEmpty()) {
            log.error("获取房间信息失败，房间ID: {}", roomId);
            return Result.fail("获取房间信息失败");
        }
        Object statusObj = roomInfo.get("status");
        if (statusObj == null) {
            log.error("房间状态为空，房间ID: {}", roomId);
            return Result.fail("房间状态异常");
        }
        String status = statusObj.toString();
        if (!status.equals("0")) {  // 0=等待中
            log.error("房间状态不允许加入");
            return Result.fail("房间状态不允许加入");
        }

//        Long currentPlayers = redisTemplate.opsForList().size("room:" + roomId + ":players");
        Long currentPlayers = stringRedisTemplate.opsForZSet().size("room:" + roomId + ":players");
        Object maxPlayersObj = roomInfo.get("maxPlayers");
        if (maxPlayersObj == null) {
            log.error("最大玩家数为空，房间ID: {}", roomId);
            return Result.fail("房间配置异常");
        }
        Integer maxPlayers;
        try {
            maxPlayers = Integer.parseInt(maxPlayersObj.toString());
        } catch (NumberFormatException e) {
            log.error("最大玩家数格式错误，房间ID: {}, 值: {}", roomId, maxPlayersObj);
            return Result.fail("房间配置异常");
        }
        if (currentPlayers == null) {
            log.error("获取当前玩家数量失败");
            return Result.fail("获取当前玩家数量失败");
        }
        if (currentPlayers >= maxPlayers) {
            log.error("房间已满");
            return Result.fail(String.format("房间已满(当前%d人/最大%d人)", currentPlayers, maxPlayers));
        }
        if (currentPlayers < 0) {
            log.error("当前玩家数量异常");
            return Result.fail("当前玩家数量异常");
        }

//        Long playerCount = redisTemplate.opsForList().size("room:" + roomId + ":players");
        Long playerCount = stringRedisTemplate.opsForZSet().size("room:" + roomId + ":players");
        if (playerCount == null) {
            log.error("获取玩家数量失败");
            throw new RuntimeException("获取玩家数量返回null");
        }
        final Double score;
        Double tempScore = stringRedisTemplate.opsForZSet().score("users_coin_count", userId);
        if (tempScore == null) {
            stringRedisTemplate.opsForZSet().add("users_coin_count", userId, 100);
            score = 100.0;
        } else {
            score = tempScore;
        }
        try {
            // 使用Redis事务保证原子性
            List<Object> txResults = stringRedisTemplate.execute(new SessionCallback<List<Object>>() {
                @Override
                public List<Object> execute(RedisOperations operations) throws DataAccessException {
                    operations.multi();
                    if (operations == null) {
                        log.error("Redis operations 为空");
                        return Collections.emptyList();
                    }
                    // 1. 将玩家加入房间玩家列表
//                    operations.opsForList().rightPush("room:" + roomId + ":players", userId);

                    operations.opsForZSet().add("room:" + roomId + ":players", userId, score);
                    log.info("当前房间玩家列表为：" + operations.opsForZSet().range("room:" + roomId + ":players", 0, -1).toString());
                    // 2. 设置玩家状态
                    Map<String, String> playerStatus = new HashMap<>();
                    playerStatus.put("userId", Objects.requireNonNull(userId, "用户ID不能为空"));
                    playerStatus.put("seatNo", String.valueOf(playerCount));
                    playerStatus.put("status", "1"); // 初始状态为准备
                    playerStatus.put("roomId", roomId);
                    operations.opsForHash().putAll("players_status:" + userId, playerStatus);
                    log.info("当前玩家状态为：" + playerStatus.toString());
                    return operations.exec();
                }
            });

            if (txResults == null || txResults.isEmpty()) {
                return Result.fail("txResults == null || txResults.isEmpty(),加入房间失败，请重试");
            }
        } catch (Exception e) {
            log.error("加入房间失败 - 房间ID: {}, 用户ID: {}, 异常类型: {}, 异常信息: {}",
                    roomId, userId, e.getClass().getName(), e.getMessage(), e);
            return Result.fail("Exception e，加入房间失败，请稍后重试");
        }
        return Result.ok("成功加入房间");
    }

    @Override
    public Result startGame(String roomId) throws IOException {
        Set<String>  userIds = stringRedisTemplate.opsForZSet().range("room:" + roomId + ":players", 0, -1);
        webSocket.sendMessageToUsers("游戏即将开始，房间号为" + roomId+",请点击开始进入游戏", userIds);
        return null;
    }

    //todo 添加逻辑，如果是房主，房间解散 dOWN
    //ps:这里没有做验证是否成员在本房间内，考虑此功能应该由客户端完成开发，只有在房间内的成员才能调用此接口
    //离开房间和加入房间不同，不需要考虑并发，主要是删除操作，即使多次触发，也不影响业务流程
//    @Override
//    public Result leaveRoom(String userId, String roomId) {
//        if (userId == null || userId.trim().isEmpty()) {
//            return Result.fail("用户ID不能为空");
//        }
//        if (roomId == null || roomId.trim().isEmpty()) {
//            return Result.fail("房间ID不能为空");
//        }
//        // 检查是否是房主
//        String ownerId = (String) redisTemplate.opsForHash().get("room:" + roomId, "ownerId");
//        if (userId.equals(ownerId)) {
//            return dismissRoom(userId,roomId);
//        }
//        try {
//            // 1. 从房间玩家列表中移除该玩家
//            redisTemplate.opsForList().remove("room:" + roomId + ":players", 0, userId);
//
//            // 2. 删除该玩家的状态信息
//            redisTemplate.delete("players_status:" + userId);
//
//            // 3. 如果房间没有玩家了，自动解散房间
//            //PS:这里逻辑肯定走不到，因为房主离开房间，房间会自动解散
//            if (redisTemplate.opsForList().size("room:" + roomId + ":players") == 0) {
//                redisTemplate.delete("room:" + roomId);
//                redisTemplate.delete("room:" + roomId + ":players");
//                redisTemplate.opsForList().remove("ready_room_list", 0, roomId);
//            }
//
//        } catch (Exception e) {
//            return Result.fail("离开房间失败，请稍后重试");
//        }
//        return Result.ok("成功离开房间");
//    }

    //方便测试，去掉校验逻辑
    @Override
    public Result leaveRoom(String userId, String roomId) {
        if (userId == null || userId.trim().isEmpty()) {
            return Result.fail("用户ID不能为空");
        }
        roomId = (String) stringRedisTemplate.opsForHash().get("players_status:" + userId, "roomId");
        try {
            // 从房间玩家列表中移除当前用户
//            redisTemplate.opsForList().remove("room:" + roomId + ":players", 0, userId);
            stringRedisTemplate.opsForZSet().remove("room:" + roomId + ":players", userId);
            // 删除该用户在房间中的状态
            stringRedisTemplate.delete("players_status:" + userId);
            return Result.ok("成功离开房间");
        } catch (Exception e) {
            return Result.fail("离开房间失败，请稍后重试");
        }
    }

    //只有房主才能解散房间
    @Override
    public Result dismissRoom(String roomId) {
        if (roomId == null || roomId.trim().isEmpty()) {
            return Result.fail("房间ID不能为空");
        }
        try {
            // 1. 删除房间信息
            stringRedisTemplate.delete("room:" + roomId);

            // 2. 获取所有玩家ID并删除他们的状态
//            List players = redisTemplate.opsForList().range("room:" + roomId + ":players", 0, -1);
            Set<String> players = stringRedisTemplate.opsForZSet().range("room:" + roomId + ":players", 0, -1);
            if (players != null) {
                for (String playerId : players) {
                    stringRedisTemplate.delete("players_status:" + playerId);
                }
            }

            // 3. 删除房间玩家列表
            stringRedisTemplate.delete("room:" + roomId + ":players");

            // 4. 从等待房间列表中移除
            stringRedisTemplate.opsForList().remove("ready_room_list", 0, roomId);

            stringRedisTemplate.delete("hotpot:pool" + roomId);

        } catch (Exception e) {
            log.error("解散房间失败", e);
            return Result.fail("解散房间失败，请稍后重试");
        }
        return Result.ok("房间已成功解散");
    }

    //todo 房间状态变更时，如何推送，可以考虑建立websocket双向通道
    //设计点，拉去房间列表时推送20个房间，同时推送每个房间当前状态，实时更新
    @Override
    public Result listRoom(Integer page) {
        if (page == null || page < 1) {
            page = 1;  // 默认第一页
        }
        int pageSize = 20;  // 每页20条记录
        long start = (page - 1) * pageSize;
        long end = start + pageSize - 1;

        try {
            // 获取分页范围内的等待中的房间ID列表
            List<String> roomIds = stringRedisTemplate.opsForList().range("ready_room_list", start, end);
            if (roomIds == null || roomIds.isEmpty()) {
                return Result.ok(Collections.emptyList());
            }

            List<Map<String, Object>> roomList = new ArrayList<>();
            for (String roomId : roomIds) {
                // 获取房间基本信息
                Map<Object, Object> roomInfo = stringRedisTemplate.opsForHash().entries("room:" + roomId);
                if (roomInfo.isEmpty()) {
                    continue;
                }

                // 获取当前玩家数量
                Long playerCount = stringRedisTemplate.opsForZSet().size("room:" + roomId + ":players");

                // 组装房间信息
                Map<String, Object> room = new HashMap<>();
                room.put("roomId", roomId);
                room.put("ownerId", roomInfo.get("ownerId"));
                room.put("status", roomInfo.get("status"));
                room.put("maxPlayers", roomInfo.get("maxPlayers"));
                room.put("currentPlayers", playerCount != null ? playerCount : 0);
                // 添加房间状态变更时间戳
                room.put("lastUpdateTime", System.currentTimeMillis());

                roomList.add(room);
            }
            return Result.ok(roomList);
        } catch (Exception e) {
            log.error("获取房间列表失败", e);
            return Result.fail("获取房间列表失败");
        }
    }


    public String generateRoomId() {
        String roomId;
        do {
            int num = 100000 + new Random().nextInt(900000);
            roomId = String.valueOf(num);
        } while (stringRedisTemplate.hasKey("room:" + roomId));
        return roomId;
    }
}

