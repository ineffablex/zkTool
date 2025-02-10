package com.mytool.zktool;

import lombok.Data;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class NodeInfo {
    private String path;            // 节点路径
    private String name;            // 节点名称
    private String data;            // 节点数据
    private List<ACL> acls;         // 访问控制列表
    private Stat stat;              // 节点状态
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private String version;         // 版本号
    private String status;          // 节点状态
    private List<NodeInfo> children; // 子节点列表
    private boolean expanded;       // 是否展开
    private boolean selected;       // 是否选中
    private boolean isLeaf;         // 是否为叶子节点

    public boolean getIsLeaf() {
        return children == null || children.isEmpty();
    }

    public void setIsLeaf(boolean isLeaf) {
        this.isLeaf = isLeaf;
    }
}