package com.example.gobang001.mode;

import com.example.gobang001.game.OnlineUserManager;
import com.example.gobang001.game.RoomManager;
import com.example.gobang001.service.RecordService;
import com.example.gobang001.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

@Data
@Slf4j
public class Room {
    //Java对象与json字符串互相转换
    private ObjectMapper objectMapper = new ObjectMapper();
    //房间号，唯一身份标识, 使用UUID生成
    private String rid;
    //对战玩家
    private User[] players;
    //房间玩家人数
    private static final int N = 2;
    //棋盘大小
    private static int ROW = 32;
    private static int COL = 18;
    //棋盘
    private int[][] board;
    //先手uid
    private Integer priorityUid;

    //初始化房间
    private void init() {
        players = new User[N];

        //使用UUID分配id
        this.rid = UUID.randomUUID().toString();
    }
    //初始化棋盘，当棋盘为空时，按照ROW与COL的大小创建棋盘
    private void initBoard() {
        if (board == null) board = new int[ROW][COL];
    }

    //构造Room对象
    public Room() {
        init();
    }

    //添加玩家
    public void add(int idx, User user) {
        //判断插入对象是否合法
        if (user == null) {
            log.info("[添加玩家到房间异常] 玩家参数不合法，如传入的玩家对象为空！");
            return;
        }
        //判断下标是否合法
        if (idx < 0 || idx >= N) {
            log.info("[添加玩家到房间异常] 添加玩家:" + user.getUsername() + "时，房间数组下标越界！");
            return;
        }
        //判断当前位置是否存在玩家，如果存在则覆盖，并发出通知
        if (players[idx] != null) {
            log.info("[添加玩家到房间提示] 玩家位" + idx + "不为空, 当前玩家为："
                    + players[idx].getUsername() + "已被新玩家：" + user.getUsername() + "覆盖！");
        }
        //插入玩家
        players[idx] = user;
    }

    //清空玩家
    public void clear() {
        for (int i = 0; i < N; i++) players[i] = null;
    }
    //删除某一个位置的玩家
    public User remove(Integer idx) {
        User user = players[idx];
        players[idx] = null;
        return user;
    }

    //按照玩家的操作进行落子
    public void putChess(GameRequest request, OnlineUserManager onlineUserManager, RoomManager roomManager, UserService userService, RecordService recordService) {
        //开始处理落子请求
        log.info("[websocket请求与响应：游戏对战阶段] 落子请求处理！");
        //初始化后端棋盘
        //获取棋盘大小
        ROW = request.getMaxRow();
        COL = request.getMaxCol();
        log.info("[websocket请求与响应：游戏对战阶段] 初始化棋盘大小为：" + ROW + "X" + COL);
        //创建棋盘
        initBoard();

        //判断时玩家1落的子还是玩家2落的子
        int chessPlayerIndex = Objects.equals(request.getUid(), players[0].getUid()) ? 1 : 2;

        //获取下棋的位置
        int row = request.getRow();
        int col = request.getCol();

        //判断该位置是否有子
        if (board[row][col] != 0) {
            //该位置已经落子，不能够再进行落子操作
            log.info("[websocket请求与响应：游戏对战阶段] 落子请求处理时，落子请求指定的位置已经落子，无法继续落子！");
            return;
        }

        //落子
        board[row][col] = chessPlayerIndex;
        //输出棋盘局势
        printChessBoard();
        //判断落子后是分出胜负，如分出胜负，返回胜利者玩家对象，否则返回null
        User winner = checkWinner(row, col, chessPlayerIndex);

        //构造响应请求
        GameResponse response = new GameResponse();
        response.setMessage("putChess");
        response.setUid(players[chessPlayerIndex - 1].getUid());
        response.setIsOk(true);
        response.setRow(row);
        response.setCol(col);
        response.setWinner(winner == null ? 0 : winner.getUid());
        response.setWinnerName(winner == null ? "" : winner.getUsername());

        //将结果发送给房间中的每一位玩家
        //获取房间中玩家的会话, 需要通过房间在线管理器进行获取，但是Room属于实体类，存在多个，且我们自己管理Room类，因此不能通过自动注入房间在线管理器
        //方式1：在Room构造时，手动获取
        //方式2：通过传参获取
        //采用方式2获取
        WebSocketSession session1 = onlineUserManager.getWebSocketSessionForRoom(players[0].getUid());
        WebSocketSession session2 = onlineUserManager.getWebSocketSessionForRoom(players[1].getUid());

        //判断玩家是否断线
        //情况1：两位玩家同时断线
        if (session1 == null && session2 == null) {
            //两位玩家同时断线
            log.info("[websocket请求与响应：游戏对战阶段] 两位玩家同时断线，做平手处理！");
            User user = new User();
            user.setUsername("平手");
            user.setUid(-1);
            response.setUid(-1);
            response.setReason("两位玩家打成平手!");
            response.setWinner(user.getUid());
            response.setWinnerName(user.getUsername());
            winner = user;
        } else if (session1 == null) {
            //玩家1下线，判定玩家2获胜
            log.info("[websocket请求与响应：游戏对战阶段] 玩家1：" + players[0].getUsername() +"对战过程当中掉线，判定玩家2获胜！");
            response.setWinner(players[1].getUid());
            response.setWinnerName(players[1].getUsername());
        } else if (session2 == null) {
            //玩家2下线，判定玩家1获胜
            log.info("[websocket请求与响应：游戏对战阶段] 玩家2：" + players[1].getUsername() + "对战过程当中掉线，判定玩家1获胜！");
            response.setWinner(players[0].getUid());
            response.setWinnerName(players[0].getUsername());
        }

        //将响应广播至所有的玩家，如果有观众，也需要一并返回
        //返回给玩家
        for (int i = 0; i < players.length; i++) {
            WebSocketSession session = onlineUserManager.getWebSocketSessionForRoom(players[i].getUid());
            if (session != null) {
                try {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                } catch (IOException e) {
                    log.info("[websocket请求与响应：游戏对战阶段] 响应落子结果时，发生格式转换错误！");
                }

            }
        }
        //返回给观众（保留），还没有实现观战功能

        //如果胜负已经分晓，需要销毁房间
        if (response.getWinner() != 0) {
            //结算分数
            if (winner != null && winner.getUid() != -1) {
                //有一方获胜
                //获取胜者的uid
                Integer winnerUid = winner.getUid();
                //获取当前胜利者最新的战绩信息
                winner = userService.findUserByUsername(winner.getUsername());
                if (winner == null) {
                    //用户被非法删除或注销
                    log.info("[websocket请求与响应：游戏对战阶段] 查询胜利者最新战绩时，获取结果不存在！！");
                    //停止更新，销毁房间
                    Room room = roomManager.getRoomByUid(players[0].getUid());
                    roomManager.remove(room);
                    log.info("[websocket请求与响应：游戏对战阶段] 房间：" + room.getRid() + "已经销毁！");
                    return;
                }
                //获取失败方对象
                User loser = Objects.equals(players[0].getUid(), winnerUid) ? players[1] : players[0];
                //获取失败方最新的战绩
                loser = userService.findUserByUsername(loser.getUsername());
                //按照规则更新玩家的分数，计算结算后胜利者的分数
                handlerScore(winner, loser, userService, recordService);
                //战绩记录变换表更新（备用）
            } else {
                //两方打成平手
                //更新战绩
                equalHandlerScore(players[0], players[1], userService, recordService);
                log.info("[websocket请求与响应：游戏对战阶段] 两方打平，成绩已结算！");
            }
            //销毁房间
            Room room = roomManager.getRoomByUid(players[0].getUid());
            roomManager.remove(room);
            log.info("[websocket请求与响应：游戏对战阶段] 房间：" + room.getRid() + "已经销毁！");
        }
    }

    //判断棋盘下标是否越界
    private Boolean isOverIndex(Integer row, Integer col) {
        if (row >= board.length || row < 0) return true;
        if (col >= board[0].length || col < 0) return true;
        return false;
    }

    private User checkWinner(Integer row, Integer col, Integer chessPlayerIndex) {
        //胜负判断
        //判断参数是否合法
        if (isOverIndex(row, col) || chessPlayerIndex > 2 || chessPlayerIndex <= 0) return null;
        //与落子位相同数的最大连续个数
        int cnt = 0;
        //判断行是否存在五子
        int idx = Math.max(col - 4, 0);
        while (idx <= col + 4 && idx < COL) {
            if (board[row][idx] == chessPlayerIndex) cnt++;
            else cnt = 0;

            if (cnt == 5) {
                //表示存在五个相同类型的棋子连珠
                return players[chessPlayerIndex - 1];
            }
            idx++;
        }
        //判断列是否存在五子
        idx = Math.max(row - 4, 0);
        cnt = 0;
        while (idx <= row + 4 && idx < ROW) {
            if (board[idx][col] == chessPlayerIndex) cnt++;
            else cnt = 0;
            System.out.println("idx" + idx);
            if (cnt == 5) {
                return players[chessPlayerIndex - 1];
            }
            idx++;
        }

        //判断左对角线是否存在五子
        int i = row - 4;
        int j = col - 4;
        cnt = 0;
        while (i <= row + 4 && i < ROW && j <= col + 4 && j < COL) {
            if (isOverIndex(i, j)) {
                i++;
                j++;
                continue;
            }
            if (board[i][j] == chessPlayerIndex) cnt++;
            else cnt = 0;
            if (cnt == 5) {
                return players[chessPlayerIndex - 1];
            }
            i++;
            j++;
        }
        //判断右对角线是否存在五子
        i = row - 4;
        j = col + 4;
        cnt = 0;
        while (i <= row + 4 && i < ROW && j >= col - 4 && j >= 0) {
            if (isOverIndex(i, j)) {
                i++;
                j--;
                continue;
            }
            if (board[i][j] == chessPlayerIndex) cnt++;
            else cnt = 0;
            if (cnt == 5) {
                return players[chessPlayerIndex - 1];
            }
            i++;
            j--;
        }
        //四种情况均不存在五子棋连珠，未分胜负，返回null
        return null;
    }
    /**
     * 分数结算规则如下：
     * 玩家初始积分：1500分
     * 获胜方：
     *    基础加分：32分，附加分：与对手天梯分绝对值差值的16%，如果对手天梯分更高，比例提升至32%。
     * 失败方：
     *    基础减分：32分，附减分：如果比对手天梯分更高，减去对手低于你的分值的16%，反之如果对方分数更高，加上对手高于你分数分值部分的16%
     * 说明：
     *     如果计算的分数为小数，仅取整数部分。
     */
    //正常规则结算
    public void handlerScore(User winner, User loser, UserService userService, RecordService recordService) {
        int winnerScore = winner.getScore();
        int loserScore = loser.getScore();
        int originWinnerScore = winnerScore;
        int originLoserScore = loserScore;
        if (loser.getScore() > winner.getScore()) winnerScore += Math.abs(loserScore - winnerScore) * 0.32;
        else winnerScore += Math.abs(loserScore - winnerScore) * 0.16;

        winnerScore += 32;
        //计算失败方结算后的分数
        if (loser.getScore() > winner.getScore()) loserScore -= Math.abs(winner.getScore() - loser.getScore()) * 0.16;
        else loserScore += Math.abs(winner.getScore() - loser.getScore()) * 0.16;

        loserScore -= 32;

        //更新数据库中的战绩数据
        userService.updateWinnerScore(winner, winnerScore);
        userService.updateLoserScore(loser, loserScore);

        log.info("[websocket请求与响应：游戏对战阶段] 获胜方是：" + winner.getUsername() + "结算后分数为：" + winnerScore + "增加了" + (winnerScore - originWinnerScore) + "分");
        log.info("[websocket请求与响应：游戏对战阶段] 失败方是：" + loser.getUsername() + "结算后分数为：" + loserScore + "增加了" + (loserScore - originLoserScore) + "分");
        //记录对局数据
        GobangPlayRecord record1 = new GobangPlayRecord();
        GobangPlayRecord record2 = new GobangPlayRecord();

        Integer changeNum1 = winnerScore - originWinnerScore;
        Integer changeNum2 = loserScore - originLoserScore;
        log.info("[websocket请求与响应：游戏对战阶段] 玩家1：" + winner.getUsername() + "，开始记录对战记录！");
        record1.setUid1(winner.getUid());
        record1.setUid2(loser.getUid());
        record1.setEnemy(loser.getUsername());
        record1.setOldScore(originWinnerScore);
        record1.setChangeNum(changeNum1);
        record1.setTotalCount(winner.getTotalCount() + 1);
        record1.setWinnerCount(winner.getWinCount() + 1);
        record1.setWinnerRate(100.00 * record1.getWinnerCount() / record1.getTotalCount());
        record1.setIsWinner(true);
        record1.setLastScore(winnerScore);
        log.info("[websocket请求与响应：游戏对战阶段] 玩家1：" + winner.getUsername() + "，完成记录对战记录！");
        log.info("[websocket请求与响应：游戏对战阶段] 玩家2：" + loser.getUsername() + "，开始记录对战记录！");
        record2.setUid1(loser.getUid());
        record2.setUid2(winner.getUid());
        record2.setEnemy(winner.getUsername());
        record2.setOldScore(originLoserScore);
        record2.setChangeNum(changeNum2);
        record2.setTotalCount(loser.getTotalCount() + 1);
        record2.setWinnerCount(loser.getWinCount());
        record2.setWinnerRate(100.00 * record2.getWinnerCount() / record2.getTotalCount());
        record2.setIsWinner(false);
        record2.setLastScore(loserScore);
        log.info("[websocket请求与响应：游戏对战阶段] 玩家2：" + loser.getUsername() + "，完成记录对战记录！");
        recordService.savePlayRecords(record1, record2);
        log.info("[websocket请求与响应：游戏对战阶段] 游戏结算成功！");
    }

    //平局游戏分数结算
    public void equalHandlerScore(User thisPlayer, User thatPlayer, UserService userService, RecordService recordService) {
        if (thisPlayer == null || thatPlayer == null) {
            log.info("[websocket 游戏对战阶段] 无效的选手！");
            return;
        }
        userService.updateDrawScore(thisPlayer);
        userService.updateDrawScore(thatPlayer);
        //记录对局数据
        GobangPlayRecord record1 = new GobangPlayRecord();
        GobangPlayRecord record2 = new GobangPlayRecord();

        record1.setUid1(thisPlayer.getUid());
        record1.setUid2(thatPlayer.getUid());
        record1.setEnemy(thatPlayer.getUsername());
        record1.setOldScore(thisPlayer.getScore());
        record1.setWinnerCount(thisPlayer.getWinCount());
        record1.setTotalCount(thisPlayer.getTotalCount() + 1);
        record1.setChangeNum(0);
        record1.setWinnerRate(100.00 * record1.getWinnerCount() / record1.getTotalCount());
        record1.setIsWinner(false);
        record1.setLastScore(thisPlayer.getScore());

        record2.setUid1(thatPlayer.getUid());
        record2.setUid2(thisPlayer.getUid());
        record2.setEnemy(thisPlayer.getUsername());
        record2.setOldScore(thatPlayer.getScore());
        record2.setWinnerCount(thatPlayer.getWinCount());
        record2.setTotalCount(thatPlayer.getTotalCount() + 1);
        record2.setChangeNum(0);
        record1.setWinnerRate(100.00 * record1.getWinnerCount() / record1.getTotalCount());
        record2.setIsWinner(false);
        record2.setLastScore(thatPlayer.getScore());

        recordService.savePlayRecords(record1, record2);
    }
    //打印棋盘，便于调试
    public void printChessBoard() {
        if (board == null) {
            //棋盘还未初始化
            System.out.println("棋盘还未初始化!");
            return;
        }
        System.out.println(this.rid + "棋盘信息如下：");

        int n = board.length;
        int m = board[0].length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                System.out.print(board[i][j] + "  ");
            }
            System.out.println();
        }
    }

}
