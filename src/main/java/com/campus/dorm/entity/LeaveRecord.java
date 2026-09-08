package com.campus.dorm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("leave_record")
public class LeaveRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Integer leaveType;
    private String reason;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private String auditRemark;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String studentName;
    @TableField(exist = false)
    private String studentNo;
}
