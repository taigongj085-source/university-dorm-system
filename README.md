# 大学宿舍管理系统（栖舍）

基于 **Spring Boot 4** 的单体宿舍管理系统。

## 技术栈

- Spring Boot 4.0.8
- Spring Security（表单登录 + 角色授权）
- MySQL 8 + MyBatis-Plus
- Redis（统计/公告缓存，失败自动降级）
- Thymeleaf + Hutool

## 功能

### 前台
注册登录、首页、公告、我的宿舍、报修、请假晚归、访客登记、卫生记录、入住指南、个人中心

### 后台（需 ADMIN）
仪表盘、楼栋/房间/床位、学生/教师/账号、公告、报修处理、请假审核、访客、卫生检查、系统设置

## 快速启动

1. 导入数据库：

```bash
mysql -uroot -p < docs/schema.sql
```

2. 修改 `src/main/resources/application.yml` 中的 MySQL / Redis 连接（默认端口参考本机：`3307` / `6379`）。

3. 启动：

```bash
mvn spring-boot:run
```

4. 访问：

- 前台：http://localhost:8088/
- 后台：http://localhost:8088/admin/login

## 演示账号（密码均为 123456）

| 账号 | 角色 |
|------|------|
| admin | 管理员 |
| teacher01 | 教师 |
| 2021001 | 学生 |

## 目录

```
docs/schema.sql
dorm-ui/                 # 纯静态原型（可选预览）
src/main/java/com/campus/dorm/
src/main/resources/templates/{front,back}
src/main/resources/static/
```
