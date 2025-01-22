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

    @GetMapping("/connect")
    public ResponseEntity<Void> connect(@RequestParam String address) {
        try {
            zkService.connect(address);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/nodes")
    public List<NodeInfo> getNodes(@RequestParam String cluster) {
        return zkService.getNodes(cluster);
    }
}