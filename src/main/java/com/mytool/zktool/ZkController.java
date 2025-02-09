package com.mytool.zktool;

import com.mytool.zktool.dto.ApiResponse;
import com.mytool.zktool.dto.BatchOperationRequest;
import com.mytool.zktool.dto.NodeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/zk")
@Tag(name = "ZooKeeper管理", description = "提供ZooKeeper节点的管理和操作功能")
public class ZkController {

    @Autowired
    private ZkService zkService;

    @Operation(summary = "连接到ZooKeeper集群")
    @GetMapping("/connect")
    public ApiResponse<String> connect(@RequestParam String address) {
        try {
            zkService.connect(address);
            return ApiResponse.success("成功连接到 " + address);
        } catch (Exception e) {
            return ApiResponse.error("连接失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取节点信息")
    @GetMapping("/nodes")
    public ApiResponse<NodeInfo> getNode(@RequestParam String path) {
        try {
            NodeInfo node = zkService.getNode(path);
            return ApiResponse.success(node);
        } catch (Exception e) {
            return ApiResponse.error("获取节点信息失败: " + e.getMessage());
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
    @PutMapping("/nodes")
    public ApiResponse<String> updateNode(@Valid @RequestBody NodeRequest request) {
        try {
            zkService.updateNode(request.getPath(), request.getData(), request.getVersion());
            return ApiResponse.success("节点更新成功");
        } catch (Exception e) {
            return ApiResponse.error("更新节点失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除节点")
    @DeleteMapping("/nodes")
    public ApiResponse<String> deleteNode(@RequestParam String path, @RequestParam(required = false) Integer version) {
        try {
            zkService.deleteNode(path, version != null ? version : -1);
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