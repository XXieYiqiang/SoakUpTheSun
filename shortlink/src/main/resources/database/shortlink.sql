create table suts.t_short_link_help
(
    id               bigint auto_increment comment '唯一ID'
        primary key,
    short_code       varchar(8)           not null comment '短链接码',
    full_short_link  varchar(64)          null comment '完整的短链接',
    target_room_link varchar(256)         not null comment '房间ID',
    auth_ticket      varchar(512)         null comment '透传给IM服务的鉴权票据/Token',
    invite_code      varchar(6)           not null comment '用户需输入的邀请码',
    expire_time      datetime             not null comment '过期时间',
    status           tinyint(1) default 1 null comment '0:等待中 1:进行中 2:已结束',
    create_time      datetime             null comment '创建时间',
    update_time      datetime             null comment '修改时间',
    del_flag         tinyint(2)           null comment '逻辑删除 0/1 未删除/删除',
    constraint uk_code
        unique (short_code)
)
    comment '求助短链路由表' charset = utf8mb4;

create index idx_expire
    on suts.t_short_link_help (expire_time);

create table suts.t_short_link_help_log
(
    id           bigint auto_increment
        primary key,
    request_id   bigint                             not null comment 'help_request.id',
    volunteer_id bigint                             not null comment '志愿者ID',
    join_time    datetime default CURRENT_TIMESTAMP null comment '进入房间时间',
    leave_time   datetime                           null comment '离开时间',
    create_time  datetime                           null comment '创建时间',
    update_time  datetime                           null comment '修改时间',
    del_flag     tinyint(2)                         null comment '逻辑删除 0/1 未删除/删除',
    constraint uk_req_vol
        unique (request_id, volunteer_id)
)
    comment '服务接单记录' charset = utf8mb4;

