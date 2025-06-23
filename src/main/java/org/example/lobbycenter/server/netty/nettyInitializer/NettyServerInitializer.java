package org.example.lobbycenter.server.netty.nettyInitializer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import lombok.AllArgsConstructor;
import org.example.common.serializer.impl.ObjectSerializer;
import org.example.common.serializer.myCode.MyDecode;
import org.example.common.serializer.myCode.MyEncode;
import org.example.lobbycenter.server.netty.handler.NettyRPCServerHandler;
import org.example.lobbycenter.server.provider.ServiceProvider;


/*
    @author 张星宇
 */
@AllArgsConstructor
public class NettyServerInitializer extends ChannelInitializer<SocketChannel> {

    private ServiceProvider serviceProvider;
//netty自带编码解码器，JDK序列化，通过handler添加长度字段解决粘包半包问题
/*    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(
                new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,0,
                        4,0,4));
        pipeline.addLast(new LengthFieldPrepender(4));
        pipeline.addLast(new ObjectEncoder());
        pipeline.addLast(new ObjectDecoder(new ClassResolver() {
            @Override
            public Class<?> resolve(String className) throws ClassNotFoundException {
                return Class.forName(className);
            }
        }));
        pipeline.addLast(new NettyRPCServerHandler(serviceProvider));
    }*/

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline.addLast(new MyDecode());
        pipeline.addLast(new MyEncode(new ObjectSerializer()));
        pipeline.addLast(new NettyRPCServerHandler(serviceProvider));
    }
}
