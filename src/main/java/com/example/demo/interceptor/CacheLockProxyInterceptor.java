package com.example.demo.interceptor;

import com.example.demo.annotation.CacheLock;
import com.example.demo.annotation.LockedComplexObject;
import com.example.demo.annotation.LockedObject;
import com.example.demo.service.ProductService;
import com.example.demo.utils.RedisTool;
import org.springframework.beans.factory.annotation.Qualifier;
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
 * @Description: 代理方式，需要使用动态代理
 */
@Component
public class CacheLockProxyInterceptor implements InvocationHandler {

    private JedisPool jedisPool;

    private ProductService
            productService;

    public CacheLockProxyInterceptor(JedisPool jedisPool,ProductService productService){
        this.jedisPool = jedisPool;
        this.productService = productService;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//        return method.invoke(productService,args);
        CacheLock cacheLock = method.getAnnotation(CacheLock.class);
        if(cacheLock == null){
            return method.invoke(productService,args);
        }
        Annotation[][] annotations = method.getParameterAnnotations();
        Object lockedObject = getLockedObject(annotations,args);
        String objectValue = lockedObject.toString();
        Jedis jedis = jedisPool.getResource();

        String requestId = UUID.randomUUID().toString();
        Long nanoTime = System.nanoTime();
        Boolean b = false;
//        b = RedisTool.tryGetDistributedLock(jedis,cacheLock.lockedProfix()+objectValue,requestId,cacheLock.expireTime());
        while (System.nanoTime()-nanoTime<cacheLock.timeOut()*1000*1000*1000){
            b = RedisTool.tryGetDistributedLock(jedis,cacheLock.lockedProfix()+objectValue,requestId,cacheLock.expireTime());
            if(b) break;
        }
        if(!b){ //取锁失败
            System.out.println(Thread.currentThread().getId()+"==取锁失败");
            return null;
        }else {
            try {
                //加锁成功，执行方法
                System.out.println(Thread.currentThread().getId()+"==取锁成功");
                return method.invoke(productService,args);
            }finally {
                //释放锁
                System.out.println(Thread.currentThread().getId()+"==释放锁成功");
                RedisTool.releaseDistributedLock(jedis,cacheLock.lockedProfix()+objectValue,requestId);
            }
        }
    }

    /**
     * 根据获取到的参数注解和参数列表获得加锁的参数
     * @param annotations
     * @param args
     * @return
     */
    private Object getLockedObject(Annotation[][] annotations, Object[] args) throws Exception {

        if(args == null || args.length == 0 ){
            throw new Exception("方法参数为空");
        }

        if(annotations == null || annotations.length == 0 ){
            throw new Exception("没有被注解的参数");
        }
        //不支持多个参数加锁，只支持第一个注解为LockedObject或者LockedComplexObject的参数
        int index = -1;
        for (int i = 0; i < annotations.length; i++) {
            for (int j = 0; j < annotations[i].length; j++) {
                if(annotations[i][j] instanceof LockedComplexObject){
                    index = i;
                    return args[i].getClass().getField(((LockedComplexObject)annotations[i][j]).field());
                }

                if(annotations[i][j] instanceof LockedObject){
                    index = i;
                    break;
                }
            }

            //找到第一个后直接break，不支持多参数加锁
            if(index != -1){
                break;
            }
        }

        if(index == -1){
            throw new Exception("请指定被锁定的参数");
        }

        return args[index];
    }
}
