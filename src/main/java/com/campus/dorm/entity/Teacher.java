package com.campus.dorm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("teacher")
public class Teacher {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String teacherNo;
    private String name;
    private Integer gender;
    private String phone;
    private String college;
    private String title;
    private Integer status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
