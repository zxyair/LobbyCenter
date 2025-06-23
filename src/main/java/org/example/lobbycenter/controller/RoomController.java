package org.example.lobbycenter.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.lobbycenter.pojo.Result;
import org.example.lobbycenter.service.ILobbyMatchService;
import org.example.lobbycenter.service.ILobbyRoomService;
import org.example.lobbycenter.utils.GetUserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/room")
@Slf4j
public class RoomController {

    @Qualifier("lobbyRoomServiceImpl")
    @Autowired
    private ILobbyRoomService lobbyRoomService;

    @Autowired
    private GetUserInfo getUserInfo;
    /**
     * 创建房间
     */
    //版本一：6人一个房间
    //todo 增加房间等级的区分，如6人房间，8人房间，10人房间
    @PostMapping("/create")
    public Result createRoom(
            @RequestHeader("Authorization") String token,
            @RequestParam("maxPlayers") Integer maxPlayers) {
        // 从Redis中根据token获取用户ID
        String ownerId = getUserInfo.getUserIdByToken(token);
        log.info("ownerId: " + ownerId);
        if (ownerId == null || ownerId.isEmpty()) {
            return Result.fail("无效的token或用户未登录");
        }
        return lobbyRoomService.createRoom(ownerId, maxPlayers);
    }

    /**
     * 加入房间
     */
    @PostMapping("/join")
    public Result joinRoom(
            @RequestHeader("Authorization") String token,
            @RequestParam("roomId") String roomId
    ) {
        String userId = getUserInfo.getUserIdByToken(token);
        if (userId == null || userId.isEmpty()) {
            return Result.fail("无效的token或用户未登录");
        }
        return lobbyRoomService.joinRoom(userId,roomId);
    }
    /**
     * 拉取房间列表
     */
    //todo 可以做成分页的，现在只拉取前10个房间
    //todo 可以实现分段机制，将房间分为不同等级的，高等级的房间在前
    @GetMapping("/list")
    public Result listRoom(
            @RequestParam("page") Integer page
    ) {
        return lobbyRoomService.listRoom(page);
    }


    /**
     * 开始游戏
     *
     * @return 开始游戏结果
     */
    @PostMapping("/start")
    public Result startGame( @RequestParam("roomId") String roomId) {
        return lobbyRoomService.startGame(roomId);
    }

    /**
     * 离开房间
     *
     * @return 离开结果
     */
    @PostMapping("/leave")
    public Result leaveRoom( @RequestHeader("Authorization") String token,
                             @RequestParam("roomId") String roomId) {
        String userId = getUserInfo.getUserIdByToken(token);
        if (userId == null || userId.isEmpty()) {
            return Result.fail("无效的token或用户未登录");
        }
        return lobbyRoomService.leaveRoom(userId,roomId);
    }

    /**
     * 解散房间
     * @return 解散结果
     */
    @PostMapping("/dismiss")
    public Result dismissRoom(@RequestParam("roomId") String roomId) {
        return lobbyRoomService.dismissRoom(roomId);
    }
}