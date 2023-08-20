package com.example.gobang001.game;

import com.example.gobang001.mode.MatchResponse;
import com.example.gobang001.mode.Room;
import com.example.gobang001.mode.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.IndexedReadOnlyStringMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 匹配器，控制玩家的匹配
 */
@Component
@Slf4j
public class Matcher {
    //在线玩家会话管理
    @Autowired
    private OnlineUserManager onlineUserManager;
    //房间管理器
    @Autowired
    private RoomManager roomManager;
    //json字符串与java对象的转换
    @Autowired
    private ObjectMapper objectMapper;
    //匹配队列，可以针对不同阶段的天梯分，进行分级匹配，处于同一匹配队列的玩家，可成功匹配，并开始游戏
    //默认队列个数
    private static final Integer N = 12;
    //记录队列个数
    private static int n = N;
    private Queue<User>[] queues;

    //队列线程池
    private Thread[] threads;

    //初始化队列，按照默认值创建匹配队列
    private void init() {
        queues = new Queue[n];
        threads = new Thread[n];
        for (int i = 0; i < n; i++) {
            queues[i] = new LinkedList<>();
        }
    }
    //指定队列个数
    private void init(Integer num) {
        queues = new Queue[num];
        threads = new Thread[n];
        n = num;
        for (int i = 0; i < num; i++) {
            queues[i] = new LinkedList<>();
        }
    }
    public Matcher() {
        init();
        //启用线程，每一个队列需要一个线程进行扫描
        for (int i = 0; i < n; i++) {
            //创建线程
            threads[i] = createThread(queues[i]);
            threads[i].start();
        }
    }

    //创建线程
    private Thread createThread(Queue<User> workerQueue) {
        return new Thread(() -> {
            //扫描对应的队列
            while (true) {
                handerMatch(workerQueue);
            }
        });
    }

    //扫描对应的队列，实现匹配控制
    private void handerMatch(Queue<User> workerQueue) {
        synchronized (workerQueue) {
            try {
                //判断该队列匹配人数是否足够
                while (workerQueue.size() < 2) {
                    workerQueue.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //取出两位玩家
            User player1 = workerQueue.poll();
            User player2 = workerQueue.poll();
            log.info("玩家1：" + player1.getUsername() + "与玩家2：" + player2.getUsername() + "进行匹配！");
            //获取两位玩家的websocket会话信息
            WebSocketSession session1 = onlineUserManager.getWebsocketSessionForHall(player1.getUid());
            WebSocketSession session2 = onlineUserManager.getWebsocketSessionForHall(player2.getUid());
            //获取两位玩家的会话信息的目的是为了通知两位玩家已经匹配成功了，然后进入房间进行游戏

            //多重判断，再次判断玩家的会话是否有效，如果存在无效的会话信息，则此次匹配失败，需将拥有有效会话的玩家重新放入到队列当中
            if (session1 == null && session2 == null) {
                //两位玩家会话均无效，直接返回，无需处理
                log.info("[匹配失败]两位玩家的会话信息均无效，无法继续进行匹配！");
                return;
            }
            if (session1 == null) {
                //会话1为空表示玩家1不存在，将玩家二放回到匹配队列当中
                workerQueue.offer(player2);
                log.info("[匹配失败]玩家1会话信息无效");
                return;
            }
            if (session2 == null) {
                workerQueue.offer(player1);
                log.info("[匹配失败]玩家2会话信息无效");
                return;
            }

            //排除同用户匹配的情况
            if (session1 == session2) {
                //玩家1与玩家2的会话相同，多设备登录同一个账户，无效
                log.info("[匹配失败]同一玩家无法进行匹配");
                return;
            }

            //TODO 将玩家放入同一个房间准备进行游戏
            Room room = new Room();
            //由于进入游戏需要切换页面，在此处玩家正常在线，但是并不代表可以顺利进入房间，所以此处不能将玩家放入房间当中
            //将房间交给房间管理器进行管理
            roomManager.add(room, new User[]{player1, player2});
            //通知两位玩家，告知以及匹配成功，准备进入游戏
            MatchResponse response1 = new MatchResponse();
            response1.setIsOk(true);
            response1.setMessage("matchSuccess");
            response1.setReason("匹配成功，你的对手是" + player2.getUsername());
            MatchResponse response2 = new MatchResponse();
            response2.setIsOk(true);
            response2.setMessage("matchSuccess");
            response2.setReason("匹配成功，你的对手是" + player1.getUsername());

            try {
                //发送websocket响应
                session1.sendMessage(new TextMessage(objectMapper.writeValueAsString(response1)));
                session2.sendMessage(new TextMessage(objectMapper.writeValueAsString(response2)));
            } catch (IOException e) {
                log.info("[匹配模块]返回websocket响应格式转换出现问题");
                e.printStackTrace();
            }
        }

    }

    //指定队列个数 （需要重新实现分数分配，该项目以默认12为标准）
    private Matcher(Integer num) {

    }

    //将玩家放入某个队列
    private Boolean add(Integer qid, User user) {
        if (qid < 0 || qid >= queues.length) return false;
        //针对与某一个队列，在多线程情况下，可能存在同时添加，删除等修改操作，存在线程安全问题需要进行加锁
        //将玩家插入到对应的队列当中
        Queue<User> queue = queues[qid];
        synchronized (queue) {
            queue.offer(user);
            log.info("将玩家" + user.getUsername() + "加入到qid为" + qid + "的匹配队列！");
            //唤醒该线程，使得该线程扫描该队列，因为当有新成员加入后，有机会满员匹配
            queue.notify();
        }
        return true;
    }

    //根据玩家的天梯分， 确定好去哪一个匹配队列当中进行匹配
    private Integer getPlayerQueueId(Integer score) {
        //获取玩家的天梯分
        int idx = -1;
        //以1500为基础，500分以上，以200分为梯度，分为500， 700，900,1100,1300 分界线，占用前五个队列，高于1500，以1800,2000,2200,2400,2600,2800为分界线占用后六个队列
        if (score < 500) idx = 0;
        else if (score < 700) idx = 1;
        else if (score < 900) idx = 2;
        else if (score < 1100) idx = 3;
        else if (score < 1300) idx = 4;
        else if (score < 1800) idx = 5;
        else if (score < 2000) idx = 6;
        else if (score < 2200) idx = 7;
        else if (score < 2400) idx = 8;
        else if (score < 2600) idx = 9;
        else if (score < 2800) idx = 10;
        else idx = 11;
        return idx;
    }

    //将玩家按照天梯分，放入对应的队列当中
    public Boolean add(User user) {
        //验证玩家是否有效
        if (user == null) {
            log.info("传入的玩家不存在");
            return false;
        }
        //获取玩家的天梯分
        int idx = getPlayerQueueId(user.getScore());
        add(idx, user);
        return true;
    }

    //将玩家从队列当中删除
    private Boolean remove(Integer qid, User user) {
        //判断下标是否越界
        if (qid < 0 || qid > n) return false;
        //删除该玩家
        Queue<User> queue = queues[qid];
        //可能存在多个线程删除该队列中的元素，需要进行加锁，保证线程安全
        synchronized (queue) {
            queue.remove(user);
        }
        return true;
    }

    public Boolean remove(User user) {
        //验证参数是否有效
        if (user == null) {
            log.info("[matcher] 移除用户失败，因为传入的参数是无效的！");
            return false;
        }
        //获取用户的天梯分数
        Integer score = user.getScore();
        //获取匹配队列的编号
        int qid = getPlayerQueueId(score);
        //从该队列中删除该用户
        return remove(qid, user);
    }
}
