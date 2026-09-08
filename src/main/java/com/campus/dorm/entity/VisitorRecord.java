package com.campus.dorm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("visitor_record")
public class VisitorRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String visitorName;
    private String visitorPhone;
    private String idCardTail;
    private String reason;
    private LocalDateTime visitTime;
    private LocalDateTime leaveTime;
    private Integer status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String studentName;
}
