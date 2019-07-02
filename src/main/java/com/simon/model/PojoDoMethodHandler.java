package com.simon.model;

import com.simon.annotation.PathParam;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class PojoDoMethodHandler {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PojoDoMethodHandler.class);
    private static final AttributeKey<Session> SESSION_KEY = AttributeKey.valueOf("WEBSOCKET_SESSION");
    private static final AttributeKey POJO_KEY = AttributeKey.valueOf("WEBSOCKET_IMPLEMENT");
    private PojoMethodMapping pojoMethodMapping;

    public PojoDoMethodHandler(PojoMethodMapping pojoMethodMapping){
        this.pojoMethodMapping = pojoMethodMapping;
    }

    public void doOpen(ChannelHandlerContext ctx,String pathParam,String wildcard) throws InvocationTargetException,IllegalAccessException {
        Method onOpenMethod =pojoMethodMapping.getOnOpen();
        Channel channel = ctx.channel();
        Attribute attrPojo = channel.attr(POJO_KEY);
        //将类实例放到参数中去，让不同handler使用同一个实例
        Object implement = null;
        try {
            implement = pojoMethodMapping.getEndpointInstance();
            attrPojo.set(implement);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        Attribute<Session> attrSession = channel.attr(SESSION_KEY);
        Session session = null;
        try {
            session = new Session(channel);
            attrSession.set(session);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //查找通配符是否在继承的方法中写有注解，且注解的value与通配符值一致
        if(findParamAnnotation(onOpenMethod.getParameters(),wildcard)) {
            onOpenMethod.invoke(implement, pojoMethodMapping.getOnOpenArgs(session, null, pathParam));
        }
        else{
            onOpenMethod.invoke(implement, pojoMethodMapping.getOnOpenArgs(session, null, null));
        }
    }

    public void doClose(ChannelHandlerContext ctx) throws InvocationTargetException, IllegalAccessException {
        Method onCloseMethod = pojoMethodMapping.getOnClose();
        Object implement = ctx.channel().attr(POJO_KEY).get();
        Session session = ctx.channel().attr(SESSION_KEY).get();
        if (session == null ) {
            logger.error("session is null");
            return;
        }
        if (implement == null){
            logger.error("implement is null");
            return;
        }
        onCloseMethod.invoke(implement,pojoMethodMapping.getOnCloseArgs(session));
    }

    public void doMessage(ChannelHandlerContext ctx, TextWebSocketFrame frame) throws InvocationTargetException, IllegalAccessException {
        Method onMessageMethod = pojoMethodMapping.getOnMessage();
        Object implement = ctx.channel().attr(POJO_KEY).get();
        Session session = ctx.channel().attr(SESSION_KEY).get();
        if (session == null ) {
            logger.error("session is null");
            return;
        }
        if (implement == null){
            logger.error("implement is null");
            return;
        }
        onMessageMethod.invoke(implement, pojoMethodMapping.getOnMessageArgs(session, frame.text()));
    }

    public void doError(ChannelHandlerContext ctx, Throwable throwable) throws InvocationTargetException, IllegalAccessException {
        Method onErrorMethod = pojoMethodMapping.getOnError();
        Object implement = ctx.channel().attr(POJO_KEY).get();
        Session session = ctx.channel().attr(SESSION_KEY).get();
        if (session == null ) {
            logger.error("session is null");
            return;
        }
        if (implement == null){
            logger.error("implement is null");
            return;
        }
        onErrorMethod.invoke(implement, pojoMethodMapping.getOnErrorArgs(session,throwable));
    }

    private boolean findParamAnnotation(Parameter[] parameters,String wildcard){
        String value = null;
        for(Parameter parameter:parameters){
            PathParam pathParam = parameter.getAnnotation(PathParam.class);
            if(pathParam!=null && pathParam.value().equals(wildcard)){
                return true;
            }
        }
        return false;
    }
}
