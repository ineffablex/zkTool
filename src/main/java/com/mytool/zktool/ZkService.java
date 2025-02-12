package com.mytool.zktool;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class ZkService {
    private ZooKeeper zooKeeper;
    private final CountDownLatch connectedSignal = new CountDownLatch(1);
    private String currentCluster;
    private final Map<String, Set<ZkNodeListener>> pathListeners = new ConcurrentHashMap<>();
    private final Map<String, byte[]> dataCache = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(ZkService.class);

    public void connect(String address) throws IOException, InterruptedException {
        logger.info("开始连接ZooKeeper集群: {}", address);
        try {
            if (zooKeeper != null && zooKeeper.getState().isAlive()) {
                logger.info("关闭已存在的连接");
                close();
            }

            CountDownLatch connectionLatch = new CountDownLatch(1);
            this.zooKeeper = new ZooKeeper(address, 30000, event -> {
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    logger.info("ZooKeeper连接已建立");
                    connectionLatch.countDown();
                } else if (event.getState() == Watcher.Event.KeeperState.Expired) {
                    logger.warn("ZooKeeper会话已过期");
                } else if (event.getState() == Watcher.Event.KeeperState.Disconnected) {
                    logger.warn("ZooKeeper连接已断开");
                }
                handleWatchedEvent(event);
            });

            logger.info("等待ZooKeeper连接建立...");
            if (!connectionLatch.await(5, TimeUnit.SECONDS)) {
                throw new IOException("连接ZooKeeper超时");
            }

            // 验证连接
            try {
                zooKeeper.exists("/", false);
                this.currentCluster = address;
                logger.info("成功连接到ZooKeeper集群");
            } catch (KeeperException e) {
                throw new IOException("连接验证失败: " + e.getMessage());
            }
        } catch (Exception e) {
            logger.error("连接ZooKeeper失败: {}", e.getMessage(), e);
            if (zooKeeper != null) {
                try {
                    zooKeeper.close();
                } catch (InterruptedException ie) {
                    logger.warn("关闭失败的连接时出错", ie);
                }
                zooKeeper = null;
            }
            throw new IOException("无法连接到ZooKeeper: " + e.getMessage());
        }
    }

    public void close() throws InterruptedException {
        if (zooKeeper != null) {
            zooKeeper.close();
        }
        pathListeners.clear();
        dataCache.clear();
    }

    // 节点基础操作
    public NodeInfo getNode(String path) throws KeeperException, InterruptedException {
        validateConnection();
        Stat stat = new Stat();
        byte[] data = zooKeeper.getData(path, true, stat);
        List<String> children = zooKeeper.getChildren(path, true);
        List<ACL> acls = zooKeeper.getACL(path, stat);

        NodeInfo node = new NodeInfo();
        node.setPath(path);
        node.setName(getNodeName(path));
        node.setData(data != null ? new String(data, StandardCharsets.UTF_8) : null);
        node.setAcls(acls);
        node.setStat(stat);
        node.setCreateTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(stat.getCtime()), ZoneId.systemDefault()));
        node.setUpdateTime(LocalDateTime.ofInstant(Instant.ofEpochMilli(stat.getMtime()), ZoneId.systemDefault()));
        node.setVersion(String.valueOf(stat.getVersion()));
        node.setStatus("connected");
        node.setExpanded(false);
        node.setSelected(false);
        node.setIsLeaf(children.isEmpty());

        if (!children.isEmpty()) {
            List<NodeInfo> childNodes = new ArrayList<>();
            for (String child : children) {
                String childPath = path.equals("/") ? "/" + child : path + "/" + child;
                childNodes.add(getNode(childPath));
            }
            node.setChildren(childNodes);
        } else {
            node.setChildren(new ArrayList<>());
        }

        return node;
    }

    public void createNode(String path, String data, CreateMode mode) throws KeeperException, InterruptedException {
        validateConnection();
        byte[] bytes = data != null ? data.getBytes(StandardCharsets.UTF_8) : new byte[0];
        zooKeeper.create(path, bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, mode);
        notifyListeners(path, NodeEvent.CREATED);
    }

    public void updateNode(String path, String data, int version) throws KeeperException, InterruptedException {
        validateConnection();
        byte[] bytes = data != null ? data.getBytes(StandardCharsets.UTF_8) : new byte[0];
        zooKeeper.setData(path, bytes, version);
        notifyListeners(path, NodeEvent.UPDATED);
    }

    public void deleteNode(String path, int version) throws KeeperException, InterruptedException {
        validateConnection();
        zooKeeper.delete(path, version);
        notifyListeners(path, NodeEvent.DELETED);
    }

    // 批量操作
    public void batchCreate(List<NodeInfo> nodes) throws KeeperException, InterruptedException {
        validateConnection();
        for (NodeInfo node : nodes) {
            createNode(node.getPath(), node.getData(), CreateMode.PERSISTENT);
        }
    }

    public void batchDelete(List<String> paths) throws KeeperException, InterruptedException {
        validateConnection();
        // 按路径长度降序排序，确保先删除子节点
        paths.sort((a, b) -> b.length() - a.length());
        for (String path : paths) {
            Stat stat = zooKeeper.exists(path, false);
            if (stat != null) {
                deleteNode(path, stat.getVersion());
            }
        }
    }

    // 数据导入导出
    public Map<String, String> exportData(String path) throws KeeperException, InterruptedException {
        validateConnection();
        Map<String, String> data = new HashMap<>();
        exportNodeRecursive(path, data);
        return data;
    }

    public void importData(Map<String, String> data) throws KeeperException, InterruptedException {
        validateConnection();
        // 按路径长度升序排序，确保先创建父节点
        List<String> paths = new ArrayList<>(data.keySet());
        paths.sort(Comparator.comparingInt(String::length));
        for (String path : paths) {
            Stat stat = zooKeeper.exists(path, false);
            if (stat == null) {
                createNode(path, data.get(path), CreateMode.PERSISTENT);
            } else {
                updateNode(path, data.get(path), stat.getVersion());
            }
        }
    }

    // 监听器管理
    public void addListener(String path, ZkNodeListener listener) throws KeeperException, InterruptedException {
        validateConnection();
        pathListeners.computeIfAbsent(path, k -> new HashSet<>()).add(listener);
        // 设置监听
        zooKeeper.exists(path, true);
    }

    public void removeListener(String path) {
        pathListeners.remove(path);
    }

    // 工具方法
    private void validateConnection() {
        if (zooKeeper == null || !zooKeeper.getState().isAlive()) {
            logger.error("ZooKeeper连接未建立或已关闭");
            throw new IllegalStateException("ZooKeeper连接未建立或已关闭");
        }
    }

    private String getNodeName(String path) {
        if (path.equals("/")) return "/";
        int lastSlash = path.lastIndexOf('/');
        return lastSlash < 0 ? path : path.substring(lastSlash + 1);
    }

    private void exportNodeRecursive(String path, Map<String, String> data) throws KeeperException, InterruptedException {
        byte[] nodeData = zooKeeper.getData(path, false, null);
        if (nodeData != null) {
            data.put(path, new String(nodeData, StandardCharsets.UTF_8));
        }

        List<String> children = zooKeeper.getChildren(path, false);
        for (String child : children) {
            String childPath = path.equals("/") ? "/" + child : path + "/" + child;
            exportNodeRecursive(childPath, data);
        }
    }

    private void handleWatchedEvent(WatchedEvent event) {
        if (event == null) {
            logger.warn("收到空的Watch事件");
            return;
        }

        logger.debug("收到Watch事件: type={}, state={}, path={}", 
                    event.getType(), 
                    event.getState(), 
                    event.getPath());

        // 只处理有路径的事件
        if (event.getPath() == null) {
            return;
        }
        
        try {
            // 重新注册监听器
            zooKeeper.exists(event.getPath(), true);
            // 通知相关监听器
            notifyListeners(event.getPath(), convertEventType(event.getType()));
        } catch (Exception e) {
            logger.error("处理Watch事件失败: {}", e.getMessage(), e);
        }
    }

    private NodeEvent convertEventType(Watcher.Event.EventType eventType) {
        switch (eventType) {
            case NodeCreated:
                return NodeEvent.CREATED;
            case NodeDeleted:
                return NodeEvent.DELETED;
            case NodeDataChanged:
                return NodeEvent.UPDATED;
            default:
                return NodeEvent.UNKNOWN;
        }
    }

    private void notifyListeners(String path, NodeEvent event) {
        // 获取当前路径的监听器
        Set<ZkNodeListener> listeners = pathListeners.get(path);
        if (listeners != null) {
            // 遍历通知所有监听器
            for (ZkNodeListener listener : listeners) {
                try {
                    listener.onNodeEvent(path, event);
                } catch (Exception e) {
                    // 捕获并记录监听器执行过程中的异常，但不影响其他监听器
                    e.printStackTrace();
                }
            }
        }

        // 通知父节点的监听器
        String parentPath = getParentPath(path);
        if (parentPath != null) {
            Set<ZkNodeListener> parentListeners = pathListeners.get(parentPath);
            if (parentListeners != null) {
                for (ZkNodeListener listener : parentListeners) {
                    try {
                        listener.onNodeEvent(path, event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // 更新缓存
        try {
            if (event == NodeEvent.DELETED) {
                dataCache.remove(path);
            } else if (event == NodeEvent.CREATED || event == NodeEvent.UPDATED) {
                byte[] data = zooKeeper.getData(path, true, null);
                dataCache.put(path, data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getParentPath(String path) {
        if (path == null || path.equals("/")) {
            return null;
        }
        int lastSlashIndex = path.lastIndexOf('/');
        if (lastSlashIndex == 0) {
            return "/";
        }
        return path.substring(0, lastSlashIndex);
    }
}

enum NodeEvent {
    CREATED,
    UPDATED,
    DELETED,
    UNKNOWN
}

interface ZkNodeListener {
    void onNodeEvent(String path, NodeEvent event);
}