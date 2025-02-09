package com.mytool.zktool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mytool.zktool.dto.NodeRequest;
import org.apache.zookeeper.CreateMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ZkController.class)
public class ZkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ZkService zkService;

    @Test
    void testConnect() throws Exception {
        // 准备
        String address = "localhost:2181";
        doNothing().when(zkService).connect(address);

        // 执行和验证
        mockMvc.perform(get("/api/zk/connect")
                .param("address", address))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("成功连接到 " + address));

        verify(zkService).connect(address);
    }

    @Test
    void testGetNode() throws Exception {
        // 准备
        String path = "/test";
        NodeInfo node = new NodeInfo();
        node.setPath(path);
        node.setName("test");
        node.setData("test data");
        when(zkService.getNode(path)).thenReturn(node);

        // 执行和验证
        mockMvc.perform(get("/api/zk/nodes")
                .param("path", path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.path").value(path))
                .andExpect(jsonPath("$.data.name").value("test"))
                .andExpect(jsonPath("$.data.data").value("test data"));
    }

    @Test
    void testCreateNode() throws Exception {
        // 准备
        NodeRequest request = new NodeRequest();
        request.setPath("/test");
        request.setData("test data");
        request.setMode("PERSISTENT");

        // 执行和验证
        mockMvc.perform(post("/api/zk/nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(zkService).createNode(eq("/test"), eq("test data"), eq(CreateMode.PERSISTENT));
    }

    @Test
    void testUpdateNode() throws Exception {
        // 准备
        NodeRequest request = new NodeRequest();
        request.setPath("/test");
        request.setData("updated data");
        request.setVersion(1);

        // 执行和验证
        mockMvc.perform(put("/api/zk/nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(zkService).updateNode(eq("/test"), eq("updated data"), eq(1));
    }

    @Test
    void testDeleteNode() throws Exception {
        // 准备
        String path = "/test";
        int version = 1;

        // 执行和验证
        mockMvc.perform(delete("/api/zk/nodes")
                .param("path", path)
                .param("version", String.valueOf(version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(zkService).deleteNode(path, version);
    }

    @Test
    void testExportData() throws Exception {
        // 准备
        String path = "/test";
        Map<String, String> data = new HashMap<>();
        data.put("/test", "test data");
        data.put("/test/child", "child data");
        when(zkService.exportData(path)).thenReturn(data);

        // 执行和验证
        mockMvc.perform(get("/api/zk/nodes/export")
                .param("path", path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data./test").value("test data"))
                .andExpect(jsonPath("$.data./test/child").value("child data"));
    }

    @Test
    void testImportData() throws Exception {
        // 准备
        Map<String, String> data = new HashMap<>();
        data.put("/test", "test data");
        data.put("/test/child", "child data");

        // 执行和验证
        mockMvc.perform(post("/api/zk/nodes/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(zkService).importData(data);
    }

    @Test
    void testValidationError() throws Exception {
        // 准备
        NodeRequest request = new NodeRequest();
        request.setPath("invalid-path"); // 缺少前导/
        request.setData("test data");

        // 执行和验证
        mockMvc.perform(post("/api/zk/nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("请求参数验证失败"));
    }
}