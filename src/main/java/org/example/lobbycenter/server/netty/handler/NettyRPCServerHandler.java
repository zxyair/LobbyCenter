package org.example.lobbycenter.server.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.AllArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.example.common.Message.RpcRequest;
import org.example.common.Message.RpcResponse;
import org.example.common.pojo.Result;
import org.example.lobbycenter.server.provider.ServiceProvider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.example.common.util.ResponseConverter.convertToResponse;


/*
    @author 张星宇
 */
@Slf4j
@AllArgsConstructor
public class NettyRPCServerHandler extends SimpleChannelInboundHandler<RpcRequest> {
    private ServiceProvider serviceProvide;

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext,
                                RpcRequest rpcRequest) throws Exception {
        RpcResponse response=getResponse(rpcRequest);
        //打印相应信息
        System.out.println("接收到请求，返回响应,响应信息：" + response);
        channelHandlerContext.writeAndFlush(response).addListener(future -> {
            if (future.isSuccess()) {
                log.info("响应发送成功: {}", response);
            } else {
                log.error("响应发送失败", future.cause());
                // 可以在这里添加重试逻辑或其他错误处理
            }
        });
        channelHandlerContext.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private RpcResponse getResponse(RpcRequest rpcRequest) {
        //得到服务名
        String interfaceName = rpcRequest.getInterfaceName();
//        RateLimit rataLimit = serviceProvide.rateLimitProvider.getRateLimit(interfaceName);
//        if(!rataLimit.getToken()){
//            return RpcResponse.fail("请求太快，请稍后再试");
//        }
        //得到服务端相应服务实现类
        Object service = serviceProvide.getServiceProvider(interfaceName);
        log.info("调用方法：" + rpcRequest.getMethodName());
        //反射调用方法
        Method method = null;
        try {
            method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamsType());
            Result invoke = (Result) method.invoke(service, rpcRequest.getParams());
            log.info("方法执行成功，返回结果：" + invoke);
            return convertToResponse(invoke);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
            log.error("方法调用错误", e);
            Result errorResult = Result.fail("方法调用错误: " + e.getMessage());
            return convertToResponse(errorResult);
        } catch (InvocationTargetException e) {
            Throwable targetEx = e.getTargetException();
            log.error("业务方法执行错误", targetEx);
            Result errorResult = Result.fail("业务错误: " + targetEx.getMessage());
            return convertToResponse(errorResult);
        }
    }
}
