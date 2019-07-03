package com.simon.netty;


import com.simon.model.PojoDoMethodHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * netty版本的websocket
 */
public class NettyServer {
    //正则表达式初始化
    Pattern p = Pattern.compile("\\w*");
    private int port = 80;
    private String path = "/";
    private String wildcard = "";
    private PojoDoMethodHandler pojoDoMethodHandler;
    public NettyServer(int port,String path,PojoDoMethodHandler pojoDoMethodHandler){
        this.port = port;
        this.path = path;
        try{
            this.wildcard = path.substring(path.indexOf("{")+1,path.lastIndexOf("}"));
            Matcher m = p.matcher(this.wildcard);
            if(!m.matches()){
                throw new IllegalArgumentException("无效的路径,path:"+path);
            }
            this.path = this.path.replace("{","").replace("}","").replace(this.wildcard,"");
        }catch (Exception e){
            throw new IllegalArgumentException("无效的路径,path:"+path);
        }
        this.pojoDoMethodHandler = pojoDoMethodHandler;
    }

    public void start() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap sb = new ServerBootstrap();
            sb.option(ChannelOption.SO_BACKLOG, 1024);
            sb.group(group, bossGroup) // 绑定线程池
                    .channel(NioServerSocketChannel.class) // 指定使用的channel
                    .localAddress(this.port)// 绑定监听端口
                    .childHandler(new ChannelInitializer<SocketChannel>() { // 绑定客户端连接时候触发操作

                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            System.out.println("收到新连接");
                            //websocket协议本身是基于http协议的，所以这边也要使用http解编码器
                            ch.pipeline()
                                    .addLast(new ServerIdleStateHandler())
                                    .addLast(new HttpServerCodec())
                                    .addLast(new HttpObjectAggregator(65536))
                                    .addLast(new WebsocketFilterHandler(pojoDoMethodHandler,wildcard,path))
                                    .addLast(new WebSocketServerProtocolHandler(path,
                                            null, true))
                                    .addLast(new WebsocketFrameHandler(pojoDoMethodHandler))
                                    ;
                        }
                    });
            ChannelFuture cf = sb.bind().sync(); // 服务器异步创建绑定
            System.out.println(NettyServer.class + " 启动正在监听： " + cf.channel().localAddress());
            cf.channel().closeFuture().sync(); // 关闭服务器通道
        } finally {
            group.shutdownGracefully().sync(); // 释放线程池资源
            bossGroup.shutdownGracefully().sync();
        }
    }
}