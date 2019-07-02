# springboot-websocket-netty

#### 介绍
本项目是对springboot官方提供的websocket进行的netty版本封装，api与原版的完全一致，让广大springboot用户更方便的使用netty版本的websocket。netty与tomcat的相比，占用内存更小，效率更高，在特殊环境下，netty的效率是tomcat的20倍，想更轻松的使用netty版本的websocket，那么现在就来使用它吧！~~

#### 使用说明

1. 添加maven库
```xml
	<dependency>
        <groupId>com.simon</groupId>
        <artifactId>spring-boot-starter-websocket-netty</artifactId>
        <version>0.0.1</version>
    </dependency>
```
2. 代码示例
```java
    import com.simon.annotation.*;
    import com.simon.model.Session;
    import org.apache.commons.logging.Log;
    import org.apache.commons.logging.LogFactory;
    import org.springframework.stereotype.Component;
    
    
    import java.io.IOException;
    import java.util.concurrent.CopyOnWriteArraySet;
    
    @ServerEndpoint(port = 8081,path = "/websocket/{sid}")
    @Component
    public class NettyServer {
        static Log log= LogFactory.getLog(NettyServer.class);
        //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
        private static int onlineCount = 0;
        //concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
        private static CopyOnWriteArraySet<NettyServer> webSocketSet = new CopyOnWriteArraySet<NettyServer>();
    
        //与某个客户端的连接会话，需要通过它来给客户端发送数据
        private Session session;
    
        //接收sid
        private String sid="";
        /**
         * 连接建立成功调用的方法*/
        @OnOpen
        public void onOpen(Session session,@PathParam("sid") String sid) {
            this.session = session;
            webSocketSet.add(this);     //加入set中
            addOnlineCount();           //在线数加1
            log.info("有新窗口开始监听:"+sid+",当前在线人数为" + getOnlineCount());
            this.sid=sid;
            try {
                sendMessage("连接成功");
            } catch (IOException e) {
                log.error("websocket IO异常");
            }
        }
    
        /**
         * 连接关闭调用的方法
         */
        @OnClose
        public void onClose() {
            webSocketSet.remove(this);  //从set中删除
            subOnlineCount();           //在线数减1
            log.info("有一连接关闭！当前在线人数为" + getOnlineCount());
        }
    
        /**
         * 收到客户端消息后调用的方法
         *
         * @param message 客户端发送过来的消息*/
        @OnMessage
        public void onMessage(String message, Session session) throws IOException {
            log.info("收到来自窗口"+sid+"的信息:"+message);
            this.sendMessage(message);
        }
    
        /**
         *
         * @param session
         * @param error
         */
        @OnError
        public void onError(Session session, Throwable error) {
            log.error("发生错误");
            error.printStackTrace();
        }
        /**
         * 实现服务器主动推送
         */
        public void sendMessage(String message) throws IOException {
            this.session.sendText(message);
        }
    
    
        /**
         * 群发自定义消息
         * */
        public static void sendInfo(String message,String sid) throws IOException {
            log.info("推送消息到窗口"+sid+"，推送内容:"+message);
            for (NettyServer item : webSocketSet) {
                try {
                    //这里可以设定只推送给这个sid的，为null则全部推送
                    if(sid==null) {
                        item.sendMessage(message);
                    }else if(item.sid.equals(sid)){
                        item.sendMessage(message);
                    }
                } catch (IOException e) {
                    continue;
                }
            }
        }
    
        public static synchronized int getOnlineCount() {
            return onlineCount;
        }
    
        public static synchronized void addOnlineCount() {
            NettyServer.onlineCount++;
        }
    
        public static synchronized void subOnlineCount() {
            NettyServer.onlineCount--;
        }
```
3.测试demo:https://gitee.com/pxm6666/springboot-websocket


