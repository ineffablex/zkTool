package com.mytool.zktool;

import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.time.LocalDateTime;
import java.util.List;

public class NodeInfo {
    private String path;           // 节点完整路径
    private String name;           // 节点名称
    private String data;           // 节点数据
    private List<ACL> acls;       // 访问控制列表
    private Stat stat;            // 节点状态信息
    private List<NodeInfo> children; // 子节点列表
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private String version;        // 数据版本
    private String status;         // 节点状态
    private String ip;            // 节点IP地址

    // Getters and Setters
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public List<ACL> getAcls() {
        return acls;
    }

    public void setAcls(List<ACL> acls) {
        this.acls = acls;
    }

    public Stat getStat() {
        return stat;
    }

    public void setStat(Stat stat) {
        this.stat = stat;
    }

    public List<NodeInfo> getChildren() {
        return children;
    }

    public void setChildren(List<NodeInfo> children) {
        this.children = children;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}