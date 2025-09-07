package com.qiandoudou.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 用于配置静态资源映射
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // 获取项目根目录的绝对路径
        String projectRoot = System.getProperty("user.dir");
        String imagesPath = "file:" + projectRoot + "/qiandoudou-frontend/images/";
        String uploadsPath = "file:" + projectRoot + "/uploads/";
        String audioPath = "file:" + projectRoot + "/uploads/audio/";
        String videoPath = "file:" + projectRoot + "/uploads/video/";
        
        // 配置静态资源映射
        registry.addResourceHandler("/images/**")
                .addResourceLocations(imagesPath);
        
        // 配置上传文件访问路径
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsPath);
                
        // 配置音频文件访问路径
        registry.addResourceHandler("/audio/**")
                .addResourceLocations(audioPath);
                
        // 配置视频文件访问路径
        registry.addResourceHandler("/video/**")
                .addResourceLocations(videoPath);
                
        System.out.println("静态资源配置完成:");
        System.out.println("- /images/** -> " + imagesPath);
        System.out.println("- /uploads/** -> " + uploadsPath);
        System.out.println("- /audio/** -> " + audioPath);
        System.out.println("- /video/** -> " + videoPath);
        System.out.println("项目根目录: " + projectRoot);
    }
}
