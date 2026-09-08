package com.campus.dorm.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.dorm.entity.LeaveRecord;
import com.campus.dorm.entity.Notice;
import com.campus.dorm.entity.RepairOrder;
import com.campus.dorm.entity.Student;
import com.campus.dorm.entity.SysConfig;
import com.campus.dorm.mapper.DormBedMapper;
import com.campus.dorm.mapper.DormBuildingMapper;
import com.campus.dorm.mapper.LeaveRecordMapper;
import com.campus.dorm.mapper.NoticeMapper;
import com.campus.dorm.mapper.RepairOrderMapper;
import com.campus.dorm.mapper.StudentMapper;
import com.campus.dorm.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DormBizService {

    private final NoticeMapper noticeMapper;
    private final SysConfigMapper sysConfigMapper;
    private final StudentMapper studentMapper;
    private final DormBuildingMapper buildingMapper;
    private final DormBedMapper bedMapper;
    private final RepairOrderMapper repairOrderMapper;
    private final LeaveRecordMapper leaveRecordMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${dorm.redis.stats-key:dorm:stats}")
    private String statsKey;

    @Value("${dorm.redis.notices-key:dorm:notices:top}")
    private String noticesKey;

    @Value("${dorm.redis.ttl-seconds:60}")
    private long ttlSeconds;

    public Map<String, String> configMap() {
        return sysConfigMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysConfig::getConfigKey, SysConfig::getConfigValue, (a, b) -> a));
    }

    public String config(String key, String defaultValue) {
        SysConfig cfg = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key));
        return cfg == null ? defaultValue : cfg.getConfigValue();
    }

    public List<Notice> latestNotices(int limit) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(noticesKey);
            if (cached != null && !cached.isBlank()) {
                return JSONUtil.toList(cached, Notice.class);
            }
        } catch (Exception e) {
            log.debug("redis notices miss: {}", e.getMessage());
        }
        List<Notice> list = noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, 1)
                .orderByDesc(Notice::getIsTop)
                .orderByDesc(Notice::getCreateTime)
                .last("limit " + limit));
        try {
            stringRedisTemplate.opsForValue().set(noticesKey, JSONUtil.toJsonStr(list), ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("redis notices set fail: {}", e.getMessage());
        }
        return list;
    }

    public Map<String, Object> dashboardStats() {
        try {
            String cached = stringRedisTemplate.opsForValue().get(statsKey);
            if (cached != null && !cached.isBlank()) {
                return JSONUtil.toBean(cached, Map.class);
            }
        } catch (Exception e) {
            log.debug("redis stats miss: {}", e.getMessage());
        }
        Map<String, Object> stats = new HashMap<>();
        stats.put("students", studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getStatus, 1)));
        stats.put("buildings", buildingMapper.selectCount(null));
        stats.put("repairs", repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                .in(RepairOrder::getStatus, 0, 1)));
        stats.put("leaves", leaveRecordMapper.selectCount(new LambdaQueryWrapper<LeaveRecord>()
                .eq(LeaveRecord::getStatus, 0)));
        stats.put("occupiedBeds", bedMapper.selectCount(new LambdaQueryWrapper<com.campus.dorm.entity.DormBed>()
                .eq(com.campus.dorm.entity.DormBed::getStatus, 1)));
        try {
            stringRedisTemplate.opsForValue().set(statsKey, JSONUtil.toJsonStr(stats), ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("redis stats set fail: {}", e.getMessage());
        }
        return stats;
    }

    public void clearCache() {
        try {
            stringRedisTemplate.delete(List.of(statsKey, noticesKey));
        } catch (Exception ignored) {
        }
    }
}
