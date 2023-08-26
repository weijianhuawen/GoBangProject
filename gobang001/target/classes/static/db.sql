create database if not exists java_gobang;
use java_gobang;

drop table if exists user;
create table user (
    -- id
    uid int primary key auto_increment,
    -- 用户名
    username varchar(50) unique ,
    -- 邮箱
    email varchar(128) unique,
    -- 手机号
    numbers varchar(20) unique,
    -- 密码
    password varchar(50),
    -- 战绩
    score int,
    -- 对战场数
    totalCount int,
    -- 获胜场数
    winCount int,
    -- 胜率
    winRate double(8, 2)
);

insert into user values(null, 'zhangsan', '', '', '122333', 111, 22, 2, 0.00);
insert into user values(null, 'lisi',  '111', '111', '1223', 11, 2, 0, 0.00);
insert into user values(null, 'wangwu',  '222', '222', '122333', 1122, 222,92, 0.00);
insert into user values(null, 'wwww',  '333', '333', '122333', 1211, 2222, 542, 0.00);
insert into user values(null, 'chatGPT',  '333', '333', '122333', 1500, 0, 0, 0.00);

-- 数据记录表
drop table  if exists play_record;
create table play_record (
    -- rid 记录表编号
    rid int primary key auto_increment,
    -- 玩家id
    uid1 int not null,
    uid2 int not null,
    -- 对手昵称
--     enemy varchar(50) not null default '匿名玩家',
    -- 分数变化
    change_num int default 0,
    -- 是否获胜
    is_winner int default 1,
    foreign key (uid1) references user(uid), foreign key (uid2) references user(uid)
);

insert into play_record values (null, 6, '123', -36, 0);
insert into play_record(uid, enemy, change_num, is_winner) values (7, 'heizi', 32, 1);

drop table  if exists play_record;
create table play_record (
    -- rid 记录表编号
                             rid int primary key auto_increment,
    -- 玩家id
                             uid1 int not null ,
    -- 对手
                             uid2 int not null,
    -- 对手昵称
                             enemy varchar(50) default '未知玩家',
    -- 分数变化
                             change_num int default 0,
    -- 当前结算后的分数
                             cur_score int default 0,
    -- 结算前的分数
                             pre_score int default 0,
    -- 当前结算后胜场
                             cur_winner_count int default 0,
    -- 当前结算后的总胜场
                             cur_total_count int default 0,
    -- 当前结算后的胜率
                             cur_winner_rate double(8, 2) default 0,
    -- 是否获胜
                             is_winner int default 1,
    -- 结算时间
                             update_time datetime default now(),
                             foreign key (uid1) references user(uid), foreign key (uid2) references user(uid)
);

insert into play_record values (null, 6, 7, '123', -36, 1464, 1500, 0, now());
insert into play_record(uid1,uid2, enemy, change_num, cur_score, pre_score, is_winner) values (7, 6, 'heizi', 32,1532, 1500, 1);