create table suts.t_picture
(
    id            bigint auto_increment comment 'id'
        primary key,
    url           varchar(512)                       not null comment '图片 url',
    thumbnail_url varchar(512)                       null comment '缩略图 url',
    name          varchar(128)                       not null comment '图片名称',
    introduction  varchar(512)                       null comment '简介',
    tags          varchar(512)                       null comment '标签（JSON 数组）',
    pic_size      bigint                             null comment '图片体积',
    pic_width     int                                null comment '图片宽度',
    pic_height    int                                null comment '图片高度',
    pic_scale     double                             null comment '图片宽高比例',
    pic_format    varchar(32)                        null comment '图片格式',
    user_id       bigint                             not null comment '创建用户 id',
    create_time   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    del_flag      tinyint  default 0                 not null comment '是否删除'
)
    comment '图片' collate = utf8mb4_unicode_ci;

create index idx_introduction
    on suts.t_picture (introduction);

create index idx_name
    on suts.t_picture (name);

create index idx_tags
    on suts.t_picture (tags);

create index idx_userId
    on suts.t_picture (user_id);


create table suts.t_picture_space
(
    id          bigint auto_increment comment 'id'
        primary key,
    space_name  varchar(128)                       null comment '空间名称',
    max_size    bigint   default 0                 null comment '空间图片的最大总大小',
    max_count   bigint   default 0                 null comment '空间图片的最大数量',
    total_size  bigint   default 0                 null comment '当前空间下图片的总大小',
    total_count bigint   default 0                 null comment '当前空间下的图片数量',
    user_id     bigint                             not null comment '创建用户 id',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    del_flag    tinyint  default 0                 not null comment '是否删除'
)
    comment '空间' collate = utf8mb4_unicode_ci;

create index idx_spaceName
    on suts.t_picture_space (space_name);

create index idx_userId
    on suts.t_picture_space (user_id);

