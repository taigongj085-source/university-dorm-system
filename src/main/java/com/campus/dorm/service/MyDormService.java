package com.campus.dorm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.dorm.common.BusinessException;
import com.campus.dorm.entity.DormBed;
import com.campus.dorm.entity.DormBuilding;
import com.campus.dorm.entity.DormRoom;
import com.campus.dorm.entity.HygieneCheck;
import com.campus.dorm.entity.Student;
import com.campus.dorm.mapper.DormBedMapper;
import com.campus.dorm.mapper.DormBuildingMapper;
import com.campus.dorm.mapper.DormRoomMapper;
import com.campus.dorm.mapper.HygieneCheckMapper;
import com.campus.dorm.mapper.StudentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MyDormService {

    private final StudentMapper studentMapper;
    private final DormBedMapper bedMapper;
    private final DormRoomMapper roomMapper;
    private final DormBuildingMapper buildingMapper;
    private final HygieneCheckMapper hygieneCheckMapper;

    public Map<String, Object> loadMyDorm(Long userId) {
        Student me = studentMapper.selectOne(new LambdaQueryWrapper<Student>().eq(Student::getUserId, userId));
        if (me == null) {
            throw new BusinessException("未找到学生档案，请联系宿管");
        }
        DormBed myBed = bedMapper.selectOne(new LambdaQueryWrapper<DormBed>()
                .eq(DormBed::getStudentId, me.getId())
                .eq(DormBed::getStatus, 1)
                .last("limit 1"));
        Map<String, Object> data = new HashMap<>();
        data.put("student", me);
        if (myBed == null) {
            data.put("assigned", false);
            return data;
        }
        DormRoom room = roomMapper.selectById(myBed.getRoomId());
        DormBuilding building = room == null ? null : buildingMapper.selectById(room.getBuildingId());
        List<DormBed> beds = bedMapper.selectList(new LambdaQueryWrapper<DormBed>()
                .eq(DormBed::getRoomId, myBed.getRoomId())
                .orderByAsc(DormBed::getBedNo));
        List<Map<String, Object>> roommates = new ArrayList<>();
        for (DormBed bed : beds) {
            Map<String, Object> row = new HashMap<>();
            row.put("bed", bed);
            if (bed.getStudentId() != null) {
                row.put("student", studentMapper.selectById(bed.getStudentId()));
            }
            roommates.add(row);
        }
        List<HygieneCheck> hygiene = hygieneCheckMapper.selectList(new LambdaQueryWrapper<HygieneCheck>()
                .eq(HygieneCheck::getRoomId, myBed.getRoomId())
                .orderByDesc(HygieneCheck::getCheckDate)
                .last("limit 5"));
        data.put("assigned", true);
        data.put("myBed", myBed);
        data.put("room", room);
        data.put("building", building);
        data.put("roommates", roommates);
        data.put("hygiene", hygiene);
        return data;
    }

    public Long resolveRoomId(Long userId) {
        Map<String, Object> dorm = loadMyDorm(userId);
        if (!Boolean.TRUE.equals(dorm.get("assigned"))) {
            return null;
        }
        DormBed bed = (DormBed) dorm.get("myBed");
        return bed.getRoomId();
    }

    public Student requireStudent(Long userId) {
        Student me = studentMapper.selectOne(new LambdaQueryWrapper<Student>().eq(Student::getUserId, userId));
        if (me == null) {
            throw new BusinessException("未找到学生档案");
        }
        return me;
    }
}
