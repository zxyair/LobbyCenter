package org.example.lobbycenter.server;

import org.example.lobbycenter.server.provider.ServiceProvider;
import org.example.lobbycenter.server.services.RpcServer;
import org.example.lobbycenter.server.services.impl.NettyRPCServer;
import org.example.lobbycenter.service.ILobbyMatchService;
import org.example.lobbycenter.service.ILobbyRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/*
    @author 张星宇
 */
@Component
public class TestServer {
    @Qualifier("lobbyMatchServiceImpl")
    @Autowired
    private ILobbyMatchService lobbyMatchService;

    @Qualifier("lobbyRoomServiceImpl")
    @Autowired
    private ILobbyRoomService lobbyRoomService;

    @PostConstruct
    public void initRpcServer(){

        new Thread(() -> {
            ServiceProvider serviceProvider = new ServiceProvider("127.0.0.1", 9999);
            serviceProvider.providerServiceProvider(lobbyRoomService, true);
            RpcServer rpcServer = new NettyRPCServer(serviceProvider);
            rpcServer.start(9999); // 这里面如果有阻塞，没关系，因为在新线程里
        }).start();
    }

}
