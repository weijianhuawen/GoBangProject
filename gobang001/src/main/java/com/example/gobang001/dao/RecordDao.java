package com.example.gobang001.dao;

import com.example.gobang001.mode.GobangPlayRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RecordDao {
    //插入记录
    public int insertRecordByGobangPlayRecord(@Param("record") GobangPlayRecord record);

    //查询记录
    public List<GobangPlayRecord> selectRecordByAll();
    public GobangPlayRecord selectRecordByRid(Integer rid);
    public List<GobangPlayRecord> selectRecordByUid1(Integer uid1);
    public List<GobangPlayRecord> selectRecordByUid2(Integer uid2);

    public List<GobangPlayRecord> selectRecordByTwoPlayerUid(Integer uid1, Integer uid2);

    public List<GobangPlayRecord> selectRecordByChangeScore(Integer changeNum);

    public List<GobangPlayRecord> selectRecordByLastScore(Integer lastScore);
    public List<GobangPlayRecord> selectRecordByUpdateTime(String startTime, String endTime);

    public Integer deleteByRid(Integer rid);
    public Integer deleteByUid(Integer uid);
    public Integer updatePlayRecord(@Param("record") GobangPlayRecord record);
}
