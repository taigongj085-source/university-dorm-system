package com.campus.dorm.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.dorm.common.BusinessException;
import com.campus.dorm.entity.Student;
import com.campus.dorm.entity.Teacher;
import com.campus.dorm.mapper.StudentMapper;
import com.campus.dorm.mapper.TeacherMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherMapper teacherMapper;
    private final StudentMapper studentMapper;

    public Page<Teacher> page(String q, long page, long size) {
        LambdaQueryWrapper<Teacher> qw = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(q)) {
            qw.and(w -> w.like(Teacher::getTeacherNo, q)
                    .or().like(Teacher::getName, q)
                    .or().like(Teacher::getCollege, q));
        }
        qw.orderByDesc(Teacher::getId);
        return teacherMapper.selectPage(new Page<>(page, size), qw);
    }

    public List<Teacher> listActive() {
        return teacherMapper.selectList(new LambdaQueryWrapper<Teacher>()
                .eq(Teacher::getStatus, 1)
                .orderByAsc(Teacher::getTeacherNo));
    }

    public void save(Teacher form) {
        if (StrUtil.isBlank(form.getTeacherNo()) || StrUtil.isBlank(form.getName())) {
            throw new BusinessException("工号和姓名不能为空");
        }
        Long exists = teacherMapper.selectCount(new LambdaQueryWrapper<Teacher>()
                .eq(Teacher::getTeacherNo, form.getTeacherNo())
                .ne(form.getId() != null, Teacher::getId, form.getId()));
        if (exists != null && exists > 0) {
            throw new BusinessException("工号已存在");
        }
        if (form.getStatus() == null) {
            form.setStatus(1);
        }
        if (form.getGender() == null) {
            form.setGender(1);
        }
        if (form.getId() == null) {
            teacherMapper.insert(form);
        } else {
            teacherMapper.updateById(form);
        }
    }

    public void delete(Long id) {
        Long bound = studentMapper.selectCount(new LambdaQueryWrapper<Student>()
                .eq(Student::getTeacherId, id));
        if (bound != null && bound > 0) {
            throw new BusinessException("仍有学生关联该辅导员，无法删除");
        }
        teacherMapper.deleteById(id);
    }
}
