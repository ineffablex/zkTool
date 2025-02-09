package com.mytool.zktool.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class NodeRequest {
    @NotBlank(message = "节点路径不能为空")
    @Pattern(regexp = "^/.*", message = "节点路径必须以/开头")
    private String path;
    
    private String data;
    
    private Integer version;
    
    private String mode = "PERSISTENT";
}