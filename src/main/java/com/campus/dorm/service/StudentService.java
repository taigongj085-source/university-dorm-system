package com.campus.dorm.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.dorm.common.BusinessException;
import com.campus.dorm.entity.DormBed;
import com.campus.dorm.entity.DormBuilding;
import com.campus.dorm.entity.DormRoom;
import com.campus.dorm.entity.Student;
import com.campus.dorm.entity.Teacher;
import com.campus.dorm.mapper.DormBedMapper;
import com.campus.dorm.mapper.DormBuildingMapper;
import com.campus.dorm.mapper.DormRoomMapper;
import com.campus.dorm.mapper.StudentMapper;
import com.campus.dorm.mapper.TeacherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final DormBedMapper dormBedMapper;
    private final DormRoomMapper dormRoomMapper;
    private final DormBuildingMapper dormBuildingMapper;

    public Student findByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                .eq(Student::getUserId, userId));
    }

    public Student requireByUserId(Long userId) {
        Student student = findByUserId(userId);
        if (student == null) {
            throw new BusinessException("当前账号未绑定学生信息");
        }
        return student;
    }

    public Student findById(Long id) {
        return studentMapper.selectById(id);
    }

    public Page<Student> page(String q, long page, long size) {
        LambdaQueryWrapper<Student> qw = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(q)) {
            qw.and(w -> w.like(Student::getStudentNo, q)
                    .or().like(Student::getName, q)
                    .or().like(Student::getClassName, q)
                    .or().like(Student::getCollege, q));
        }
        qw.orderByDesc(Student::getId);
        Page<Student> result = studentMapper.selectPage(new Page<>(page, size), qw);
        for (Student s : result.getRecords()) {
            fillMeta(s);
        }
        return result;
    }

    public List<Student> listAllSimple() {
        return studentMapper.selectList(new LambdaQueryWrapper<Student>()
                .eq(Student::getStatus, 1)
                .orderByAsc(Student::getStudentNo));
    }

    public void save(Student form) {
        if (StrUtil.isBlank(form.getStudentNo()) || StrUtil.isBlank(form.getName())) {
            throw new BusinessException("学号和姓名不能为空");
        }
        Long exists = studentMapper.selectCount(new LambdaQueryWrapper<Student>()
                .eq(Student::getStudentNo, form.getStudentNo())
                .ne(form.getId() != null, Student::getId, form.getId()));
        if (exists != null && exists > 0) {
            throw new BusinessException("学号已存在");
        }
        if (form.getStatus() == null) {
            form.setStatus(1);
        }
        if (form.getGender() == null) {
            form.setGender(1);
        }
        if (form.getId() == null) {
            studentMapper.insert(form);
        } else {
            studentMapper.updateById(form);
        }
    }

    public void delete(Long id) {
        Long occupied = dormBedMapper.selectCount(new LambdaQueryWrapper<DormBed>()
                .eq(DormBed::getStudentId, id)
                .eq(DormBed::getStatus, 1));
        if (occupied != null && occupied > 0) {
            throw new BusinessException("学生仍在住，请先办理退宿");
        }
        studentMapper.deleteById(id);
    }

    public void fillMeta(Student s) {
        if (s == null) {
            return;
        }
        if (s.getTeacherId() != null) {
            Teacher t = teacherMapper.selectById(s.getTeacherId());
            if (t != null) {
                s.setTeacherName(t.getName());
            }
        }
        DormBed bed = dormBedMapper.selectOne(new LambdaQueryWrapper<DormBed>()
                .eq(DormBed::getStudentId, s.getId())
                .eq(DormBed::getStatus, 1)
                .last("LIMIT 1"));
        if (bed != null) {
            DormRoom room = dormRoomMapper.selectById(bed.getRoomId());
            if (room != null) {
                DormBuilding building = dormBuildingMapper.selectById(room.getBuildingId());
                String bName = building == null ? "" : building.getName();
                s.setDormLabel(bName + " · " + room.getRoomNo() + " · 床位" + bed.getBedNo());
            }
        }
    }
}
