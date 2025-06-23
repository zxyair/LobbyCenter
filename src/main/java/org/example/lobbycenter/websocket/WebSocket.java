package org.example.lobbycenter.websocket;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
/**
 * @Author：JCccc
 * @Description：
 * @Date： created in 15:56 2019/5/13
 */

@Component
@ServerEndpoint(value = "/connectWebSocket/{userId}")
public class WebSocket {

    private static StringRedisTemplate stringRedisTemplate;

    @Autowired
    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        WebSocket.stringRedisTemplate = stringRedisTemplate;
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    /**
     * 在线人数
     */
    public static int onlineNumber = 0;
    /**
     * 以用户的姓名为key，WebSocket为对象保存起来
     */
    private static Map<String, WebSocket> clients = new ConcurrentHashMap<String, WebSocket>();
    /**
     * 会话
     */
    private Session session;
    /**
     * 用户名称
     */
    private String userId;

    /**
     * 建立连接
     *
     * @param session
     */





    @OnOpen
    public void onOpen(@PathParam("userId") String userId, Session session)
    {
        onlineNumber++;
        logger.info("现在来连接的客户id："+session.getId()+"用户名："+userId);
        this.userId = userId;
        this.session = session;
        //  logger.info("有新连接加入！ 当前在线人数" + onlineNumber);
        try {
            //messageType 1代表上线 2代表下线 3代表在线名单 4代表普通消息
            //把自己的信息加入到map当中去
            clients.put(userId, this);
            logger.info("当前在线人数" + clients.size());
            //给自己发一条消息：成功登陆，在线人数：clients.size()
            Map<String,Object> loginSuccessMsg = new HashMap<>();
            loginSuccessMsg.put("messageType", 1);  // 1表示上线消息
            loginSuccessMsg.put("message", "成功登陆");
            loginSuccessMsg.put("onlineCount", clients.size());
            loginSuccessMsg.put("当前排名", stringRedisTemplate.opsForZSet().rank("users_coin_count",userId)+1);
            sendMessageTo(JSON.toJSONString(loginSuccessMsg), userId);
        }
        catch (IOException e){
            logger.info(userId+"上线的时候通知所有人发生了错误");
        }



    }

    @OnError
    public void onError(Session session, Throwable error) {
        logger.info("服务端发生了错误"+error.getMessage());
        //error.printStackTrace();
    }
    /**
     * 连接关闭
     */
    @OnClose
    public void onClose()
    {
        onlineNumber--;
        //webSockets.remove(this);
        clients.remove(userId);

        logger.info("用户：" + userId + " 已下线，当前在线人数：" + onlineNumber);
    }

    /**
     * 收到客户端的消息
     *
     * @param message 消息
     * @param session 会话
     */
    @OnMessage
    public void onMessage(String message, Session session)
    {

    }


    public void sendMessageTo(String message, String TouserId) throws IOException {
        for (WebSocket item : clients.values()) {


            //    System.out.println("在线人员名单  ："+item.userId.toString());
            if (item.userId.equals(TouserId) ) {
                item.session.getAsyncRemote().sendText(message);

                break;
            }
        }
    }

    public void sendMessageAll(String message,String FromuserId) throws IOException {
        for (WebSocket item : clients.values()) {
            item.session.getAsyncRemote().sendText(message);
        }
    }

    //编写方法，发给一个string集合中的所有人
    public void sendMessageToUsers(String message, Set<String> userIds) throws IOException {
        for (String userId : userIds) {
            WebSocket item = clients.get(userId);
            if (item != null) {
                item.session.getAsyncRemote().sendText(message);
            }
        }
    }

    public static synchronized int getOnlineCount() {
        return onlineNumber;
    }


}