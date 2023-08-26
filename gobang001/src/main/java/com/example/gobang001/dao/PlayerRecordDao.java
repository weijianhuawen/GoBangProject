package com.example.gobang001.dao;

import com.example.gobang001.mode.PlayerRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PlayerRecordDao {
    public List<PlayerRecord> selectPlayerRecordByUid(Integer uid);
}
