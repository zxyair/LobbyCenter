package org.example.common.serializer.myCode;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.AllArgsConstructor;

import org.example.common.Message.RpcResponse;
import org.example.common.serializer.impl.JsonSerializer;
import org.example.common.Message.MessageType;
import org.example.common.serializer.Serializer;
import org.example.common.Message.RpcRequest;

/*
    @author 张星宇
 */
@AllArgsConstructor
public class MyEncode extends MessageToByteEncoder {
    private Serializer serializer;

    public MyEncode(JsonSerializer jsonSerializer) {
    }

    @Override
    protected void encode(ChannelHandlerContext ctx,
                          Object message, ByteBuf out) throws Exception {
        System.out.println(message.getClass());
        if(message instanceof RpcRequest) out.writeShort(MessageType.REQUEST.getCode());
        else if(message instanceof RpcResponse){
            out.writeShort(MessageType.RESPONSE.getCode());
        }
        out.writeShort(serializer.getType());
        System.out.println(serializer.getType());
        byte[] seriliazeBytes = serializer.serialize(message);
        out.writeInt(seriliazeBytes.length);
        System.out.println(seriliazeBytes.length);
        out.writeBytes(seriliazeBytes);
        System.out.println("完成编码");

    }
}
