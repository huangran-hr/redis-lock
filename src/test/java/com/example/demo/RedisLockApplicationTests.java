package com.example.demo;

import com.example.demo.service.ProductService;
import com.example.demo.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.PathVariable;

@SpringBootTest
class RedisLockApplicationTests {

    @Autowired
    private ProductService productService;

    @Test
    void contextLoads() {
    }

    @Test
    public void test() throws InterruptedException {

//        Thread[] threads = new Thread[500];
//        //起500个线程秒杀商品
//        for (int i = 0; i < 500; i++) {
//            threads[i] = new Thread(() -> productService.seckill(1L));
//            threads[i].start();
//        }
//        System.out.println("线程休眠===============" +Thread.activeCount());
//        Thread.sleep(1000L);

        productService.seckill(1L);

        System.out.println(ProductServiceImpl.productMap.get(1L));
    }
}
