package com.example.demo.annotation;

import java.lang.annotation.*;

/**
 * @Auther: HR
 * @Date 2021/12/16 11:33
 * @Description:
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheLock {
    String lockedProfix() default ""; //redis 锁key的前缀
    long timeOut() default 10; //轮询超时时间 10s
    int expireTime() default 1000; //key的过期时间 1000s
}
