package com.mytool.zktool;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

@Service
public class ZkService {
    private ZooKeeper zooKeeper;
    private final CountDownLatch connectedSignal = new CountDownLatch(1);

    public void connect(String host) throws IOException, InterruptedException {
        this.zooKeeper = new ZooKeeper(host, 30000, watchedEvent -> {
            if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
                connectedSignal.countDown();
            }
        });
        connectedSignal.await();
    }

    public void close() throws InterruptedException {
        if (zooKeeper != null) {
            zooKeeper.close();
        }
    }

    private ZooKeeper zooKeeper;
    private String currentCluster;

    public void connect(String address) throws IOException, InterruptedException {
        CountDownLatch connectedSignal = new CountDownLatch(1);
        this.zooKeeper = new ZooKeeper(address, 30000, watchedEvent -> {
            if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
                connectedSignal.countDown();
            }
        });
        connectedSignal.await();
        this.currentCluster = address;
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