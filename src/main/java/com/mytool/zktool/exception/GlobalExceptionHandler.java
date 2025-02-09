package com.mytool.zktool.exception;

import com.mytool.zktool.dto.ApiResponse;
import org.apache.zookeeper.KeeperException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(KeeperException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<String> handleZookeeperException(KeeperException ex) {
        String message = "ZooKeeper操作失败";
        if (ex instanceof KeeperException.NoNodeException) {
            message = "节点不存在";
        } else if (ex instanceof KeeperException.NodeExistsException) {
            message = "节点已存在";
        } else if (ex instanceof KeeperException.NoAuthException) {
            message = "没有操作权限";
        } else if (ex instanceof KeeperException.BadVersionException) {
            message = "节点版本不匹配";
        }
        return ApiResponse.error(message + ": " + ex.getMessage());
    }

    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<String> handleIOException(IOException ex) {
        return ApiResponse.error("网络或IO错误: " + ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<String> handleIllegalStateException(IllegalStateException ex) {
        return ApiResponse.error("操作状态错误: " + ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        return ApiResponse.error("请求参数验证失败", errors);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<String> handleGeneralException(Exception ex) {
        return ApiResponse.error("服务器内部错误: " + ex.getMessage());
    }
}