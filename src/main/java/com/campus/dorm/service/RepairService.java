package com.campus.dorm.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.dorm.common.BusinessException;
import com.campus.dorm.entity.DormBed;
import com.campus.dorm.entity.RepairOrder;
import com.campus.dorm.entity.Student;
import com.campus.dorm.mapper.DormBedMapper;
import com.campus.dorm.mapper.RepairOrderMapper;
import com.campus.dorm.mapper.StudentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RepairService {

    private final RepairOrderMapper repairOrderMapper;
    private final StudentMapper studentMapper;
    private final DormBedMapper dormBedMapper;
    private final DormService dormService;
    private final StatsCacheService statsCacheService;

    public Page<RepairOrder> pageMine(Long studentId, long page, long size) {
        Page<RepairOrder> result = repairOrderMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<RepairOrder>()
                        .eq(RepairOrder::getStudentId, studentId)
                        .orderByDesc(RepairOrder::getId));
        result.getRecords().forEach(this::fillMeta);
        return result;
    }

    public Page<RepairOrder> adminPage(Integer status, String q, long page, long size) {
        LambdaQueryWrapper<RepairOrder> qw = new LambdaQueryWrapper<>();
        if (status != null) {
            qw.eq(RepairOrder::getStatus, status);
        }
        if (StrUtil.isNotBlank(q)) {
            qw.and(w -> w.like(RepairOrder::getTitle, q).or().like(RepairOrder::getDescription, q));
        }
        qw.orderByAsc(RepairOrder::getStatus).orderByDesc(RepairOrder::getId);
        Page<RepairOrder> result = repairOrderMapper.selectPage(new Page<>(page, size), qw);
        result.getRecords().forEach(this::fillMeta);
        return result;
    }

    public void submit(Long studentId, String title, String description, String category) {
        if (StrUtil.isBlank(title) || StrUtil.isBlank(description)) {
            throw new BusinessException("请填写报修标题和描述");
        }
        RepairOrder order = new RepairOrder();
        order.setStudentId(studentId);
        order.setTitle(title.trim());
        order.setDescription(description.trim());
        order.setCategory(StrUtil.blankToDefault(category, "其他"));
        order.setStatus(0);
        DormBed bed = dormBedMapper.selectOne(new LambdaQueryWrapper<DormBed>()
                .eq(DormBed::getStudentId, studentId)
                .eq(DormBed::getStatus, 1)
                .last("LIMIT 1"));
        if (bed != null) {
            order.setRoomId(bed.getRoomId());
        }
        repairOrderMapper.insert(order);
        statsCacheService.clearCache();
    }

    public void updateMine(Long studentId, Long id, String title, String description, String category) {
        RepairOrder order = requireMine(studentId, id);
        if (order.getStatus() != null && order.getStatus() != 0) {
            throw new BusinessException("仅待处理工单可修改");
        }
        RepairOrder upd = new RepairOrder();
        upd.setId(id);
        upd.setTitle(title);
        upd.setDescription(description);
        upd.setCategory(category);
        repairOrderMapper.updateById(upd);
    }

    public void deleteMine(Long studentId, Long id) {
        RepairOrder order = requireMine(studentId, id);
        if (order.getStatus() != null && order.getStatus() != 0) {
            throw new BusinessException("仅待处理工单可删除");
        }
        repairOrderMapper.deleteById(id);
        statsCacheService.clearCache();
    }

    public void handle(Long id, Integer status, String reply) {
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("工单不存在");
        }
        RepairOrder upd = new RepairOrder();
        upd.setId(id);
        upd.setStatus(status);
        upd.setReply(reply);
        repairOrderMapper.updateById(upd);
        statsCacheService.clearCache();
    }

    public void delete(Long id) {
        repairOrderMapper.deleteById(id);
        statsCacheService.clearCache();
    }

    private RepairOrder requireMine(Long studentId, Long id) {
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order == null || !studentId.equals(order.getStudentId())) {
            throw new BusinessException("工单不存在");
        }
        return order;
    }

    private void fillMeta(RepairOrder order) {
        Student student = studentMapper.selectById(order.getStudentId());
        if (student != null) {
            order.setStudentName(student.getName());
        }
        order.setRoomLabel(dormService.roomLabel(order.getRoomId()));
    }
}
