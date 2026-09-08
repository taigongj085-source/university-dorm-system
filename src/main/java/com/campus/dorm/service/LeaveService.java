package com.campus.dorm.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.dorm.common.BusinessException;
import com.campus.dorm.entity.LeaveRecord;
import com.campus.dorm.entity.Student;
import com.campus.dorm.mapper.LeaveRecordMapper;
import com.campus.dorm.mapper.StudentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRecordMapper leaveRecordMapper;
    private final StudentMapper studentMapper;
    private final StatsCacheService statsCacheService;

    public Page<LeaveRecord> pageMine(Long studentId, long page, long size) {
        Page<LeaveRecord> result = leaveRecordMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<LeaveRecord>()
                        .eq(LeaveRecord::getStudentId, studentId)
                        .orderByDesc(LeaveRecord::getId));
        result.getRecords().forEach(this::fillMeta);
        return result;
    }

    public Page<LeaveRecord> adminPage(Integer status, long page, long size) {
        LambdaQueryWrapper<LeaveRecord> qw = new LambdaQueryWrapper<>();
        if (status != null) {
            qw.eq(LeaveRecord::getStatus, status);
        }
        qw.orderByAsc(LeaveRecord::getStatus).orderByDesc(LeaveRecord::getId);
        Page<LeaveRecord> result = leaveRecordMapper.selectPage(new Page<>(page, size), qw);
        result.getRecords().forEach(this::fillMeta);
        return result;
    }

    public void submit(Long studentId, Integer leaveType, String reason,
                       LocalDateTime startTime, LocalDateTime endTime) {
        if (StrUtil.isBlank(reason) || startTime == null) {
            throw new BusinessException("请填写事由和开始时间");
        }
        LeaveRecord record = new LeaveRecord();
        record.setStudentId(studentId);
        record.setLeaveType(leaveType == null ? 1 : leaveType);
        record.setReason(reason.trim());
        record.setStartTime(startTime);
        record.setEndTime(endTime);
        record.setStatus(0);
        leaveRecordMapper.insert(record);
        statsCacheService.clearCache();
    }

    public void deleteMine(Long studentId, Long id) {
        LeaveRecord record = leaveRecordMapper.selectById(id);
        if (record == null || !studentId.equals(record.getStudentId())) {
            throw new BusinessException("记录不存在");
        }
        if (record.getStatus() != null && record.getStatus() != 0) {
            throw new BusinessException("仅待审核记录可删除");
        }
        leaveRecordMapper.deleteById(id);
        statsCacheService.clearCache();
    }

    public void audit(Long id, Integer status, String auditRemark) {
        if (status == null || (status != 1 && status != 2)) {
            throw new BusinessException("审核状态无效");
        }
        LeaveRecord record = leaveRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("记录不存在");
        }
        LeaveRecord upd = new LeaveRecord();
        upd.setId(id);
        upd.setStatus(status);
        upd.setAuditRemark(auditRemark);
        leaveRecordMapper.updateById(upd);
        statsCacheService.clearCache();
    }

    private void fillMeta(LeaveRecord record) {
        Student student = studentMapper.selectById(record.getStudentId());
        if (student != null) {
            record.setStudentName(student.getName());
            record.setStudentNo(student.getStudentNo());
        }
    }
}
