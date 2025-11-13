package com.sobolev.spring.filemanageruniversity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;

@SpringBootApplication(exclude = {
    ServletWebServerFactoryAutoConfiguration.class,
    DispatcherServletAutoConfiguration.class
})
public class FileManagerUniversityApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(FileManagerUniversityApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }

}
