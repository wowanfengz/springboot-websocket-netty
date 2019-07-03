package com.simon.netty;

import com.simon.model.PojoDoMethodHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author: Marsor
 */
public class WebsocketFilterHandler extends ChannelInboundHandlerAdapter {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(WebsocketFilterHandler.class);
    private PojoDoMethodHandler pojoDoMethodHandler;
    private String wildcard;
    private String serverPath;
    public WebsocketFilterHandler(PojoDoMethodHandler pojoDoMethodHandler,String wildcard,String serverPath){
        this.pojoDoMethodHandler = pojoDoMethodHandler;
        this.wildcard = wildcard;
        this.serverPath = serverPath;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        final FullHttpRequest req = (FullHttpRequest) msg;
        String uri = req.uri();
        String pathParam = uri.substring(uri.lastIndexOf('/') + 1);
        String uriDomain = uri.replace(pathParam,"");
        System.out.println("pathParam:"+pathParam);
        req.setUri(uriDomain);
        // 协议升级完成后不需要
        ctx.pipeline().remove(this);
        super.channelRead(ctx, msg);

        //执行onOpen方法
        //判断是否按照通配符格式,如果按照则传参，否则传null
        if(serverPath.equals(uriDomain)) {
            pojoDoMethodHandler.doOpen(ctx, pathParam,wildcard);
        }
        else{
            pojoDoMethodHandler.doOpen(ctx, null,null);
            logger.error("暂时通配符仅支持放在url最后,path:"+serverPath);
        }
    }
}
