package com.example.demo.service.impl;

import com.example.demo.annotation.CacheLock;
import com.example.demo.annotation.LockedObject;
import com.example.demo.dto.Product;
import org.springframework.stereotype.Service;

import java.util.HashMap;

/**
 * @Auther: HR
 * @Date 2021/12/16 11:55
 * @Description:
 */
@Service
public class ProduService {

    public static HashMap<Long,Product> productMap;

    static {
        productMap = new HashMap<>();
        Product product = new Product();
        product.setId(1L);
        product.setStock(1000L);

        Product product2 = new Product();
        product2.setId(1L);
        product2.setStock(1000L);
        productMap.put(1L,product);
        productMap.put(2L,product2);
    }

    @CacheLock(lockedProfix = "product_")
    public void seckill(@LockedObject Long productId) {
        Product product = productMap.get(productId);
        product.setStock(productMap.get(productId).getStock()-1);
        productMap.put(productId,product);
    }


}
