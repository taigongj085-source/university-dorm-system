package com.campus.dorm.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.dorm.entity.DormBed;
import com.campus.dorm.entity.LeaveRecord;
import com.campus.dorm.entity.RepairOrder;
import com.campus.dorm.entity.Student;
import com.campus.dorm.mapper.DormBedMapper;
import com.campus.dorm.mapper.DormBuildingMapper;
import com.campus.dorm.mapper.LeaveRecordMapper;
import com.campus.dorm.mapper.RepairOrderMapper;
import com.campus.dorm.mapper.StudentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsCacheService {

    private final StudentMapper studentMapper;
    private final DormBuildingMapper dormBuildingMapper;
    private final DormBedMapper dormBedMapper;
    private final RepairOrderMapper repairOrderMapper;
    private final LeaveRecordMapper leaveRecordMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${dorm.redis.stats-key:dorm:stats}")
    private String statsKey;

    @Value("${dorm.redis.cache-ttl-seconds:60}")
    private long ttlSeconds;

    public Map<String, Object> getDashboardStats() {
        try {
            String cached = stringRedisTemplate.opsForValue().get(statsKey);
            Map<String, Object> parsed = parse(cached);
            if (parsed != null) {
                return parsed;
            }
        } catch (Exception e) {
            log.warn("读取 Redis 统计缓存失败，回退数据库: {}", e.getMessage());
        }
        Map<String, Object> stats = loadFromDb();
        try {
            stringRedisTemplate.opsForValue().set(statsKey, format(stats), ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入 Redis 统计缓存失败: {}", e.getMessage());
        }
        return stats;
    }

    public void clearCache() {
        try {
            stringRedisTemplate.delete(statsKey);
        } catch (Exception e) {
            log.warn("清理 Redis 缓存失败: {}", e.getMessage());
        }
    }

    private Map<String, Object> loadFromDb() {
        Map<String, Object> stats = new HashMap<>();
        Long students = studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getStatus, 1));
        Long buildings = dormBuildingMapper.selectCount(null);
        Long occupied = dormBedMapper.selectCount(new LambdaQueryWrapper<DormBed>().eq(DormBed::getStatus, 1));
        Long pendingRepair = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>()
                .in(RepairOrder::getStatus, 0, 1));
        Long pendingLeave = leaveRecordMapper.selectCount(new LambdaQueryWrapper<LeaveRecord>()
                .eq(LeaveRecord::getStatus, 0));
        stats.put("students", students == null ? 0 : students);
        stats.put("buildings", buildings == null ? 0 : buildings);
        stats.put("occupiedBeds", occupied == null ? 0 : occupied);
        stats.put("pendingRepairs", pendingRepair == null ? 0 : pendingRepair);
        stats.put("pendingLeaves", pendingLeave == null ? 0 : pendingLeave);
        return stats;
    }

    private String format(Map<String, Object> stats) {
        return "students=" + stats.get("students")
                + ";buildings=" + stats.get("buildings")
                + ";occupiedBeds=" + stats.get("occupiedBeds")
                + ";pendingRepairs=" + stats.get("pendingRepairs")
                + ";pendingLeaves=" + stats.get("pendingLeaves");
    }

    private Map<String, Object> parse(String cached) {
        if (StrUtil.isBlank(cached)) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        for (String part : cached.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) {
                return null;
            }
            try {
                map.put(kv[0], Long.parseLong(kv[1]));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (!map.containsKey("students") || !map.containsKey("buildings")
                || !map.containsKey("occupiedBeds") || !map.containsKey("pendingRepairs")
                || !map.containsKey("pendingLeaves")) {
            return null;
        }
        return map;
    }
}
