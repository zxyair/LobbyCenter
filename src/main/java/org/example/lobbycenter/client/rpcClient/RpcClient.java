package org.example.lobbycenter.client.rpcClient;

import org.example.common.Message.RpcRequest;
import org.example.common.Message.RpcResponse;

/*
    @author 张星宇
 */
public interface RpcClient {
    RpcResponse sendRequest(RpcRequest request);
}
