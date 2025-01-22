package com.mytool.zktool;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
public class ZkService {
    private ZooKeeper zooKeeper;
    private final CountDownLatch connectedSignal = new CountDownLatch(1);
    private String currentCluster;


    public void close() throws InterruptedException {
        if (zooKeeper != null) {
            zooKeeper.close();
        }
    }

    public void connect(String address) throws IOException, InterruptedException {
        try {
            CountDownLatch connectedSignal = new CountDownLatch(1);
            this.zooKeeper = new ZooKeeper(address, 30000, watchedEvent -> {
                if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    connectedSignal.countDown();
                }
            });
            if (!connectedSignal.await(5, TimeUnit.SECONDS)) {
                throw new IOException("连接Zookeeper超时");
            }
            this.currentCluster = address;
        } catch (Exception e) {
            throw new IOException("无法连接到Zookeeper: " + e.getMessage());
        }
    }

    public List<NodeInfo> getNodes(String cluster) {
        if (!cluster.equals(currentCluster)) {
            throw new IllegalStateException("Cluster not connected");
        }

        List<NodeInfo> nodes = new ArrayList<>();
        try {
            List<String> children = zooKeeper.getChildren("/", false);
            for (String child : children) {
                NodeInfo node = new NodeInfo();
                node.setName(child);
                node.setStatus("connected");
                node.setIp("127.0.0.1"); // TODO: Get actual IP
                node.setConnectionTime(LocalDateTime.now());
                nodes.add(node);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get nodes", e);
        }
        return nodes;
    }
}