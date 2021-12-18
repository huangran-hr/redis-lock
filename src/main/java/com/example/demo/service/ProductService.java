package com.example.demo.service;

import com.example.demo.annotation.CacheLock;
import com.example.demo.annotation.LockedObject;


/**
 * @Auther: HR
 * @Date 2021/12/16 11:55
 * @Description:
 */
public interface ProductService {
    @CacheLock(lockedProfix = "product_") //使用动态代理的方法时，CacheLock注解才加到接口方法中
    void seckill(@LockedObject Long productId);

}
