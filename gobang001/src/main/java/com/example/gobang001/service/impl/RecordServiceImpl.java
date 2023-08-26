package com.example.gobang001.service.impl;

import com.example.gobang001.dao.PlayerRecordDao;
import com.example.gobang001.dao.RecordDao;
import com.example.gobang001.mode.GobangPlayRecord;
import com.example.gobang001.mode.PlayerRecord;
import com.example.gobang001.mode.User;
import com.example.gobang001.service.RecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
@Service
@Slf4j
public class RecordServiceImpl implements RecordService {
    //游戏对战记录数据层对象
    @Resource
    private RecordDao recordDao;
    //玩家对战记录数据层对象
    @Resource
    private PlayerRecordDao playerRecordDao;
    @Override
    public List<GobangPlayRecord> getAllPlayRecord() {
        //查询全站所有的游戏对战记录
        return recordDao.selectRecordByAll();
    }

    @Override
    public List<GobangPlayRecord> getPlayRecordBySelf(Integer uid) {
        //检查参数
        if (uid == null || uid <= 0) return null;
        return recordDao.selectRecordByUid1(uid);
    }

    @Override
    public List<GobangPlayRecord> getPlayRecordByEnemy(Integer uid) {
        if (uid == null || uid <= 0) return null;
        return recordDao.selectRecordByUid2(uid);
    }

    @Override
    public List<GobangPlayRecord> getPlayRecordByTwoPlayer(Integer uid1, Integer uid2) {
        if (uid1 == null || uid2 == null || uid1 <= 0 || uid2 <= 0) return null;
        return recordDao.selectRecordByTwoPlayerUid(uid1, uid2);
    }

    @Override
    public List<GobangPlayRecord> getPlayRecordByUpdateTime(String startTime, String endTime) {
        if (startTime == null || endTime == null || "".equals(startTime) || "".equals(endTime)) return null;
        return recordDao.selectRecordByUpdateTime(startTime, endTime);
    }

    @Override
    public List<GobangPlayRecord> getPlayRecordByChangeNum(Integer changeNum) {
        if (changeNum == null) return null;
        return recordDao.selectRecordByChangeScore(changeNum);
    }

    @Override
    public List<GobangPlayRecord> getPlayRecordByLastScore(Integer score) {
        if (score == null) return null;
        return recordDao.selectRecordByLastScore(score);
    }

    @Override
    public Boolean addPlayRecord(GobangPlayRecord record) {
        if (record == null) return false;
        return recordDao.insertRecordByGobangPlayRecord(record) != 0;
    }

    @Override
    public Boolean savePlayRecords(GobangPlayRecord record1, GobangPlayRecord record2) {
        if (record1 == null || record2 == null) {
            log.info("[记录服务模块] 两方记录数据存在空，缺少数据，无法记录！");
            return false;
        }
        addPlayRecord(record1);
        addPlayRecord(record2);
        return true;
    }

    @Override
        public Integer removePlayRecordByRid(Integer rid) {
        if (rid == null || rid <= 0) return 0;
        return recordDao.deleteByRid(rid);
    }

    @Override
    public Integer removePlayRecordByUid(Integer uid) {
        if (uid == null || uid <= 0) return 0;
        return recordDao.deleteByUid(uid);
    }

    @Override
    public Boolean updatePlayRecord(GobangPlayRecord record) {
        if (record == null || record.getRid() == null || record.getRid() <= 0) return null;
        return recordDao.updatePlayRecord(record) != 0;
    }

    @Override
    public List<PlayerRecord> getPlayerRecord(Integer uid) {
        if (uid == null || uid <= 0) return null;
        return playerRecordDao.selectPlayerRecordByUid(uid);
    }
}
