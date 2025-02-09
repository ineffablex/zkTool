package com.mytool.zktool;

import org.apache.zookeeper.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.zookeeper.data.Stat;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ZkServiceTest {

    @Mock
    private ZooKeeper zooKeeper;

    @InjectMocks
    private ZkService zkService;

    private static final String TEST_PATH = "/test";
    private static final String TEST_DATA = "test_data";

    @BeforeEach
    void setUp() throws Exception {
        // 模拟ZooKeeper连接
        when(zooKeeper.getState()).thenReturn(ZooKeeper.States.CONNECTED);
    }

    @Test
    void testCreateNode() throws Exception {
        // 准备
        when(zooKeeper.create(eq(TEST_PATH), any(), any(), eq(CreateMode.PERSISTENT)))
                .thenReturn(TEST_PATH);

        // 执行
        zkService.createNode(TEST_PATH, TEST_DATA, CreateMode.PERSISTENT);

        // 验证
        verify(zooKeeper).create(eq(TEST_PATH), 
                               eq(TEST_DATA.getBytes()),
                               eq(ZooDefs.Ids.OPEN_ACL_UNSAFE),
                               eq(CreateMode.PERSISTENT));
    }

    @Test
    void testGetNode() throws Exception {
        // 准备
        Stat stat = new Stat();
        when(zooKeeper.getData(eq(TEST_PATH), eq(true), any(Stat.class)))
                .thenReturn(TEST_DATA.getBytes());
        when(zooKeeper.getChildren(eq(TEST_PATH), eq(true)))
                .thenReturn(Arrays.asList("child1", "child2"));

        // 执行
        NodeInfo node = zkService.getNode(TEST_PATH);

        // 验证
        assertNotNull(node);
        assertEquals(TEST_PATH, node.getPath());
        assertEquals(TEST_DATA, node.getData());
        verify(zooKeeper).getData(eq(TEST_PATH), eq(true), any(Stat.class));
        verify(zooKeeper).getChildren(eq(TEST_PATH), eq(true));
    }

    @Test
    void testUpdateNode() throws Exception {
        // 准备
        int version = 1;
        when(zooKeeper.setData(eq(TEST_PATH), any(), eq(version)))
                .thenReturn(new Stat());

        // 执行
        zkService.updateNode(TEST_PATH, TEST_DATA, version);

        // 验证
        verify(zooKeeper).setData(eq(TEST_PATH),
                                eq(TEST_DATA.getBytes()),
                                eq(version));
    }

    @Test
    void testDeleteNode() throws Exception {
        // 准备
        int version = 1;

        // 执行
        zkService.deleteNode(TEST_PATH, version);

        // 验证
        verify(zooKeeper).delete(eq(TEST_PATH), eq(version));
    }

    @Test
    void testBatchCreate() throws Exception {
        // 准备
        List<NodeInfo> nodes = Arrays.asList(
            createTestNode("/test1", "data1"),
            createTestNode("/test2", "data2")
        );

        // 执行
        zkService.batchCreate(nodes);

        // 验证
        verify(zooKeeper, times(2))
            .create(anyString(), any(), any(), eq(CreateMode.PERSISTENT));
    }

    @Test
    void testExportData() throws Exception {
        // 准备
        when(zooKeeper.getData(eq(TEST_PATH), eq(false), isNull()))
                .thenReturn(TEST_DATA.getBytes());
        when(zooKeeper.getChildren(eq(TEST_PATH), eq(false)))
                .thenReturn(Arrays.asList("child1", "child2"));

        // 执行
        var result = zkService.exportData(TEST_PATH);

        // 验证
        assertNotNull(result);
        assertEquals(TEST_DATA, result.get(TEST_PATH));
        verify(zooKeeper).getData(eq(TEST_PATH), eq(false), isNull());
        verify(zooKeeper).getChildren(eq(TEST_PATH), eq(false));
    }

    private NodeInfo createTestNode(String path, String data) {
        NodeInfo node = new NodeInfo();
        node.setPath(path);
        node.setData(data);
        return node;
    }

    @Test
    void testConnectionFailure() {
        // 准备
        when(zooKeeper.getState()).thenReturn(ZooKeeper.States.CLOSED);

        // 执行和验证
        assertThrows(IllegalStateException.class, () -> 
            zkService.getNode(TEST_PATH)
        );
    }
}