package com.example.gobang001.service;

import com.example.gobang001.mode.GobangPlayRecord;
import com.example.gobang001.mode.PlayerRecord;
import com.example.gobang001.mode.User;

import java.util.List;

public interface RecordService {
    //查询全站所有的对战记录
    public List<GobangPlayRecord> getAllPlayRecord();

    //根据玩家1的uid查询
    public List<GobangPlayRecord> getPlayRecordBySelf(Integer uid);
    //根据玩家2的uid查询
    public List<GobangPlayRecord> getPlayRecordByEnemy(Integer uid);
    //根据玩家2与玩家2的uid查询
    public List<GobangPlayRecord> getPlayRecordByTwoPlayer(Integer uid1, Integer uid2);
    //根据日期范围查询
    public List<GobangPlayRecord> getPlayRecordByUpdateTime(String startTime, String endTime);
    //根据分数变化范围查询
    public List<GobangPlayRecord> getPlayRecordByChangeNum(Integer changeNum);
    //根据结算后分数进行查询
    public List<GobangPlayRecord> getPlayRecordByLastScore(Integer score);


    //插入
    public Boolean addPlayRecord(GobangPlayRecord record);
    //更新俩位玩家结算时的分数
    public Boolean savePlayRecords(GobangPlayRecord record1, GobangPlayRecord record2);

    //删除

    //通过rid进行删除
    public Integer removePlayRecordByRid(Integer rid);
    //通过玩家uid批量删除
    public Integer removePlayRecordByUid(Integer uid);

    //修改 -- 可能用不到， 因为对于客户，没有权限修改，不过管理员需要改接口
    public Boolean updatePlayRecord(GobangPlayRecord record);

    //查询有关登录玩家的对战记录
    public List<PlayerRecord> getPlayerRecord(Integer uid);
}
