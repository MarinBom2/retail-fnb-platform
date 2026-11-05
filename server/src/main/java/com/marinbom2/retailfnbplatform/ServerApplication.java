package com.marinbom2.retailfnbplatform;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API 서버 앱. 설정은 common:core 의 application.yml 하나를 server·batch 가 같이 쓴다.
 * 모듈마다 application.yml 을 두면 클래스패스에서 하나만 읽히므로, 모듈별 값은 여기서 기본값으로 넣는다.
 */
@SpringBootApplication
public class ServerApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ServerApplication.class);
        app.setDefaultProperties(Map.of("spring.application.name", "retail-fnb-platform"));
        app.run(args);
    }

}
