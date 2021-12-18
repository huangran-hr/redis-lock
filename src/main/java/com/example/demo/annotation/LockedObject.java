package com.example.demo.annotation;

import java.lang.annotation.*;

/**
 * @Auther: HR
 * @Date 2021/12/16 11:38
 * @Description: 参数级的注解，用于注解商品ID等基本类型的参数
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LockedObject {
}
