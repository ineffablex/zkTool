package com.mytool.zktool;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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

    public void connect(String address) throws IOException, InterruptedException {
        try {
            CountDownLatch connectedSignal = new CountDownLatch(1);
            this.zooKeeper = new ZooKeeper(address, 30000, watchedEvent -> {
                if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    connectedSignal.countDown();
                }
                handleWatchedEvent(watchedEvent);
            });
            if (!connectedSignal.await(5, TimeUnit.SECONDS)) {
                throw new IOException("连接Zookeeper超时");
            }
            this.currentCluster = address;
        } catch (Exception e) {
            throw new IOException("无法连接到Zookeeper: " + e.getMessage());
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
        node.setCreateTime(LocalDateTime.now()); // TODO: Convert from stat.getCtime()
        node.setUpdateTime(LocalDateTime.now()); // TODO: Convert from stat.getMtime()
        node.setVersion(String.valueOf(stat.getVersion()));
        node.setStatus("connected");

        if (!children.isEmpty()) {
            List<NodeInfo> childNodes = new ArrayList<>();
            for (String child : children) {
                String childPath = path.equals("/") ? "/" + child : path + "/" + child;
                childNodes.add(getNode(childPath));
            }
            node.setChildren(childNodes);
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
            throw new IllegalStateException("ZooKeeper 连接未建立或已关闭");
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
        try {
            // 重新注册监听
            zooKeeper.exists(event.getPath(), true);
            
            // 通知相关监听器
            Set<ZkNodeListener> listeners = pathListeners.get(event.getPath());
            if (listeners != null) {
                NodeEvent nodeEvent = convertEventType(event.getType());
                for (ZkNodeListener listener : listeners) {
                    listener.onNodeEvent(event.getPath(), nodeEvent);
                }
            }
        } catch (Exception e) {
            // 处理异常
            e.printStackTrace();
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