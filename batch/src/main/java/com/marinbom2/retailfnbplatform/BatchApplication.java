package com.marinbom2.retailfnbplatform;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 배치 앱. 패키지를 루트(com.marinbom2.retailfnbplatform)에 둬서 common:core 의 엔티티·리포지토리·설정까지 스캔한다.
 * 배치 작업은 com.marinbom2.retailfnbplatform.batch.* 아래에 둔다.
 * 설정은 common:core 의 application.yml 을 같이 쓰고, 모듈별 값은 여기서 기본값으로 넣는다.
 */
@SpringBootApplication
public class BatchApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(BatchApplication.class);
        app.setDefaultProperties(Map.of("spring.application.name", "batch"));
        app.run(args);
    }

}
