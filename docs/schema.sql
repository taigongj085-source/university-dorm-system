-- 大学宿舍管理系统数据库脚本
-- MySQL 8

CREATE DATABASE IF NOT EXISTS university_dorm
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE university_dorm;

-- ----------------------------
-- 清理旧表（注意依赖顺序）
-- ----------------------------
DROP TABLE IF EXISTS hygiene_check;
DROP TABLE IF EXISTS visitor_record;
DROP TABLE IF EXISTS leave_record;
DROP TABLE IF EXISTS repair_order;
DROP TABLE IF EXISTS dorm_bed;
DROP TABLE IF EXISTS dorm_room;
DROP TABLE IF EXISTS dorm_building;
DROP TABLE IF EXISTS notice;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS teacher;
DROP TABLE IF EXISTS sys_config;
DROP TABLE IF EXISTS sys_user;

-- ----------------------------
-- 系统用户（登录账号）
-- ----------------------------
CREATE TABLE sys_user (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  username      VARCHAR(32)  NOT NULL COMMENT '登录名',
  password      VARCHAR(64)  NOT NULL COMMENT '密码(MD5)',
  real_name     VARCHAR(32)  DEFAULT NULL COMMENT '真实姓名',
  phone         VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  email         VARCHAR(64)  DEFAULT NULL COMMENT '邮箱',
  avatar        VARCHAR(16)  DEFAULT '🧑' COMMENT '头像表情',
  role          VARCHAR(16)  NOT NULL DEFAULT 'STUDENT' COMMENT '角色：ADMIN/TEACHER/STUDENT',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
  deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

-- ----------------------------
-- 教师 / 辅导员
-- ----------------------------
CREATE TABLE teacher (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '教师ID',
  user_id       BIGINT       DEFAULT NULL COMMENT '关联登录账号',
  teacher_no    VARCHAR(32)  NOT NULL COMMENT '工号',
  name          VARCHAR(32)  NOT NULL COMMENT '姓名',
  gender        TINYINT      DEFAULT 1 COMMENT '性别：1男 2女',
  phone         VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  college       VARCHAR(64)  DEFAULT NULL COMMENT '所属学院',
  title         VARCHAR(32)  DEFAULT '辅导员' COMMENT '职称/职务',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1在职 0离职',
  deleted       TINYINT      NOT NULL DEFAULT 0,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_teacher_no (teacher_no),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师辅导员';

-- ----------------------------
-- 学生
-- ----------------------------
CREATE TABLE student (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '学生ID',
  user_id       BIGINT       DEFAULT NULL COMMENT '关联登录账号',
  student_no    VARCHAR(32)  NOT NULL COMMENT '学号',
  name          VARCHAR(32)  NOT NULL COMMENT '姓名',
  gender        TINYINT      DEFAULT 1 COMMENT '性别：1男 2女',
  phone         VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  college       VARCHAR(64)  DEFAULT NULL COMMENT '学院',
  major         VARCHAR(64)  DEFAULT NULL COMMENT '专业',
  grade         VARCHAR(16)  DEFAULT NULL COMMENT '年级',
  class_name    VARCHAR(32)  DEFAULT NULL COMMENT '班级',
  teacher_id    BIGINT       DEFAULT NULL COMMENT '辅导员ID',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1在校 0离校',
  deleted       TINYINT      NOT NULL DEFAULT 0,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_student_no (student_no),
  KEY idx_user_id (user_id),
  KEY idx_teacher_id (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生';

-- ----------------------------
-- 楼栋
-- ----------------------------
CREATE TABLE dorm_building (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '楼栋ID',
  name          VARCHAR(64)  NOT NULL COMMENT '楼栋名称',
  code          VARCHAR(32)  NOT NULL COMMENT '楼栋编号',
  gender_type   TINYINT      NOT NULL DEFAULT 1 COMMENT '1男寝 2女寝 3混合',
  floors        INT          NOT NULL DEFAULT 6 COMMENT '楼层数',
  manager_name  VARCHAR(32)  DEFAULT NULL COMMENT '宿管姓名',
  manager_phone VARCHAR(20)  DEFAULT NULL COMMENT '宿管电话',
  address       VARCHAR(128) DEFAULT NULL COMMENT '位置描述',
  remark        VARCHAR(255) DEFAULT NULL COMMENT '备注',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  deleted       TINYINT      NOT NULL DEFAULT 0,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宿舍楼栋';

-- ----------------------------
-- 房间
-- ----------------------------
CREATE TABLE dorm_room (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '房间ID',
  building_id   BIGINT       NOT NULL COMMENT '楼栋ID',
  room_no       VARCHAR(32)  NOT NULL COMMENT '房间号',
  floor_no      INT          NOT NULL COMMENT '所在楼层',
  bed_count     INT          NOT NULL DEFAULT 4 COMMENT '床位数',
  room_type     VARCHAR(32)  DEFAULT '标准间' COMMENT '房间类型',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1可用 0维修中 2停用',
  deleted       TINYINT      NOT NULL DEFAULT 0,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_building_room (building_id, room_no),
  KEY idx_building_id (building_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宿舍房间';

-- ----------------------------
-- 床位入住
-- ----------------------------
CREATE TABLE dorm_bed (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '床位ID',
  room_id       BIGINT       NOT NULL COMMENT '房间ID',
  bed_no        VARCHAR(16)  NOT NULL COMMENT '床位号，如 A/B/C/D',
  student_id    BIGINT       DEFAULT NULL COMMENT '入住学生ID，空表示空床',
  check_in_date DATE         DEFAULT NULL COMMENT '入住日期',
  status        TINYINT      NOT NULL DEFAULT 0 COMMENT '0空闲 1已入住',
  deleted       TINYINT      NOT NULL DEFAULT 0,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_room_bed (room_id, bed_no),
  KEY idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='床位';

-- ----------------------------
-- 公告
-- ----------------------------
CREATE TABLE notice (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '公告ID',
  title         VARCHAR(120) NOT NULL COMMENT '标题',
  content       MEDIUMTEXT   NOT NULL COMMENT '内容',
  category      VARCHAR(32)  DEFAULT '通知' COMMENT '分类：通知/安全/活动/维修',
  is_top        TINYINT      NOT NULL DEFAULT 0 COMMENT '是否置顶',
  publisher     VARCHAR(32)  DEFAULT '宿管中心' COMMENT '发布人',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1发布 0草稿',
  deleted       TINYINT      NOT NULL DEFAULT 0,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宿舍公告';

-- ----------------------------
-- 报修工单
-- ----------------------------
CREATE TABLE repair_order (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '报修ID',
  student_id    BIGINT       NOT NULL COMMENT '报修学生',
  room_id       BIGINT       DEFAULT NULL COMMENT '关联房间',
  title         VARCHAR(120) NOT NULL COMMENT '报修标题',
  description   VARCHAR(1000) NOT NULL COMMENT '问题描述',
  category      VARCHAR(32)  DEFAULT '水电' COMMENT '类别：水电/门窗/家具/网络/其他',
  status        TINYINT      NOT NULL DEFAULT 0 COMMENT '0待处理 1处理中 2已完成 3已关闭',
  reply         VARCHAR(500) DEFAULT NULL COMMENT '处理回复',
  deleted       TINYINT      NOT NULL DEFAULT 0,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_student_id (student_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报修工单';

-- ----------------------------
-- 请假 / 晚归登记
-- ----------------------------
CREATE TABLE leave_record (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
  student_id    BIGINT       NOT NULL COMMENT '学生ID',
  leave_type    TINYINT      NOT NULL DEFAULT 1 COMMENT '1请假 2晚归 3临时外出',
  reason        VARCHAR(500) NOT NULL COMMENT '事由',
  start_time    DATETIME     NOT NULL COMMENT '开始时间',
  end_time      DATETIME     DEFAULT NULL COMMENT '结束时间',
  status        TINYINT      NOT NULL DEFAULT 0 COMMENT '0待审核 1已通过 2已驳回',
  audit_remark  VARCHAR(255) DEFAULT NULL COMMENT '审核备注',
  deleted       TINYINT      NOT NULL DEFAULT 0,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假晚归';

-- ----------------------------
-- 访客登记
-- ----------------------------
CREATE TABLE visitor_record (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '访客ID',
  student_id    BIGINT       NOT NULL COMMENT '被访学生',
  visitor_name  VARCHAR(32)  NOT NULL COMMENT '访客姓名',
  visitor_phone VARCHAR(20)  DEFAULT NULL COMMENT '访客电话',
  id_card_tail  VARCHAR(8)   DEFAULT NULL COMMENT '证件后四位',
  reason        VARCHAR(255) NOT NULL COMMENT '来访事由',
  visit_time    DATETIME     NOT NULL COMMENT '来访时间',
  leave_time    DATETIME     DEFAULT NULL COMMENT '离开时间',
  status        TINYINT      NOT NULL DEFAULT 0 COMMENT '0在访 1已离开 2已取消',
  deleted       TINYINT      NOT NULL DEFAULT 0,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访客登记';

-- ----------------------------
-- 宿舍卫生检查
-- ----------------------------
CREATE TABLE hygiene_check (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '检查ID',
  room_id       BIGINT       NOT NULL COMMENT '房间ID',
  check_date    DATE         NOT NULL COMMENT '检查日期',
  score         INT          NOT NULL DEFAULT 90 COMMENT '得分',
  level_label   VARCHAR(16)  DEFAULT '良好' COMMENT '等级：优秀/良好/合格/整改',
  checker       VARCHAR(32)  DEFAULT '宿管' COMMENT '检查人',
  remark        VARCHAR(500) DEFAULT NULL COMMENT '评语',
  deleted       TINYINT      NOT NULL DEFAULT 0,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_room_id (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='卫生检查';

-- ----------------------------
-- 系统设置（键值）
-- ----------------------------
CREATE TABLE sys_config (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  config_key    VARCHAR(64)  NOT NULL COMMENT '配置键',
  config_value  VARCHAR(500) NOT NULL COMMENT '配置值',
  remark        VARCHAR(128) DEFAULT NULL COMMENT '说明',
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置';

-- ----------------------------
-- 种子数据（密码均为 123456 的 MD5：e10adc3949ba59abbe56e057f20f883e）
-- ----------------------------
INSERT INTO sys_user (username, password, real_name, phone, role, avatar) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e', '系统管理员', '13800000000', 'ADMIN', '🛡️'),
('teacher01', 'e10adc3949ba59abbe56e057f20f883e', '王老师', '13800000001', 'TEACHER', '👩‍🏫'),
('2021001', 'e10adc3949ba59abbe56e057f20f883e', '张晓明', '13900000001', 'STUDENT', '👨‍🎓'),
('2021002', 'e10adc3949ba59abbe56e057f20f883e', '李思雨', '13900000002', 'STUDENT', '👩‍🎓'),
('2021003', 'e10adc3949ba59abbe56e057f20f883e', '陈浩', '13900000003', 'STUDENT', '🧑‍🎓');

INSERT INTO teacher (user_id, teacher_no, name, gender, phone, college, title) VALUES
(2, 'T2021001', '王老师', 2, '13800000001', '计算机学院', '辅导员');

INSERT INTO student (user_id, student_no, name, gender, phone, college, major, grade, class_name, teacher_id) VALUES
(3, '2021001', '张晓明', 1, '13900000001', '计算机学院', '软件工程', '2021', '软工2101', 1),
(4, '2021002', '李思雨', 2, '13900000002', '计算机学院', '软件工程', '2021', '软工2101', 1),
(5, '2021003', '陈浩', 1, '13900000003', '计算机学院', '网络工程', '2021', '网工2102', 1);

INSERT INTO dorm_building (name, code, gender_type, floors, manager_name, manager_phone, address) VALUES
('梅园1号楼', 'MY-01', 1, 6, '刘阿姨', '13700001111', '东区梅园路1号'),
('兰园2号楼', 'LY-02', 2, 6, '赵阿姨', '13700002222', '西区兰园路2号'),
('竹园3号楼', 'ZY-03', 1, 8, '孙师傅', '13700003333', '南区竹园路3号');

INSERT INTO dorm_room (building_id, room_no, floor_no, bed_count, room_type) VALUES
(1, '301', 3, 4, '标准四人间'),
(1, '302', 3, 4, '标准四人间'),
(1, '401', 4, 4, '标准四人间'),
(2, '201', 2, 4, '标准四人间'),
(2, '202', 2, 4, '标准四人间'),
(3, '501', 5, 6, '六人间');

INSERT INTO dorm_bed (room_id, bed_no, student_id, check_in_date, status) VALUES
(1, 'A', 1, '2024-09-01', 1),
(1, 'B', 3, '2024-09-01', 1),
(1, 'C', NULL, NULL, 0),
(1, 'D', NULL, NULL, 0),
(4, 'A', 2, '2024-09-01', 1),
(4, 'B', NULL, NULL, 0),
(4, 'C', NULL, NULL, 0),
(4, 'D', NULL, NULL, 0);

INSERT INTO notice (title, content, category, is_top, publisher) VALUES
('关于2026年春季学期宿舍安全检查的通知',
 '各宿舍楼将于本周五起开展春季安全用电检查，请同学们提前整理电线、严禁使用违章电器，配合宿管老师工作。',
 '安全', 1, '宿管中心'),
('热水供应时间调整公告',
 '自下周一起，热水供应时间为每日 06:00–08:30、12:00–14:00、18:00–23:30。请合理安排洗漱时间。',
 '通知', 0, '后勤处'),
('宿舍文化节报名开始',
 '本学期宿舍文化节主题为「温暖一隅」。欢迎以宿舍为单位报名参加布置评比、趣味运动会等活动。',
 '活动', 0, '学生处');

INSERT INTO repair_order (student_id, room_id, title, description, category, status, reply) VALUES
(1, 1, '宿舍灯管闪烁', '301室靠窗灯管晚上频繁闪烁，影响学习休息。', '水电', 1, '维修师傅已接单，预计明日上门。'),
(2, 4, '门锁松动', '房门反锁后不好打开，存在安全隐患。', '门窗', 0, NULL);

INSERT INTO leave_record (student_id, leave_type, reason, start_time, end_time, status) VALUES
(1, 1, '回家办事', '2026-03-20 08:00:00', '2026-03-22 20:00:00', 1),
(3, 2, '实验室项目加班', '2026-03-15 23:10:00', '2026-03-15 23:40:00', 0);

INSERT INTO visitor_record (student_id, visitor_name, visitor_phone, id_card_tail, reason, visit_time, leave_time, status) VALUES
(1, '王建国', '13600001111', '1024', '家长探望', '2026-03-16 14:20:00', '2026-03-16 16:05:00', 1),
(2, '周小艺', '13600002222', '7788', '同学串门', '2026-03-18 10:00:00', NULL, 0);

INSERT INTO hygiene_check (room_id, check_date, score, level_label, checker, remark) VALUES
(1, '2026-03-10', 96, '优秀', '刘阿姨', '地面整洁，物品摆放规范。'),
(1, '2026-03-17', 88, '良好', '刘阿姨', '书桌略乱，建议每日整理。'),
(4, '2026-03-17', 92, '优秀', '赵阿姨', '整体干净，阳台物品摆放有序。');

INSERT INTO sys_config (config_key, config_value, remark) VALUES
('site_name', '大学宿舍管理系统', '站点名称'),
('curfew_time', '23:00', '门禁时间'),
('hotline', '400-800-2026', '宿管服务热线'),
('allow_register', '1', '是否开放前台注册：1是 0否');
