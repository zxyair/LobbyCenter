package org.example.lobbycenter.controller;

import org.example.lobbycenter.service.ILobbyMatchService;
import org.example.lobbycenter.pojo.Result;
import org.example.lobbycenter.utils.GetUserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

//todo 可扩展大厅群聊、加好友、组队、状态展示等服务。

@RestController
@RequestMapping("/api/match")
public class MatchController {

    @Qualifier("lobbyMatchServiceImpl")
    @Autowired
    private ILobbyMatchService lobbyMatchService;

    @Autowired
    private GetUserInfo getUserInfo;
    /**
     * 加入匹配队列
     * @param token 用户认证token
     * @return 匹配结果
     */
    //todo 需要前置校验：余额还够不够
    @PostMapping("/join")
    public Result joinMatchQueue(
            @RequestHeader("Authorization") String token) {
        String userId = getUserInfo.getUserIdByToken(token);
        if (userId == null || userId.isEmpty()) {
            return Result.fail("无效的token或用户未登录");
        }
        return lobbyMatchService.joinMatch(userId);
    }

    /**
     * 取消匹配
     * @param token 用户认证token
     * @return 取消结果
     */
    @PostMapping("/cancel")
    public Result cancelMatch(
            @RequestHeader("Authorization") String token) {
        String userId = getUserInfo.getUserIdByToken(token);
        if (userId == null || userId.isEmpty()) {
            return Result.fail("无效的token或用户未登录");
        }
        return Result.fail("暂不支持取消匹配");
    }

    /**
     * 查询匹配队列状态,查看排队人数
     * @return 队列状态信息
     */
    @GetMapping("/queueInfo")
    public Result getMatchQueueStatus() {
        return Result.fail("暂不支持查询匹配队列");
    }
}

