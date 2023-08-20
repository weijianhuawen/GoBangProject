let gameInfo = {
    rid: null,
    thisUserId: null,
    thisUserName: null,
    thatUserId: null,
    thatUserName: null,
    isWhite: true
}

// 设定界面显示相关操作
function setScreenText(me) {
    var contentStyle = document.getElementById('content');
    var contentDiv = document.querySelector('#screen .content');
    //提示玩家所拿到的棋子是白子还是黑子
    var chessColor = gameInfo.isWhite ? "【玩家[" + gameInfo.thisUserName + "](我方) 棋子颜色为<strong>白子</strong>】<br>"
        : "【玩家[" + gameInfo.thisUserName + "](我方) 棋子颜色为<strong>黑子</strong>】<br>";
    if (me) {
        contentDiv.innerHTML = chessColor + "轮到我方落子了!";
        document.getElementById('content').style.color = "rgba(57, 127, 17, 0.88)"
    } else {
        contentDiv.innerHTML = chessColor + "轮到对方落子了!";
        document.getElementById('content').style.color = "rgba(202, 80, 66, 0.88)";
    }
}

// 初始化 websocket
var websocketUrl = "ws://" + location.host + "/game";
var websocket = new WebSocket(websocketUrl);
//建立连接
websocket.onopen = function() {
    console.log("游戏房间连接成功!");
}

//断开连接
websocket.onclose = function() {
    console.log("游戏连接断开");
}

//异常断开
websocket.onerror = function() {
    console.log("游戏房间连接出现异常");
}

//处理服务器的响应数据
websocket.onmessage = function(event) {
    console.log("开始处理响应");
    console.log("响应数据：" + event.data);
    var jresp = JSON.stringify(event.data);
    var resp = JSON.parse(event.data);
    //检查游戏连接
    if (!resp.isOk) {
        console.log("游戏连接异常");
        //重定向到游戏大厅
        location.assign("/games_hall.html")
    }
    //响应类型不匹配
    console.log(jresp);
    var message = resp.message;
    console.log(message);
    if (message === "gameReady") {
        gameInfo.rid = resp.rid;
        gameInfo.thisUserId = resp.thisPlayerUid;
        gameInfo.thatUserId = resp.thatPlayerUid;
        gameInfo.isWhite = resp.white;
        gameInfo.thisUserName = resp.thisPlayerName;
        gameInfo.thatUserName = resp.thatPlayerName;
        //初始化导航栏信息
        var usernameNav = document.querySelector('#username');
        usernameNav.innerHTML = gameInfo.thisUserName;
        //初始化棋盘
        initGame();
        //设置区域显示内容
        setScreenText(gameInfo.isWhite);
    } else if (message === "repeatConnection") {
        //检测到多处进入游戏房间，回到登录页面
        alert("检测到游戏异常（多处登录或进入游戏房间），请重新登录!");
        location.assign("/login.html");
    } else {
        console.log("响应类型不匹配");
    }
}

//     } else if (resp.message == 'repeatConnection') {
//         alert("检测到游戏多开! 请使用其他账号登录!");
//         location.assign("/login.html");
//     }
// }


// 初始化一局游戏
var n = 15;
var m = 450;
// initGame();
function initGame() {
    // 是我下还是对方下. 根据服务器分配的先后手情况决定
    let me = gameInfo.isWhite;
    // 游戏是否结束
    let over = false;
    let chessBoard = [];
    //初始化chessBord数组(表示棋盘的数组)
    for (let i = 0; i < 18; i++) {
        chessBoard[i] = [];
        for (let j = 0; j < 32; j++) {
            chessBoard[i][j] = 0;
        }
    }
    let chess = document.querySelector('#board');
    let context = chess.getContext('2d');
    context.strokeStyle = "rgba(80, 140, 229, 0.88)";
    //设置canvas格式
    context.textAlig = "center";
    context.textBaseline = "middle";
    // 背景图片
    // let logo = new Image();
    // logo.src = "images/chess_board.jpg";
    // context.onload = function () {
    //     console.log("初始化网格");
    //     context.drawImage(logo, 0, 0, 450, 450);
    //     initChessBoard();
    // }
    initChessBoard();

    // 绘制棋盘网格
    function initChessBoard() {
        //15*15
        // for (let i = 0; i < 16; i++) {
        //     context.moveTo(15 + i * 30, 15);
        //     context.lineTo(15 + i * 30, 435);
        //     context.stroke();
        //     context.moveTo(15, 15 + i * 30);
        //     context.lineTo(435, 15 + i * 30);
        //     context.stroke();

        //     // context.moveTo(i * 30, 0);
        //     // context.lineTo(i * 30, 450);
        //     // context.stroke();
        //     // context.moveTo(0, i * 30);
        //     // context.lineTo(450, i * 30);
        //     // context.stroke();
        // }
        //32*18
        
        for (let i = 0; i < 33; i++) {
            //画33根竖线, 注意x是横坐标
            context.moveTo(15 + i * 30, 15);
            context.lineTo(15 + i * 30, 525);
            context.stroke();

            //画19根横线
            if (i < 19) {
                context.moveTo(15, 15 + i * 30);
                context.lineTo(945, 15 + i * 30);
                context.stroke();
            }
            // context.moveTo(i * 30, 0);
            // context.lineTo(i * 30, 450);
            // context.stroke();
            // context.moveTo(0, i * 30);
            // context.lineTo(450, i * 30);
            // context.stroke();
        }
    }

    // 绘制一个棋子, me 为 true
    function oneStep(i, j, isWhite) {
        context.beginPath();
        context.arc(15 + i * 30, 15 + j * 30, 13, 0, 2 * Math.PI);
        context.closePath();
        var gradient = context.createRadialGradient(15 + i * 30 + 2, 15 + j * 30 - 2, 13, 15 + i * 30 + 2, 15 + j * 30 - 2, 0);
        if (!isWhite) {
            gradient.addColorStop(0, "#0A0A0A");
            gradient.addColorStop(1, "#636766");
        } else {
            gradient.addColorStop(0, "#D1D1D1");
            gradient.addColorStop(1, "#F9F9F9");
        }
        context.fillStyle = gradient;
        context.fill();
    }

    chess.onclick = function (e) {
        if (over) {
            return;
        }
        if (!me) {
            return;
        }
        let x = e.offsetX;
        let y = e.offsetY;
        // 注意, 横坐标是列, 纵坐标是行
        let col = Math.floor(x / 30);
        let row = Math.floor(y / 30);
        if (chessBoard[row][col] === 0) {
            // 发送坐标给服务器, 服务器要返回结果
            send(row, col, 18, 32);
            // 留到浏览器收到落子响应的时候再处理(收到响应再来画棋子)
            // oneStep(col, row, gameInfo.isWhite);
            // chessBoard[row][col] = 1;
        }
    }

    function send(row, col, maxRow, maxCol) {
        var req = {
            message: 'putChess',
            uid: gameInfo.thisUserId,
            row: row,
            col: col,
            //棋盘大小告知服务器
            maxRow: maxRow,
            maxCol: maxCol
        };

        websocket.send(JSON.stringify(req));
    }


    // 之前 websocket.onmessage 主要是用来处理了游戏就绪响应. 在游戏就绪之后, 初始化完毕之后, 也就不再有这个游戏就绪响应了.
    // 就在这个 initGame 内部, 修改 websocket.onmessage 方法~~, 让这个方法里面针对落子响应进行处理!

    websocket.onmessage = function(event) {
        console.log("[handerPutChess]开始解析落子请求:" + event.data);

        var resp = JSON.parse(event.data);
        console.log(resp);
        //判断响应类型
        var message = resp.message;
        if (message !== 'putChess') {
            console.log("响应参数类型不匹配!");
            return;
        } 

        //处理落子请求
        var row = resp.row;
        var col = resp.col;
        console.log(gameInfo);
        //注意对应canvas中的坐标 row -> y col -> x
        if (resp.uid === gameInfo.thisUserId) {
            //自己下的棋
            oneStep(col, row, gameInfo.isWhite);
        } else if (resp.uid === gameInfo.thatUserId) {
            //对手下的棋
            oneStep(col, row, !gameInfo.isWhite);
        } else {
            //响应格式错误
            console.log("[handerPutChess]: 响应格式错误!");
            return;
        }

        //标记下棋位置
        if (row >= 0 && col >= 0) chessBoard[row][col] = 1;

        //交换双方的下棋轮次
        me = !me;
        setScreenText(me);

        //判定游戏是否结束
        var screenDiv = document.querySelector('#screen .content');
    
        console.log("[handerPutChess]" + resp.winner);
        var winner = resp.winner;
        if (winner !== 0) {
            //表示胜负已经出分晓
            if (winner === gameInfo.thisUserId) {
                //我方获胜
                document.getElementById('content').style.color = "rgba(57, 127, 17, 0.88)"
                screenDiv.innerHTML = "恭喜你!你赢了!玩家: " + resp.winnerName + "获胜！";
            } else if (winner === gameInfo.thatUserId) {
                //敌方获胜
                document.getElementById('content').style.color = "rgba(202, 80, 66, 0.88)";
                screenDiv.innerHTML =  "很遗憾!你输了!玩家: " + resp.winnerName + "获胜！";
            } else if (winner === -1) {
                //棋盘占满，记为平局
                document.getElementById('content').style.color = "rgba(80, 150, 244, 0.88)"
                screenDiv.innerHTML = "天呐！你们把棋盘都下满了！那就算你们平局吧！";
            } else {
                //字段格式错误
                console.log("[handerPutChess] winner字段格式不匹配!" + winner);
                alert("winner字段格式不匹配! + winner");
            }

            //回到游戏大厅
            //新增一个按钮，玩家点击后回到游戏大厅
            var backButton = document.createElement('back');
            backButton.innerHTML = "返回大厅";
            backButton.className = "back";
            //添加点击事件
            backButton.onclick = function() {
                location.replace("/games_hall.html");
            }
            var fatherDiv = document.querySelector('.container .left');
            fatherDiv.appendChild(backButton);
        }
    }
}
