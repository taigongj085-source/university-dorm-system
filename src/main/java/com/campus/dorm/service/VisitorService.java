package com.campus.dorm.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.dorm.common.BusinessException;
import com.campus.dorm.entity.Student;
import com.campus.dorm.entity.VisitorRecord;
import com.campus.dorm.mapper.StudentMapper;
import com.campus.dorm.mapper.VisitorRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VisitorService {

    private final VisitorRecordMapper visitorRecordMapper;
    private final StudentMapper studentMapper;

    public Page<VisitorRecord> pageMine(Long studentId, long page, long size) {
        Page<VisitorRecord> result = visitorRecordMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<VisitorRecord>()
                        .eq(VisitorRecord::getStudentId, studentId)
                        .orderByDesc(VisitorRecord::getId));
        result.getRecords().forEach(this::fillMeta);
        return result;
    }

    public Page<VisitorRecord> adminPage(Integer status, String q, long page, long size) {
        LambdaQueryWrapper<VisitorRecord> qw = new LambdaQueryWrapper<>();
        if (status != null) {
            qw.eq(VisitorRecord::getStatus, status);
        }
        if (StrUtil.isNotBlank(q)) {
            qw.like(VisitorRecord::getVisitorName, q);
        }
        qw.orderByAsc(VisitorRecord::getStatus).orderByDesc(VisitorRecord::getId);
        Page<VisitorRecord> result = visitorRecordMapper.selectPage(new Page<>(page, size), qw);
        result.getRecords().forEach(this::fillMeta);
        return result;
    }

    public void register(Long studentId, String visitorName, String visitorPhone,
                         String idCardTail, String reason, LocalDateTime visitTime) {
        if (StrUtil.isBlank(visitorName) || StrUtil.isBlank(reason)) {
            throw new BusinessException("请填写访客姓名和事由");
        }
        VisitorRecord record = new VisitorRecord();
        record.setStudentId(studentId);
        record.setVisitorName(visitorName.trim());
        record.setVisitorPhone(StrUtil.trim(visitorPhone));
        record.setIdCardTail(StrUtil.trim(idCardTail));
        record.setReason(reason.trim());
        record.setVisitTime(visitTime == null ? LocalDateTime.now() : visitTime);
        record.setStatus(0);
        visitorRecordMapper.insert(record);
    }

    public void leave(Long id, Long studentIdOrNull) {
        VisitorRecord record = visitorRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("访客记录不存在");
        }
        if (studentIdOrNull != null && !studentIdOrNull.equals(record.getStudentId())) {
            throw new BusinessException("无权操作该记录");
        }
        if (record.getStatus() != null && record.getStatus() == 1) {
            throw new BusinessException("访客已离开");
        }
        visitorRecordMapper.update(null, new LambdaUpdateWrapper<VisitorRecord>()
                .eq(VisitorRecord::getId, id)
                .set(VisitorRecord::getStatus, 1)
                .set(VisitorRecord::getLeaveTime, LocalDateTime.now()));
    }

    public void delete(Long id) {
        visitorRecordMapper.deleteById(id);
    }

    private void fillMeta(VisitorRecord record) {
        Student student = studentMapper.selectById(record.getStudentId());
        if (student != null) {
            record.setStudentName(student.getName());
        }
    }
}
