package org.example.controller.exception;

import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.RestBean;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice // 这个注解用于全局处理RESTful抛出的错误, 标识一个类作为全局异常处理器
public class ValidationController {

    @ExceptionHandler(ValidationException.class)
    public RestBean<Void> validateException(ValidationException exception) {
        log.warn("Resolve [{} : {}]", exception.getClass().getName(), exception.getMessage());
        return RestBean.failure(400, "请求参数有误");
    }
}
