package com.mytool.zktool.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class BatchOperationRequest {
    public static class BatchNode {
        @NotNull(message = "节点路径不能为空")
        private String path;
        private String data;
        private Integer version;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }
    }

    @NotEmpty(message = "批量操作节点列表不能为空")
    private List<BatchNode> nodes;

    @NotEmpty(message = "操作类型不能为空")
    private String operation; // CREATE, DELETE, UPDATE

    // 用于导入/导出功能
    private Map<String, String> dataMap;
    
    // 用于数据同步功能
    private String sourceCluster;
    private String targetCluster;
    private List<String> paths;
}