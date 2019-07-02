package com.simon.netty;

import com.simon.model.PojoDoMethodHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.lang.reflect.InvocationTargetException;


/**
 * @author: Marsor
 */
public class WebsocketFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private PojoDoMethodHandler pojoDoMethodHandler;

    public WebsocketFrameHandler(PojoDoMethodHandler pojoDoMethodHandler){
        this.pojoDoMethodHandler = pojoDoMethodHandler;
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("与客户端建立连接，通道开启！");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws InvocationTargetException, IllegalAccessException {
        System.out.println("与客户端断开连接，通道关闭！");
        pojoDoMethodHandler.doClose(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws InvocationTargetException, IllegalAccessException {
        pojoDoMethodHandler.doMessage(ctx,msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws InvocationTargetException, IllegalAccessException {
        pojoDoMethodHandler.doError(ctx, cause);
    }
}
