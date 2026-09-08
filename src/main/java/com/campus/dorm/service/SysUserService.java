package com.campus.dorm.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
public class SysUserService {

    private final SysUserMapper sysUserMapper;
    private final StudentMapper studentMapper;
    private final SysConfigService sysConfigService;

    public SysUser findById(Long id) {
        return sysUserMapper.selectById(id);
    }

    public SysUser findByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            return null;
        }
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username.trim()));
    }

    public Page<SysUser> page(String q, String role, long page, long size) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(q)) {
            qw.and(w -> w.like(SysUser::getUsername, q)
                    .or().like(SysUser::getRealName, q)
                    .or().like(SysUser::getPhone, q));
        }
        if (StrUtil.isNotBlank(role)) {
            qw.eq(SysUser::getRole, role);
        }
        qw.orderByDesc(SysUser::getId);
        return sysUserMapper.selectPage(new Page<>(page, size), qw);
    }

    @Transactional
    public SysUser register(String username, String password, String password2,
                            String realName, String phone) {
        if (!"1".equals(sysConfigService.getValue("allow_register", "1"))) {
            throw new BusinessException("当前未开放注册");
        }
        username = StrUtil.trim(username);
        realName = StrUtil.trim(realName);
        if (StrUtil.isBlank(username)) {
            throw new BusinessException("请输入学号/账号");
        }
        if (StrUtil.isBlank(realName)) {
            throw new BusinessException("请输入真实姓名");
        }
        if (StrUtil.isBlank(password) || password.length() < 6) {
            throw new BusinessException("密码至少 6 位");
        }
        if (!password.equals(password2)) {
            throw new BusinessException("两次输入的密码不一致");
        }
        if (findByUsername(username) != null) {
            throw new BusinessException("该账号已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(DigestUtil.md5Hex(password));
        user.setRealName(realName);
        user.setPhone(StrUtil.trim(phone));
        user.setAvatar("🧑‍🎓");
        user.setRole(DormConstants.ROLE_STUDENT);
        user.setStatus(1);
        sysUserMapper.insert(user);

        Student student = new Student();
        student.setUserId(user.getId());
        student.setStudentNo(username);
        student.setName(realName);
        student.setPhone(user.getPhone());
        student.setGender(1);
        student.setStatus(1);
        studentMapper.insert(student);
        return user;
    }

    public void updateProfile(Long userId, String realName, String phone, String email, String avatar) {
        SysUser user = requireUser(userId);
        SysUser upd = new SysUser();
        upd.setId(userId);
        upd.setRealName(StrUtil.blankToDefault(StrUtil.trim(realName), user.getRealName()));
        upd.setPhone(StrUtil.trim(phone));
        upd.setEmail(StrUtil.trim(email));
        if (StrUtil.isNotBlank(avatar)) {
            upd.setAvatar(avatar.trim());
        }
        sysUserMapper.updateById(upd);

        Student student = studentMapper.selectOne(new LambdaQueryWrapper<Student>()
                .eq(Student::getUserId, userId));
        if (student != null) {
            Student sUpd = new Student();
            sUpd.setId(student.getId());
            sUpd.setName(upd.getRealName());
            sUpd.setPhone(upd.getPhone());
            studentMapper.updateById(sUpd);
        }
    }

    public void changePassword(Long userId, String oldPassword, String newPassword, String confirm) {
        SysUser user = requireUser(userId);
        if (StrUtil.isBlank(oldPassword) || !DigestUtil.md5Hex(oldPassword).equalsIgnoreCase(user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (StrUtil.isBlank(newPassword) || newPassword.length() < 6) {
            throw new BusinessException("新密码至少 6 位");
        }
        if (!newPassword.equals(confirm)) {
            throw new BusinessException("两次输入的新密码不一致");
        }
        SysUser upd = new SysUser();
        upd.setId(userId);
        upd.setPassword(DigestUtil.md5Hex(newPassword));
        sysUserMapper.updateById(upd);
    }

    public void saveAdmin(SysUser form) {
        if (StrUtil.isBlank(form.getUsername())) {
            throw new BusinessException("用户名不能为空");
        }
        if (form.getId() == null) {
            if (findByUsername(form.getUsername()) != null) {
                throw new BusinessException("用户名已存在");
            }
            if (StrUtil.isBlank(form.getPassword())) {
                form.setPassword("123456");
            }
            form.setPassword(DigestUtil.md5Hex(form.getPassword()));
            if (StrUtil.isBlank(form.getRole())) {
                form.setRole(DormConstants.ROLE_STUDENT);
            }
            if (form.getStatus() == null) {
                form.setStatus(1);
            }
            if (StrUtil.isBlank(form.getAvatar())) {
                form.setAvatar("🙂");
            }
            sysUserMapper.insert(form);
        } else {
            SysUser db = requireUser(form.getId());
            SysUser upd = new SysUser();
            upd.setId(form.getId());
            upd.setRealName(form.getRealName());
            upd.setPhone(form.getPhone());
            upd.setEmail(form.getEmail());
            upd.setAvatar(form.getAvatar());
            upd.setRole(form.getRole());
            upd.setStatus(form.getStatus());
            if (StrUtil.isNotBlank(form.getPassword())) {
                upd.setPassword(DigestUtil.md5Hex(form.getPassword()));
            }
            // 不允许改用户名冲突：用户名保持原样
            upd.setUsername(db.getUsername());
            sysUserMapper.updateById(upd);
        }
    }

    public void updateStatus(Long id, Integer status) {
        SysUser user = requireUser(id);
        if (DormConstants.ROLE_ADMIN.equalsIgnoreCase(user.getRole()) && status != null && status == 0) {
            throw new BusinessException("不能禁用管理员账号");
        }
        SysUser upd = new SysUser();
        upd.setId(id);
        upd.setStatus(status);
        sysUserMapper.updateById(upd);
    }

    public void delete(Long id) {
        SysUser user = requireUser(id);
        if (DormConstants.ROLE_ADMIN.equalsIgnoreCase(user.getRole())) {
            throw new BusinessException("不能删除管理员账号");
        }
        sysUserMapper.deleteById(id);
    }

    private SysUser requireUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }
}
