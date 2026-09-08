package com.campus.dorm.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.dorm.common.BusinessException;
import com.campus.dorm.common.DormConstants;
import com.campus.dorm.entity.Student;
import com.campus.dorm.entity.SysUser;
import com.campus.dorm.mapper.StudentMapper;
import com.campus.dorm.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper sysUserMapper;
    private final StudentMapper studentMapper;

    public SysUser findByUsername(String username) {
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
    }

    @Transactional
    public SysUser register(String studentNo, String realName, String phone, String password, String confirm) {
        if (StrUtil.hasBlank(studentNo, realName, password)) {
            throw new BusinessException("学号、姓名、密码不能为空");
        }
        if (!StrUtil.equals(password, confirm)) {
            throw new BusinessException("两次密码不一致");
        }
        if (password.length() < 6) {
            throw new BusinessException("密码至少 6 位");
        }
        if (findByUsername(studentNo) != null) {
            throw new BusinessException("该学号已注册");
        }
        SysUser user = new SysUser();
        user.setUsername(studentNo.trim());
        user.setPassword(DigestUtil.md5Hex(password));
        user.setRealName(realName.trim());
        user.setPhone(StrUtil.blankToDefault(phone, null));
        user.setAvatar("👨‍🎓");
        user.setRole(DormConstants.ROLE_STUDENT);
        user.setStatus(DormConstants.STATUS_OK);
        sysUserMapper.insert(user);

        Student student = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                .eq(Student::getStudentNo, studentNo.trim()));
        if (student == null) {
            student = new Student();
            student.setStudentNo(studentNo.trim());
            student.setName(realName.trim());
            student.setPhone(phone);
            student.setStatus(DormConstants.STATUS_OK);
            student.setUserId(user.getId());
            studentMapper.insert(student);
        } else {
            student.setUserId(user.getId());
            student.setName(realName.trim());
            if (StrUtil.isNotBlank(phone)) {
                student.setPhone(phone);
            }
            studentMapper.updateById(student);
        }
        return user;
    }

    public void updateProfile(Long userId, String realName, String phone, String email, String newPassword) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (StrUtil.isNotBlank(realName)) {
            user.setRealName(realName.trim());
        }
        user.setPhone(phone);
        user.setEmail(email);
        if (StrUtil.isNotBlank(newPassword)) {
            if (newPassword.length() < 6) {
                throw new BusinessException("密码至少 6 位");
            }
            user.setPassword(DigestUtil.md5Hex(newPassword));
        }
        sysUserMapper.updateById(user);
    }

    public Student findStudentByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return studentMapper.selectOne(new LambdaQueryWrapper<Student>().eq(Student::getUserId, userId));
    }
}
