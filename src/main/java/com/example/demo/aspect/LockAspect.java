package com.example.demo.aspect;

import com.example.demo.annotation.AddLock;
import com.example.demo.annotation.LockedComplexObject;
import com.example.demo.annotation.LockedObject;
import com.example.demo.utils.RedisTool;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * @Auther: HR
 * @Date 2022/2/18 16:35
 * @Description:
 */
@Aspect
@Component
public class LockAspect {

    @Resource
    private JedisPool jedisPool;

    @Pointcut("@annotation(com.example.demo.annotation.AddLock)")
    public void addLockAspect(){

    }

    @Around(value = "addLockAspect()")
    public Object around(ProceedingJoinPoint proceedingJoinPoint) {
        try {
            //获取自定义注解
            MethodSignature methodSignature =
                    (MethodSignature) proceedingJoinPoint.getSignature();
            Method method = methodSignature.getMethod();
            AddLock addLock = method.getAnnotation(AddLock.class);

            Annotation[][] annotations = method.getParameterAnnotations();
            Object lockedObject = getLockedObject(annotations,proceedingJoinPoint.getArgs());
            String lockKey = lockedObject.toString();

            Jedis
                    jedis = jedisPool.getResource();
            String requestId = UUID.randomUUID().toString();
            //加锁
            boolean b = lock(addLock,lockKey,jedis);

            if(b){
                //进入方法中
                Object o = proceedingJoinPoint.proceed();
                //释放锁
                unLock(addLock,lockKey,jedis,requestId);
                return o;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 加锁
     * @param cacheLock
     * @param objectValue
     * @param jedis
     * @return
     */
    public boolean lock(AddLock cacheLock,String objectValue,Jedis jedis){
        System.out.println(Thread.currentThread().getId()+"==准备取锁");
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
            return false;
        }
        //加锁成功，执行方法
        System.out.println(Thread.currentThread().getId()+"==取锁成功");
        return true;
    }

    /**
     * 释放锁
     * @param cacheLock
     * @param objectValue
     * @param jedis
     * @param requestId
     */
    public void unLock(AddLock cacheLock,String objectValue,Jedis jedis,String requestId){
        //释放锁
        RedisTool.releaseDistributedLock(jedis,cacheLock.lockedProfix()+objectValue,requestId);
        System.out.println(Thread.currentThread().getId()+"==释放锁成功");
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
