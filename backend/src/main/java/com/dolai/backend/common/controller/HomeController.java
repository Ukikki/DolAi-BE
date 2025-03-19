package com.dolai.backend.common.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class HomeController {
    @GetMapping // GET 요청 받을 때 사용
    public String sayHello() {
        return "안녕 서버 잘 돌아감 http://localhost:8080/login 으로 들어가면 로그인을 할 수 있어";
    }
}
