package org.example.lobbycenter.client.rpcClient.impl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.lobbycenter.client.rpcClient.RpcClient;
import org.example.common.Message.RpcRequest;
import org.example.common.Message.RpcResponse;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/*
    @author 张星宇
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SimpleSocketRpcClient implements RpcClient {
    private String host;
    private int port;
    @Override
    public RpcResponse sendRequest(RpcRequest request) {
        try {
            Socket socket = new Socket(host, port);
            ObjectOutputStream objectOutputStream =
                    new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectInputStream =
                    new ObjectInputStream(socket.getInputStream());
            objectOutputStream.writeObject(request);
            objectOutputStream.flush();
            return (RpcResponse) objectInputStream.readObject();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return  RpcResponse.fail("调用服务失败");
        }
    }

}
