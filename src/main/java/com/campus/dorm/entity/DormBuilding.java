package com.campus.dorm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dorm_building")
public class DormBuilding {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private Integer genderType;
    private Integer floors;
    private String managerName;
    private String managerPhone;
    private String address;
    private String remark;
    private Integer status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
