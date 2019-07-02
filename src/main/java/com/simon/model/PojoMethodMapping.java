package com.simon.model;

import com.simon.annotation.*;
import com.simon.exception.DeploymentException;
import io.netty.handler.codec.http.HttpHeaders;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class PojoMethodMapping {

    private final Method onOpen;
    private final Method onClose;
    private final Method onError;
    private final Method onMessage;
    private final PojoPathParam[] onOpenParams;
    private final PojoPathParam[] onCloseParams;
    private final PojoPathParam[] onErrorParams;
    private final PojoPathParam[] onMessageParams;
    private final Class pojoClazz;
    private final ApplicationContext applicationContext;

    public PojoMethodMapping(Class<?> pojoClazz, ApplicationContext context) throws DeploymentException {
        this.applicationContext = context;
        this.pojoClazz = pojoClazz;
        Method open = null;
        Method close = null;
        Method error = null;
        Method message = null;
        Method[] pojoClazzMethods = pojoClazz.getDeclaredMethods();
        for (Method method : pojoClazzMethods) {
            if (method.getAnnotation(OnOpen.class) != null) {
                checkPublic(method);
                open = method;
            } else if (method.getAnnotation(OnClose.class) != null) {
                checkPublic(method);
                close = method;
            } else if (method.getAnnotation(OnError.class) != null) {
                checkPublic(method);
                error = method;
            } else if (method.getAnnotation(OnMessage.class) != null) {
                checkPublic(method);
                message = method;
            } else {
                // Method not annotated
            }
        }
        this.onOpen = open;
        this.onClose = close;
        this.onError = error;
        this.onMessage = message;
        onOpenParams = getPathParams(onOpen, MethodType.ON_OPEN);
        onCloseParams = getPathParams(onClose, MethodType.ON_CLOSE);
        onErrorParams = getPathParams(onError, MethodType.ON_ERROR);
        onMessageParams = getPathParams(onMessage, MethodType.ON_MESSAGE);
    }

    private void checkPublic(Method m) throws DeploymentException {
        if (!Modifier.isPublic(m.getModifiers())) {
            throw new DeploymentException(
                    "pojoMethodMapping.methodNotPublic " + m.getName());
        }
    }

    public Object getEndpointInstance() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Object implement = pojoClazz.getDeclaredConstructor().newInstance();
        AutowiredAnnotationBeanPostProcessor postProcessor = applicationContext.getBean(AutowiredAnnotationBeanPostProcessor.class);
        postProcessor.postProcessPropertyValues(null, null, implement, null);
        return implement;
    }

    public Method getOnOpen() {
        return onOpen;
    }

    public Object[] getOnOpenArgs(Session session, HttpHeaders headers, String pathParam) {
        return buildArgs(onOpenParams, session, headers, pathParam, null, null, null);
    }

    public Method getOnClose() {
        return onClose;
    }

    public Object[] getOnCloseArgs(Session session) {
        return buildArgs(onCloseParams, session, null, null, null, null, null);
    }

    public Method getOnError() {
        return onError;
    }

    public Object[] getOnErrorArgs(Session session, Throwable throwable) {
        return buildArgs(onErrorParams, session, null, null, null, throwable, null);
    }

    public Method getOnMessage() {
        return onMessage;
    }

    public Object[] getOnMessageArgs(Session session, String text) {
        return buildArgs(onMessageParams, session, null, text, null, null, null);
    }
    private static PojoPathParam[] getPathParams(Method m, MethodType methodType){
        if (m == null) {
            return new PojoPathParam[0];
        }
        Class<?>[] types = m.getParameterTypes();
        PojoPathParam[] result = new PojoPathParam[types.length];
        for (int i = 0; i < types.length; i++) {
            Class<?> type = types[i];
            if(methodType == MethodType.ON_OPEN){
                if(type.equals(Session.class)){
                    result[i] = new PojoPathParam(type,"session");
                }
                else if(type.equals(String.class)){
                    result[i] = new PojoPathParam(type,"pathParam");
                }
            }
            else if(methodType == MethodType.ON_CLOSE){
                return new PojoPathParam[0];
            }
            else if(methodType == MethodType.ON_MESSAGE){
                if(type.equals(Session.class)){
                    result[i] = new PojoPathParam(type,"session");
                }
                else if(type.equals(String.class)){
                    result[i] = new PojoPathParam(type,"message");
                }
            }
            else if(methodType == MethodType.ON_ERROR){
                if(type.equals(Session.class)){
                    result[i] = new PojoPathParam(type,"session");
                }
                else if(type.equals(Throwable.class)){
                    result[i] = new PojoPathParam(type,"throwable");
                }
            }
        }
        return result;
    }

    private static Object[] buildArgs(PojoPathParam[] pathParams, Session session,
                                      HttpHeaders headers, String text, byte[] bytes,
                                      Throwable throwable, Object evt) {
        Object[] result = new Object[pathParams.length];
        for (int i = 0; i < pathParams.length; i++) {
            Class<?> type = pathParams[i].getType();
            if (type.equals(Session.class)) {
                result[i] = session;
            } else if (type.equals(HttpHeaders.class)) {
                result[i] = headers;
            } else if (type.equals(String.class)) {
                result[i] = text;
            } else if (type.equals(byte[].class)) {
                result[i] = bytes;
            } else if (type.equals(Throwable.class)) {
                result[i] = throwable;
            } else if (type.equals(Object.class)) {
                result[i] = evt;
            }
        }
        return result;
    }

    private enum MethodType {
        ON_OPEN,
        ON_CLOSE,
        ON_MESSAGE,
        ON_ERROR
    }
}