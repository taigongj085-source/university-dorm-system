package com.campus.dorm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dorm_room")
public class DormRoom {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long buildingId;
    private String roomNo;
    private Integer floorNo;
    private Integer bedCount;
    private String roomType;
    private Integer status;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String buildingName;
    @TableField(exist = false)
    private Integer occupiedCount;
}
