package com.dolai.backend.common.controller;

import com.dolai.backend.common.exception.CustomException;
import com.dolai.backend.common.exception.ErrorCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ErrorCodeTestController {

    @GetMapping("/test/error")
    public void throwError(@RequestParam(name = "type") String type) {
        if ("bad_request".equals(type)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        } else if ("token_not_found".equals(type)) {
            throw new CustomException(ErrorCode.TOKEN_NOT_FOUND);
        } else if ("server_error".equals(type)) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
