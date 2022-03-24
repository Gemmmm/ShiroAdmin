package com.howei.shiroadmin;

    import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.howei.shiroadmin.dao")
public class ShiroAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShiroAdminApplication.class, args);
    }
}
