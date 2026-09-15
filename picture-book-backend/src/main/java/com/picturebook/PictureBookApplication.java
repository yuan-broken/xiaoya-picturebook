package com.picturebook;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 小芽绘本 AI 伴读成长空间 - 后端启动入口
 */
@SpringBootApplication
@MapperScan("com.picturebook.**.mapper")
public class PictureBookApplication {
    public static void main(String[] args) {
        SpringApplication.run(PictureBookApplication.class, args);
    }
}
