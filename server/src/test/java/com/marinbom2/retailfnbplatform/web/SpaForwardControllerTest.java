package com.marinbom2.retailfnbplatform.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** 화면 경로를 새로고침해도 index.html 로 넘긴다. client routes.ts 에 화면 접두사를 더하면 여기도 더한다 */
class SpaForwardControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new SpaForwardController()).build();

    @ParameterizedTest
    @ValueSource(strings = {"/login", "/stores", "/stores/1/pos", "/brands", "/brands/3/products"})
    void forwardsScreenRoutes(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
    }
}
