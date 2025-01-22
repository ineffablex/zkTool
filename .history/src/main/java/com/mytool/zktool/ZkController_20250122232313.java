package com.mytool.zktool;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/zk")
public class ZkController {

    @GetMapping("/clusters")
    public String[] getClusters() {
        // TODO: Implement cluster list
        return new String[]{"cluster1", "cluster2"};
    }
}