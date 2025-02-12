package com.mytool.zktool.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.mytool.zktool.entity.Cluster;
import com.mytool.zktool.service.ClusterService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/clusters")
public class ClusterController {
    
    private static final Logger logger = LoggerFactory.getLogger(ClusterController.class);
    
    @Autowired
    private ClusterService clusterService;
    
    @GetMapping("")
    public List<Cluster> getAllClusters() {
        try {
            logger.info("Receiving request to get all clusters");
            List<Cluster> clusters = clusterService.getAllClusters();
            logger.info("Found {} clusters", clusters.size());
            return clusters;
        } catch (Exception e) {
            logger.error("Error getting all clusters: ", e);
            throw e;
        }
    }
    
    @GetMapping("/{id}")
    public Cluster getClusterById(@PathVariable Long id) {
        logger.info("Receiving request to get cluster with id: {}", id);
        return clusterService.getClusterById(id);
    }
    
    @PostMapping
    public Cluster createCluster(@RequestBody Cluster cluster) {
        logger.info("Receiving request to create cluster: {}", cluster);
        return clusterService.createCluster(cluster);
    }
    
    @PutMapping("/{id}")
    public Cluster updateCluster(@PathVariable Long id, @RequestBody Cluster cluster) {
        logger.info("Receiving request to update cluster with id: {}", id);
        return clusterService.updateCluster(id, cluster);
    }
    
    @DeleteMapping("/{id}")
    public void deleteCluster(@PathVariable Long id) {
        logger.info("Receiving request to delete cluster with id: {}", id);
        clusterService.deleteCluster(id);
    }
} 