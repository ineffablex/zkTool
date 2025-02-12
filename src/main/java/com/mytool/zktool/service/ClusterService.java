package com.mytool.zktool.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mytool.zktool.entity.Cluster;
import com.mytool.zktool.mapper.ClusterMapper;
import java.util.List;

@Service
public class ClusterService {
    
    @Autowired
    private ClusterMapper clusterMapper;
    
    public List<Cluster> getAllClusters() {
        return clusterMapper.findAll();
    }
    
    public Cluster getClusterById(Long id) {
        return clusterMapper.findById(id);
    }
    
    public Cluster createCluster(Cluster cluster) {
        clusterMapper.insert(cluster);
        return cluster;
    }
    
    public Cluster updateCluster(Long id, Cluster cluster) {
        cluster.setId(id);
        if (clusterMapper.update(cluster) > 0) {
            return cluster;
        }
        return null;
    }
    
    public void deleteCluster(Long id) {
        clusterMapper.delete(id);
    }
} 