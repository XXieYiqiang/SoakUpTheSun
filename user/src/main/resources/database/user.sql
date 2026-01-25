create table suts.user
(
    id            bigint auto_increment comment 'id'
        primary key,
    user_account  varchar(256)                           not null comment '账号',
    user_password varchar(512)                           not null comment '密码',
    user_name     varchar(256)                           null comment '用户昵称',
    sex           tinyint(1)                             null comment '性别 0/1 女/男',
    location      varchar(50)                            null comment '位置',
    birthday      datetime                               null comment '生日',
    user_avatar   varchar(1024)                          null comment '用户头像',
    user_profile  varchar(512)                           null comment '用户简介',
    user_role     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    edit_time     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    create_time   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete     tinyint      default 0                 not null comment '是否删除',
    constraint uk_userAccount
        unique (user_account)
)
    comment '用户' collate = utf8mb4_unicode_ci;

create index idx_userName
    on suts.user (user_name);

