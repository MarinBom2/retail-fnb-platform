package com.marinbom2.retailfnbplatform.config;

import com.marinbom2.retailfnbplatform.member.api.dto.MemberLoginRequest;
import com.marinbom2.retailfnbplatform.security.filter.MemberAuthenticationAPIFilter;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API 문서(Swagger UI) 설정. 로컬: http://localhost:9190/swagger-ui.html
 * 운영은 application-prod.yml 에서 문서와 UI 를 끈다. 컨트롤러 작성 규칙은 md/develop-principal.md §8.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI retailFnbOpenApi() {
        return new OpenAPI().info(new Info()
                .title("retail-fnb-platform API")
                .version("v1")
                .description("""
                        리테일·F&B 플랫폼 API. 경로는 /api/v1 아래에 둔다.
                        성공 응답은 DTO 그대로, 오류는 RFC 9457 ProblemDetail(application/problem+json)에 code(ErrorCode 이름)를 더해 보낸다.
                        인증은 HttpOnly 쿠키로 하므로, 같은 오리진에서 연 이 화면에서는 로그인 뒤 쿠키가 자동으로 붙는다.
                        """));
    }

    /**
     * 로그인은 컨트롤러가 아니라 시큐리티 필터(MemberAuthenticationAPIFilter)가 처리해서 springdoc 이 찾지 못한다.
     * 문서에서 빠지지 않게 직접 적어 넣는다.
     */
    @Bean
    public OpenApiCustomizer loginEndpointDocumentation() {
        return openApi -> {
            ModelConverters.getInstance().readAll(MemberLoginRequest.class)
                    .forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));

            Content problem = new Content().addMediaType("application/problem+json",
                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail")));

            Operation login = new Operation()
                    .tags(List.of("인증"))
                    .summary("로그인")
                    .description("FR-AU-002. 이메일·비밀번호를 확인하고 인증 쿠키(retailfnb_at, HttpOnly)를 내린다. "
                            + "시큐리티 필터가 처리한다")
                    .requestBody(new RequestBody().required(true).content(new Content().addMediaType("application/json",
                            new MediaType().schema(new Schema<>().$ref("#/components/schemas/MemberLoginRequest")))))
                    .responses(new ApiResponses()
                            .addApiResponse("200", new ApiResponse()
                                    .description("로그인 성공. Set-Cookie 로 인증 쿠키가 온다")
                                    .content(new Content().addMediaType("application/json",
                                            new MediaType().schema(new Schema<>().$ref("#/components/schemas/MemberResponse")))))
                            .addApiResponse("400", new ApiResponse().description("INVALID_REQUEST_DATA").content(problem))
                            .addApiResponse("401", new ApiResponse().description("LOGIN_FAILED").content(problem)));

            openApi.path(MemberAuthenticationAPIFilter.LOGIN_PATH, new PathItem().post(login));
        };
    }
}
