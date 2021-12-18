package com.example.demo.interceptor;

import com.example.demo.annotation.CacheLock;
import com.example.demo.annotation.LockedComplexObject;
import com.example.demo.annotation.LockedObject;
import com.example.demo.utils.RedisTool;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * @Auther: HR
 * @Date 2021/12/16 12:22
 * @Description:
 */
@Component
public class CacheLockInterceptor implements HandlerInterceptor {

    @Resource
    private JedisPool jedisPool;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(handler instanceof HandlerMethod) {
            HandlerMethod hm = (HandlerMethod)handler;
            Class<?> clazz = hm.getBeanType();
            Method method = hm.getMethod();
            MethodParameter[] args = hm.getMethodParameters();

            if(clazz!=null && method!=null) {
                if(!method.isAnnotationPresent(CacheLock.class)) {
                    return true;
                }
                CacheLock cacheLock = method.getAnnotation(CacheLock.class);

                String requestId = UUID.randomUUID().toString();
                request.setAttribute("requestId",requestId);

                //获取需要锁的参数
                String objectValue = "";
                for (MethodParameter methodParameter : args){
                    LockedComplexObject lockedComplexObject = methodParameter.getParameterAnnotation(LockedComplexObject.class);
                    if(lockedComplexObject != null){

                        objectValue = request.getParameterMap().get(lockedComplexObject.field())[0];
                        break;
                    }

                }
                request.setAttribute("objectValue",objectValue);

                Jedis jedis = jedisPool.getResource();

                Long nanoTime = System.nanoTime();
                Boolean b = false;
                while (System.nanoTime()-nanoTime<cacheLock.timeOut()*1000*1000*1000){
                    b = RedisTool.tryGetDistributedLock(jedis,cacheLock.lockedProfix()+objectValue,requestId,cacheLock.expireTime());
                    if(b) break;
                }
                if(!b){ //取锁失败
                    return false;
                }
                request.setAttribute("requestId",requestId);
                //加锁成功，执行方法
                return true;

            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if(handler instanceof HandlerMethod) {
            HandlerMethod hm = (HandlerMethod) handler;
            Class<?> clazz = hm.getBeanType();
            Method method = hm.getMethod();
            if (clazz != null && method != null) {
                CacheLock cacheLock = method.getAnnotation(CacheLock.class);
                if(cacheLock != null){
                    Jedis jedis = jedisPool.getResource();
                    //释放锁
                    RedisTool.releaseDistributedLock(jedis,cacheLock.lockedProfix()+request.getAttribute("objectValue").toString(),request.getAttribute("requestId").toString());
                }
            }
        }

    }

}
