package com.campus.dorm.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.dorm.common.BusinessException;
import com.campus.dorm.entity.DormBed;
import com.campus.dorm.entity.HygieneCheck;
import com.campus.dorm.mapper.DormBedMapper;
import com.campus.dorm.mapper.HygieneCheckMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HygieneService {

    private final HygieneCheckMapper hygieneCheckMapper;
    private final DormBedMapper dormBedMapper;
    private final DormService dormService;

    public Page<HygieneCheck> pageByStudent(Long studentId, long page, long size) {
        DormBed bed = dormBedMapper.selectOne(new LambdaQueryWrapper<DormBed>()
                .eq(DormBed::getStudentId, studentId)
                .eq(DormBed::getStatus, 1)
                .last("LIMIT 1"));
        if (bed == null) {
            return new Page<>(page, size);
        }
        return pageByRoom(bed.getRoomId(), page, size);
    }

    public Page<HygieneCheck> pageByRoom(Long roomId, long page, long size) {
        Page<HygieneCheck> result = hygieneCheckMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<HygieneCheck>()
                        .eq(roomId != null, HygieneCheck::getRoomId, roomId)
                        .orderByDesc(HygieneCheck::getCheckDate)
                        .orderByDesc(HygieneCheck::getId));
        result.getRecords().forEach(this::fillMeta);
        return result;
    }

    public Page<HygieneCheck> adminPage(Long roomId, String level, long page, long size) {
        LambdaQueryWrapper<HygieneCheck> qw = new LambdaQueryWrapper<>();
        if (roomId != null) {
            qw.eq(HygieneCheck::getRoomId, roomId);
        }
        if (StrUtil.isNotBlank(level)) {
            qw.eq(HygieneCheck::getLevelLabel, level);
        }
        qw.orderByDesc(HygieneCheck::getCheckDate).orderByDesc(HygieneCheck::getId);
        Page<HygieneCheck> result = hygieneCheckMapper.selectPage(new Page<>(page, size), qw);
        result.getRecords().forEach(this::fillMeta);
        return result;
    }

    public List<HygieneCheck> latestByRoom(Long roomId, int limit) {
        if (roomId == null) {
            return Collections.emptyList();
        }
        List<HygieneCheck> list = hygieneCheckMapper.selectList(new LambdaQueryWrapper<HygieneCheck>()
                .eq(HygieneCheck::getRoomId, roomId)
                .orderByDesc(HygieneCheck::getCheckDate)
                .last("LIMIT " + limit));
        list.forEach(this::fillMeta);
        return list;
    }

    public void save(HygieneCheck form) {
        if (form.getRoomId() == null || form.getScore() == null) {
            throw new BusinessException("请选择房间并填写得分");
        }
        if (form.getCheckDate() == null) {
            form.setCheckDate(LocalDate.now());
        }
        if (StrUtil.isBlank(form.getLevelLabel())) {
            form.setLevelLabel(levelOf(form.getScore()));
        }
        if (StrUtil.isBlank(form.getChecker())) {
            form.setChecker("宿管");
        }
        if (form.getId() == null) {
            hygieneCheckMapper.insert(form);
        } else {
            hygieneCheckMapper.updateById(form);
        }
    }

    public void delete(Long id) {
        hygieneCheckMapper.deleteById(id);
    }

    public static String levelOf(int score) {
        if (score >= 95) {
            return "优秀";
        }
        if (score >= 85) {
            return "良好";
        }
        if (score >= 70) {
            return "合格";
        }
        return "整改";
    }

    private void fillMeta(HygieneCheck check) {
        check.setRoomLabel(dormService.roomLabel(check.getRoomId()));
    }
}
