package com.example.gobang001;

import com.example.gobang001.dao.PlayerRecordDao;
import com.example.gobang001.dao.RecordDao;
import com.example.gobang001.mode.GobangPlayRecord;
import com.example.gobang001.mode.PlayerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class Gobang001ApplicationTests {
    @Autowired
    private RecordDao recordDao;
    @Autowired
    private PlayerRecordDao playerRecordDao;

    @Test
    void contextLoads() {
        GobangPlayRecord record = new GobangPlayRecord();
        record.setUid1(1);
        record.setUid2(3);
        record.setIsWinner(true);
        record.setEnemy("wangwu");
        record.setLastScore(1713);
        record.setOldScore(1693);
        record.setChangeNum(20);
        int ret = recordDao.insertRecordByGobangPlayRecord(record);
        System.out.println(ret);
    }

    @Test
    public void testSelectAll() {
        List<GobangPlayRecord> records = recordDao.selectRecordByAll();
        System.out.println(records);
        GobangPlayRecord record;
        record = recordDao.selectRecordByRid(1);
//        System.out.println(record);
//        records = recordDao.selectRecordByUid1(9);
//        System.out.println(records);
//        records = recordDao.selectRecordByUid2(7);
//        System.out.println(records);
//        records = recordDao.selectRecordByChangeScore(-36);
//        System.out.println(records);
//        records = recordDao.selectRecordByTwoPlayerUid(6, 7);
//        System.out.println(records);
//        record.setChangeNum(334999);
//        record.setUid1(3);
        record.setWinnerCount(10);
        record.setTotalCount(849);
        recordDao.insertRecordByGobangPlayRecord(record);
//        recordDao.updatePlayRecord(record);
//        recordDao.deleteByRid(4);
//        recordDao.deleteByUid(3);
//        records = recordDao.selectRecordByAll();
//
//        System.out.println(records);

//        System.out.println(recordDao.selectRecordByUpdateTime("2023-08-04 11:31:20", "2023-08-04 11:31:20"));
    }

    @Test
    public void testSelectByUid() {
        List<PlayerRecord> playerRecords = playerRecordDao.selectPlayerRecordByUid(1);
        System.out.println(playerRecords);


    }

}
