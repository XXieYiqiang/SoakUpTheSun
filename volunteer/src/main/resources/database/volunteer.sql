create table suts.t_volunteer_prizes
(
    id               bigint auto_increment comment '主键ID'
        primary key,
    name             varchar(255)                       not null comment '奖品名称',
    valid_start_time datetime                           null comment '有效期开始时间',
    valid_end_time   datetime                           null comment '有效期结束时间',
    stock            int      default 0                 null comment '库存',
    status           tinyint  default 0                 null comment '状态 0：未发放 1：发放中  2：已完成',
    proportion       int(4)                             null comment '比例，前？%',
    create_time      datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time      datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '修改时间',
    del_flag         tinyint  default 0                 null comment '删除标识 0：未删除 1：已删除'
)
    comment '志愿者奖品表' charset = utf8mb4;

create table suts.t_volunteer_prizes_grab
(
    id             bigint            not null comment '主键ID'
        primary key,
    volunteer_id   bigint            not null comment '志愿者id',
    prizes_id      bigint            null comment '奖品id',
    cdk            varchar(32)       null comment '奖品领取码',
    receive_count  bigint(9)         null comment '领取次数',
    valid_end_time datetime          null comment '使用截至时间',
    create_time    datetime          null comment '创建时间',
    update_time    datetime          null comment '修改时间',
    del_flag       tinyint default 0 null comment '删除标识 0：未删除 1：已删除'
);

create table suts.t_volunteer_prizes_send_fail_log
(
    id          bigint       not null comment '主键ID'
        primary key,
    prizes_id   bigint       null comment '奖品id',
    json_object varchar(200) null comment 'JSON字符串，存储失败原因、Excel行数等信息',
    create_time datetime     null comment '创建时间',
    update_time datetime     null comment '修改时间',
    del_flag    tinyint      null comment '删除标识 0：未删除 1：已删除'
);

create table suts.t_volunteer_prizes_send_log
(
    id           bigint      not null comment '主键ID'
        primary key,
    volunteer_id bigint      not null comment '志愿者id',
    prizes_id    bigint      not null comment '奖品id',
    cdk          varchar(32) not null comment '奖品领取码',
    create_time  datetime    null comment '创建时间',
    update_time  datetime    null comment '修改时间',
    del_flag     tinyint     null comment '删除标识 0：未删除 1：已删除',
    constraint CDK
        unique (cdk)
);

create table suts.t_volunteer_rating
(
    id            bigint auto_increment comment 'id'
        primary key,
    user_id       bigint     not null comment '用户id',
    volunteer_id  bigint     null comment '志愿者id',
    rating        tinyint(1) null comment '附加分(0无 1有)',
    is_calculated tinyint    null comment '是否已经新增 0/1 否/是',
    create_time   datetime   null comment '创建时间',
    update_time   datetime   null comment '修改时间',
    del_flag      tinyint    null comment '逻辑删除 0/1 存在/删除'
);

create index idx_is_calculated
    on suts.t_volunteer_rating (is_calculated);

create table suts.t_volunteer_task
(
    id                bigint               not null comment 'id'
        primary key,
    batch_id          bigint               null comment '批次id',
    task_name         varchar(100)         null comment '任务名称',
    file_address      varchar(500)         null comment '文件地址',
    fail_file_address varchar(500)         null comment '发放失败用户文件地址',
    send_num          bigint(14)           null comment '发送的数量',
    send_type         tinyint(1)           null comment '发送类型 0：立即发送 1：定时发送',
    send_time         datetime             null comment '发送时间',
    status            tinyint(2)           null comment '状态 0：待执行 1：执行中 2：执行失败 3：执行成功 4：取消',
    completion_time   datetime             null comment '完成时间',
    operator_id       bigint               null comment '操作人',
    create_time       datetime             null comment '创建时间',
    update_time       datetime             null comment '修改时间',
    del_flag          tinyint(1) default 0 null comment '删除标识 0：未删除 1：已删除'
)
    comment '志愿者任务表' charset = utf8mb4;

create table suts.t_volunteer_task_fail
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    batch_id    bigint                             not null comment '批次ID',
    json_object varchar(200)                       null comment 'JSON字符串，存储失败原因、Excel行数等信息',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '任务失败记录表' charset = utf8mb4;

create index idx_batch_id
    on suts.t_volunteer_task_fail (batch_id)
    comment '批次ID索引';

create table suts.t_volunteer_user
(
    id          bigint auto_increment comment '志愿者id'
        primary key,
    name        varchar(15) null comment '姓名',
    sex         tinyint(1)  null comment '性别 1/0 男/女',
    phone       varchar(15) null comment '志愿者手机号',
    birthday    datetime    null comment '生日',
    location    varchar(50) null comment '经纬度',
    score       double      null comment '评分',
    start_time  varchar(8)  null comment '志愿时间开始',
    end_time    varchar(8)  null comment '志愿时间结束',
    create_time datetime    null comment '创建时间',
    update_time datetime    null comment '修改时间',
    del_flag    tinyint     null comment '逻辑删除 0存在/1删除',
    constraint uk_phone
        unique (phone)
);

create index idx_create_time
    on suts.t_volunteer_user (create_time);

create index idx_del_score
    on suts.t_volunteer_user (del_flag, score);

create index idx_name
    on suts.t_volunteer_user (name);

create index idx_score_id
    on suts.t_volunteer_user (score, id);

