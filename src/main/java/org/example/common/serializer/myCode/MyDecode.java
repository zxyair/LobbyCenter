package org.example.common.serializer.myCode;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.AllArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.example.common.Message.MessageType;
import org.example.common.serializer.Serializer;

import java.util.List;

/*
    @author 张星宇
 */
@Slf4j
@AllArgsConstructor
public class MyDecode extends ByteToMessageDecoder {


    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext,
                          ByteBuf byteBuf, List<Object> out) throws Exception {
        try {
            System.out.println("[DEBUG] 开始解码，可读字节数：" + byteBuf.readableBytes());

            short messageType = byteBuf.readShort();
            System.out.println("[DEBUG] 消息类型：" + messageType);
            if(messageType!= MessageType.REQUEST.getCode() && messageType!= MessageType.RESPONSE.getCode()){
                System.out.println("messageType is not right");
                return;
            }
            short serializerType=byteBuf.readShort();
            if(serializerType!=0 && serializerType!=1){
                System.out.println("serializerType is not right");
                return;
            }
            Serializer serializer=Serializer.getSerializerByCode(serializerType);
            int length=byteBuf.readInt();
            byte[] bytes=new byte[length];
            byteBuf.readBytes(bytes);
            Object deserializer=serializer.deserialize(bytes,messageType);
            out.add(deserializer);
        } catch (Exception e) {
            System.err.println("解码异常：");
            e.printStackTrace();
            throw e;
        }
    }
}
