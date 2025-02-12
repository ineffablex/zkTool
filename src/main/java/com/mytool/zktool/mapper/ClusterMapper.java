package com.mytool.zktool.mapper;

import com.mytool.zktool.entity.Cluster;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface ClusterMapper {
    List<Cluster> findAll();
    Cluster findById(Long id);
    int insert(Cluster cluster);
    int update(Cluster cluster);
    int delete(Long id);
} 