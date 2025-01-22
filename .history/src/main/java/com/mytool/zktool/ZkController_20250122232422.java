package com.mytool.zktool;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/zk")
public class ZkController {

    @Autowired
    private ZkService zkService;

    @GetMapping("/clusters")
    public String[] getClusters() {
        return zkService.getClusters();
    }
}