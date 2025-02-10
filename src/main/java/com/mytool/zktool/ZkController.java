package com.mytool.zktool;

import com.mytool.zktool.dto.ApiResponse;
import com.mytool.zktool.dto.BatchOperationRequest;
import com.mytool.zktool.dto.NodeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/zk")
@Tag(name = "ZooKeeper管理", description = "提供ZooKeeper节点的管理和操作功能")
public class ZkController {
    private static final Logger logger = LoggerFactory.getLogger(ZkController.class);

    @Autowired
    private ZkService zkService;

    @Operation(summary = "连接到ZooKeeper集群")
    @GetMapping("/connect")
    public ApiResponse<String> connect(@RequestParam String address) {
        if (address == null || address.trim().isEmpty()) {
            logger.warn("连接地址为空");
            return ApiResponse.error("连接地址不能为空");
        }

        logger.info("尝试连接到ZooKeeper集群: {}", address);
        try {
            zkService.connect(address);
            String successMsg = String.format("成功连接到ZooKeeper集群: %s", address);
            logger.info(successMsg);
            return ApiResponse.success(successMsg);
        } catch (Exception e) {
            String errorMsg = String.format("连接ZooKeeper集群失败: %s, 原因: %s", address, e.getMessage());
            logger.error(errorMsg, e);
            return ApiResponse.error(errorMsg);
        }
    }

    @Operation(summary = "获取节点信息")
    @GetMapping("/nodes")
    public ApiResponse<NodeInfo> getNode(
            @RequestParam(required = false, defaultValue = "/") String path,
            @RequestParam(required = false) String cluster) {
        
        logger.info("获取节点信息: path={}, cluster={}", path, cluster);
        
        try {
            // 如果指定了集群地址，先尝试连接
            if (cluster != null && !cluster.trim().isEmpty()) {
                zkService.connect(cluster);
            }
            
            NodeInfo rootNode = zkService.getNode(path);
            logger.info("成功获取节点信息: {}", rootNode);
            return ApiResponse.success(rootNode);
        } catch (Exception e) {
            String errorMsg = String.format("获取节点[%s]信息失败: %s", path, e.getMessage());
            logger.error(errorMsg, e);
            return ApiResponse.error(errorMsg);
        }
    }

    @Operation(summary = "创建节点")
    @PostMapping("/nodes")
    public ApiResponse<String> createNode(@Valid @RequestBody NodeRequest request) {
        try {
            CreateMode mode = CreateMode.valueOf(request.getMode());
            zkService.createNode(request.getPath(), request.getData(), mode);
            return ApiResponse.success("节点创建成功");
        } catch (Exception e) {
            return ApiResponse.error("创建节点失败: " + e.getMessage());
        }
    }

    @Operation(summary = "更新节点")
    @PutMapping("/nodes/update")
    public ApiResponse<String> updateNode(@Valid @RequestBody NodeRequest request) {
        try {
            zkService.updateNode(request.getPath(), request.getData(), request.getVersion());
            return ApiResponse.success("节点更新成功");
        } catch (Exception e) {
            return ApiResponse.error("更新节点失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除节点")
    @DeleteMapping("/nodes/delete")
    public ApiResponse<String> deleteNode(@Valid @RequestBody NodeRequest request) {
        try {
            // 获取节点信息，检查是否有子节点
            NodeInfo node = zkService.getNode(request.getPath());
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                // 如果有子节点，先删除所有子节点
                for (NodeInfo child : node.getChildren()) {
                    zkService.deleteNode(child.getPath(), -1);
                }
            }
            
            // 删除当前节点
            zkService.deleteNode(request.getPath(), request.getVersion() != null ? request.getVersion() : -1);
            return ApiResponse.success("节点删除成功");
        } catch (Exception e) {
            return ApiResponse.error("删除节点失败: " + e.getMessage());
        }
    }

    @Operation(summary = "批量操作节点")
    @PostMapping("/nodes/batch")
    public ApiResponse<String> batchOperation(@Valid @RequestBody BatchOperationRequest request) {
        try {
            switch (request.getOperation().toUpperCase()) {
                case "CREATE":
                    zkService.batchCreate(request.getNodes().stream()
                            .map(node -> {
                                NodeInfo info = new NodeInfo();
                                info.setPath(node.getPath());
                                info.setData(node.getData());
                                return info;
                            })
                            .toList());
                    break;
                case "DELETE":
                    zkService.batchDelete(request.getNodes().stream()
                            .map(BatchOperationRequest.BatchNode::getPath)
                            .toList());
                    break;
                default:
                    return ApiResponse.error("不支持的操作类型");
            }
            return ApiResponse.success("批量操作成功");
        } catch (Exception e) {
            return ApiResponse.error("批量操作失败: " + e.getMessage());
        }
    }

    @Operation(summary = "导出节点数据")
    @GetMapping("/nodes/export")
    public ApiResponse<Map<String, String>> exportData(@RequestParam String path) {
        try {
            Map<String, String> data = zkService.exportData(path);
            return ApiResponse.success(data);
        } catch (Exception e) {
            return ApiResponse.error("导出数据失败: " + e.getMessage());
        }
    }

    @Operation(summary = "导入节点数据")
    @PostMapping("/nodes/import")
    public ApiResponse<String> importData(@RequestBody Map<String, String> data) {
        try {
            zkService.importData(data);
            return ApiResponse.success("数据导入成功");
        } catch (Exception e) {
            return ApiResponse.error("导入数据失败: " + e.getMessage());
        }
    }

    @Operation(summary = "添加节点监听器")
    @PostMapping("/nodes/watch")
    public ApiResponse<String> addNodeListener(@RequestParam String path) {
        try {
            zkService.addListener(path, (watchedPath, event) -> 
                System.out.println("Node event: " + event + " on path: " + watchedPath));
            return ApiResponse.success("监听器添加成功");
        } catch (Exception e) {
            return ApiResponse.error("添加监听器失败: " + e.getMessage());
        }
    }

    @Operation(summary = "移除节点监听器")
    @DeleteMapping("/nodes/watch")
    public ApiResponse<String> removeNodeListener(@RequestParam String path) {
        try {
            zkService.removeListener(path);
            return ApiResponse.success("监听器移除成功");
        } catch (Exception e) {
            return ApiResponse.error("移除监听器失败: " + e.getMessage());
        }
    }
}