package com.example.demo.controller;

import com.example.demo.annotation.CacheLock;
import com.example.demo.annotation.LockedComplexObject;
import com.example.demo.annotation.LockedObject;
import com.example.demo.interceptor.CacheLockInterceptor;
import com.example.demo.interceptor.CacheLockProxyInterceptor;
import com.example.demo.service.ProductService;
import com.example.demo.service.impl.ProductServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Resource;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;

/**
 * @Auther: HR
 * @Date 2021/12/16 11:45
 * @Description:
 */
@RestController
public class ProductController {
    @Resource
    private JedisPool jedisPool;
    @Resource
    private ProductService productService;

    @RequestMapping("secKill")
    public void seckill(@RequestParam Long productId){
        productService.seckill(productId);
    }

    @RequestMapping("test")
    @CacheLock(lockedProfix = "test_")
    public void test(@RequestParam @LockedComplexObject(field = "productId") Long productId) throws InterruptedException {
        int subThreadNum = 50;
        CountDownLatch
                countDownLatch = new CountDownLatch(subThreadNum);
        Thread[] threads = new Thread[subThreadNum];
        //起500个线程秒杀商品
        for (int i = 0; i < subThreadNum; i++) {
            threads[i] = new Thread(() -> {
                try {
                    seckill(productId);
                }finally {
                    //线程结束时，将计时器减一
                    countDownLatch.countDown();
                }
            });
            threads[i].start();
        }
        Thread.sleep(2000L);
        countDownLatch.await();
        if(countDownLatch.getCount() == 0){
            System.out.println(ProductServiceImpl.productMap.get(productId));
        }

//        seckill(productId);
//        System.out.println(ProductServiceImpl.productMap.get(productId));
    }

    /**
     * 使用动态代理 得到分布式锁，加锁和释放锁
     * @param productId
     * @throws InterruptedException
     */
    @RequestMapping("proxy/test/{productId}")
    public void proxyTest(@PathVariable Long productId) throws InterruptedException {
//        int subThreadNum = 50;
//        CountDownLatch
//                countDownLatch = new CountDownLatch(subThreadNum);
//        Thread[] threads = new Thread[subThreadNum];
//
//        for (int i = 0; i < subThreadNum; i++) {
//            threads[i] = new Thread(() -> {
//                try {
//                    //使用动态代理方式调用seckill方法
//                    ProductService p = new ProductServiceImpl();
//                    ProductService productService =
//                            (ProductService) Proxy.newProxyInstance(ProductService.class.getClassLoader(),new Class[]{ProductService.class}, new CacheLockProxyInterceptor(jedisPool,p));
//                    productService.seckill(productId);
//                }finally {
//                    //线程结束时，将计时器减一
//                    countDownLatch.countDown();
//                    System.out.println("countDownLatch============"+ countDownLatch.getCount());
//                }
//            });
//            threads[i].start();
//        }
//        Thread.sleep(2000L);
//        countDownLatch.await();
//        if(countDownLatch.getCount() == 0){
//            System.out.println(ProductServiceImpl.productMap.get(productId));
//        }


        ProductService p = new ProductServiceImpl();
        //使用动态代理方式调用seckill方法
        ProductService productService =
                (ProductService) Proxy.newProxyInstance(p.getClass().getClassLoader(),ProductServiceImpl.class.getInterfaces(), new CacheLockProxyInterceptor(jedisPool,p));
        productService.seckill(productId);
        System.out.println(ProductServiceImpl.productMap.get(productId));



    }

}
