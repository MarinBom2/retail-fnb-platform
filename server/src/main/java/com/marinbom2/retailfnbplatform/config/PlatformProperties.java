package com.marinbom2.retailfnbplatform.config;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 플랫폼 공통 설정 ({@code platform.*}).
 *
 * @param zone 영업일·시간대 매출을 자르는 기준 시간대. 매장별 시간대는 아직 없다
 */
@ConfigurationProperties("platform")
public record PlatformProperties(@DefaultValue("Asia/Seoul") String zone) {

    public ZoneId zoneId() {
        return ZoneId.of(zone);
    }
}
