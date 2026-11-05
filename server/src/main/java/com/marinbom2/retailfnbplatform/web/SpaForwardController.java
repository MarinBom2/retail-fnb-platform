package com.marinbom2.retailfnbplatform.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 화면 경로를 새로고침해도 SPA(index.html)가 뜨게 넘긴다 (md/runtime-structure.md 6절).
 * 화면 라우트 접두사만 적는다. /api, /assets, /actuator 와 확장자 있는 경로는 넘기지 않는다.
 */
@Controller
public class SpaForwardController {

    @GetMapping({"/login", "/stores", "/stores/**", "/brands", "/brands/**"})
    public String forward() {
        return "forward:/index.html";
    }
}
