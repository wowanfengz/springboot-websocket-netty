package com.simon.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author: Marsor
 */
public class ServerIdleStateHandler extends IdleStateHandler {

    private static final int READER_IDLE_TIME = 60*60*5;
    private static final Logger log = LoggerFactory.getLogger(ServerIdleStateHandler.class);

    public ServerIdleStateHandler() {
        super(READER_IDLE_TIME, 0, 0, TimeUnit.SECONDS);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) {
        log.warn("连接{}{}秒内未读到数据，关闭连接", ctx.channel(), READER_IDLE_TIME);
        ctx.channel().close();
    }
}
