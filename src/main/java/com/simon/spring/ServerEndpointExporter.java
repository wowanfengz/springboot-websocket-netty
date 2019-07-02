package com.simon.spring;

import com.simon.annotation.ServerEndpoint;
import com.simon.exception.DeploymentException;
import com.simon.model.PojoDoMethodHandler;
import com.simon.model.PojoMethodMapping;
import com.simon.model.WebsocketAddress;
import com.simon.netty.NettyServer;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Yeauty
 * @version 1.0
 */
public class ServerEndpointExporter extends ApplicationObjectSupport implements SmartInitializingSingleton {

    @Autowired
    Environment environment;

    private final Map<WebsocketAddress, NettyServer> nettyServerHashMap = new HashMap<>();

    @Override
    public void afterSingletonsInstantiated() {
        registerEndpoints();
    }
    protected void registerEndpoints() {
        Set<Class<?>> endpointClasses = new LinkedHashSet<>();

        ApplicationContext context = getApplicationContext();
        if (context != null) {
            String[] endpointBeanNames = context.getBeanNamesForAnnotation(ServerEndpoint.class);
            for (String beanName : endpointBeanNames) {
                endpointClasses.add(context.getType(beanName));
            }
        }

        for (Class<?> endpointClass : endpointClasses) {
            registerEndpoint(endpointClass);
        }
        init();
    }

    private void init() {
        for (Map.Entry<WebsocketAddress, NettyServer> entry : nettyServerHashMap.entrySet()) {
            NettyServer nettyServer = entry.getValue();
            new Thread(() -> {
                try {
                    nettyServer.start();
                } catch (Exception e) {
                    logger.error("netty服务器启动异常",e);
                }
            }).start();
        }
    }

    private void registerEndpoint(Class<?> endpointClass) {
        ServerEndpoint annotation = AnnotatedElementUtils.findMergedAnnotation(endpointClass, ServerEndpoint.class);
        if (annotation == null) {
            throw new IllegalStateException("missingAnnotation ServerEndpoint");
        }
        ApplicationContext context = getApplicationContext();
        PojoDoMethodHandler pojoDoMethodHandler = null;
        try {
            PojoMethodMapping pojoMethodMapping = new PojoMethodMapping(endpointClass, context);
            pojoDoMethodHandler = new PojoDoMethodHandler(pojoMethodMapping);
        } catch (DeploymentException e) {
            e.printStackTrace();
        }

        WebsocketAddress websocketAddress = getParam(annotation);
        NettyServer nettyServer = nettyServerHashMap.get(websocketAddress);
        if(nettyServer!=null){
            logger.error("端口已被占用:"+websocketAddress.getPort());
        }
        else{
            nettyServer = new NettyServer(websocketAddress.getPort(),websocketAddress.getPath(),pojoDoMethodHandler);
            nettyServerHashMap.put(websocketAddress,nettyServer);
        }
    }

    private WebsocketAddress getParam(ServerEndpoint annotation){
        WebsocketAddress websocketAddress = new WebsocketAddress();
        websocketAddress.setPath(annotation.path());
        websocketAddress.setPort(annotation.port());
        return websocketAddress;
    }

}
