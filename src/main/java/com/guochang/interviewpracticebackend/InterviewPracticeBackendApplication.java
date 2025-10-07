package com.guochang.interviewpracticebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
public class InterviewPracticeBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewPracticeBackendApplication.class, args);
    }

}
