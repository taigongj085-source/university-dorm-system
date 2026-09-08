package com.campus.dorm.controller.back;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.dorm.common.DormConstants;
import com.campus.dorm.entity.*;
import com.campus.dorm.mapper.*;
import com.campus.dorm.service.DormBizService;
import com.campus.dorm.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DormBizService dormBizService;
    private final DormBuildingMapper buildingMapper;
    private final DormRoomMapper roomMapper;
    private final DormBedMapper bedMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final SysUserMapper userMapper;
    private final NoticeMapper noticeMapper;
    private final RepairOrderMapper repairOrderMapper;
    private final LeaveRecordMapper leaveRecordMapper;
    private final VisitorRecordMapper visitorRecordMapper;
    private final HygieneCheckMapper hygieneCheckMapper;
    private final SysConfigMapper sysConfigMapper;

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, Model model) {
        if (SecurityUtil.isAdmin()) {
            return "redirect:/admin";
        }
        model.addAttribute("error", "1".equals(error));
        return "back/login";
    }

    @GetMapping({"", "/"})
    public String index(Model model) {
        model.addAttribute("stats", dormBizService.dashboardStats());
        model.addAttribute("latestRepairs", repairOrderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                .orderByDesc(RepairOrder::getCreateTime).last("limit 5")));
        model.addAttribute("latestNotices", noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .orderByDesc(Notice::getCreateTime).last("limit 5")));
        model.addAttribute("menu", "dashboard");
        return "back/index";
    }

    // -------- 楼栋 --------
    @GetMapping("/buildings")
    public String buildings(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("list", buildingMapper.selectList(new LambdaQueryWrapper<DormBuilding>()
                .and(StrUtil.isNotBlank(q), w -> w.like(DormBuilding::getName, q).or().like(DormBuilding::getCode, q))
                .orderByAsc(DormBuilding::getCode)));
        model.addAttribute("q", q);
        model.addAttribute("menu", "building");
        return "back/building";
    }

    @PostMapping("/buildings/save")
    public String buildingSave(DormBuilding building) {
        if (building.getId() == null) {
            buildingMapper.insert(building);
        } else {
            buildingMapper.updateById(building);
        }
        dormBizService.clearCache();
        return "redirect:/admin/buildings";
    }

    @PostMapping("/buildings/delete/{id}")
    public String buildingDelete(@PathVariable Long id) {
        buildingMapper.deleteById(id);
        dormBizService.clearCache();
        return "redirect:/admin/buildings";
    }

    // -------- 房间 --------
    @GetMapping("/rooms")
    public String rooms(@RequestParam(required = false) Long buildingId, Model model) {
        model.addAttribute("buildings", buildingMapper.selectList(null));
        model.addAttribute("list", roomMapper.selectList(new LambdaQueryWrapper<DormRoom>()
                .eq(buildingId != null, DormRoom::getBuildingId, buildingId)
                .orderByAsc(DormRoom::getBuildingId).orderByAsc(DormRoom::getRoomNo)));
        model.addAttribute("buildingId", buildingId);
        model.addAttribute("menu", "room");
        return "back/room";
    }

    @PostMapping("/rooms/save")
    public String roomSave(DormRoom room) {
        if (room.getId() == null) {
            roomMapper.insert(room);
            // 自动生成床位 A B C D ...
            int beds = room.getBedCount() == null ? 4 : room.getBedCount();
            for (int i = 0; i < beds; i++) {
                DormBed bed = new DormBed();
                bed.setRoomId(room.getId());
                bed.setBedNo(String.valueOf((char) ('A' + i)));
                bed.setStatus(0);
                bedMapper.insert(bed);
            }
        } else {
            roomMapper.updateById(room);
        }
        return "redirect:/admin/rooms";
    }

    // -------- 床位 --------
    @GetMapping("/beds")
    public String beds(@RequestParam(required = false) Long roomId, Model model) {
        model.addAttribute("rooms", roomMapper.selectList(null));
        model.addAttribute("students", studentMapper.selectList(new LambdaQueryWrapper<Student>().eq(Student::getStatus, 1)));
        model.addAttribute("list", bedMapper.selectList(new LambdaQueryWrapper<DormBed>()
                .eq(roomId != null, DormBed::getRoomId, roomId)
                .orderByAsc(DormBed::getRoomId).orderByAsc(DormBed::getBedNo)));
        model.addAttribute("roomId", roomId);
        model.addAttribute("menu", "bed");
        return "back/bed";
    }

    @PostMapping("/beds/assign")
    public String bedAssign(@RequestParam Long bedId, @RequestParam(required = false) Long studentId) {
        DormBed bed = bedMapper.selectById(bedId);
        if (bed != null) {
            bed.setStudentId(studentId);
            bed.setStatus(studentId == null ? 0 : 1);
            bed.setCheckInDate(studentId == null ? null : LocalDate.now());
            bedMapper.updateById(bed);
            dormBizService.clearCache();
        }
        return "redirect:/admin/beds?roomId=" + (bed == null ? "" : bed.getRoomId());
    }

    // -------- 学生 --------
    @GetMapping("/students")
    public String students(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("list", studentMapper.selectList(new LambdaQueryWrapper<Student>()
                .and(StrUtil.isNotBlank(q), w -> w.like(Student::getName, q).or().like(Student::getStudentNo, q))
                .orderByDesc(Student::getId)));
        model.addAttribute("teachers", teacherMapper.selectList(null));
        model.addAttribute("q", q);
        model.addAttribute("menu", "student");
        return "back/student";
    }

    @PostMapping("/students/save")
    public String studentSave(Student student) {
        if (student.getId() == null) {
            studentMapper.insert(student);
        } else {
            studentMapper.updateById(student);
        }
        dormBizService.clearCache();
        return "redirect:/admin/students";
    }

    // -------- 教师 --------
    @GetMapping("/teachers")
    public String teachers(Model model) {
        model.addAttribute("list", teacherMapper.selectList(new LambdaQueryWrapper<Teacher>().orderByDesc(Teacher::getId)));
        model.addAttribute("menu", "teacher");
        return "back/teacher";
    }

    @PostMapping("/teachers/save")
    public String teacherSave(Teacher teacher) {
        if (teacher.getId() == null) {
            teacherMapper.insert(teacher);
        } else {
            teacherMapper.updateById(teacher);
        }
        return "redirect:/admin/teachers";
    }

    // -------- 用户 --------
    @GetMapping("/users")
    public String users(@RequestParam(required = false) String role, Model model) {
        model.addAttribute("list", userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(StrUtil.isNotBlank(role), SysUser::getRole, role)
                .orderByDesc(SysUser::getId)));
        model.addAttribute("role", role);
        model.addAttribute("menu", "user");
        return "back/user";
    }

    @PostMapping("/users/toggle/{id}")
    public String userToggle(@PathVariable Long id) {
        SysUser user = userMapper.selectById(id);
        if (user != null && !DormConstants.ROLE_ADMIN.equals(user.getRole())) {
            user.setStatus(user.getStatus() != null && user.getStatus() == 1 ? 0 : 1);
            userMapper.updateById(user);
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/reset/{id}")
    public String userReset(@PathVariable Long id, RedirectAttributes ra) {
        SysUser user = userMapper.selectById(id);
        if (user != null) {
            user.setPassword(DigestUtil.md5Hex("123456"));
            userMapper.updateById(user);
            ra.addFlashAttribute("okMsg", "已重置为 123456");
        }
        return "redirect:/admin/users";
    }

    // -------- 公告 --------
    @GetMapping("/notices")
    public String notices(Model model) {
        model.addAttribute("list", noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .orderByDesc(Notice::getIsTop).orderByDesc(Notice::getCreateTime)));
        model.addAttribute("menu", "notice");
        return "back/notice";
    }

    @PostMapping("/notices/save")
    public String noticeSave(Notice notice) {
        if (notice.getStatus() == null) {
            notice.setStatus(1);
        }
        if (notice.getIsTop() == null) {
            notice.setIsTop(0);
        }
        if (notice.getId() == null) {
            noticeMapper.insert(notice);
        } else {
            noticeMapper.updateById(notice);
        }
        dormBizService.clearCache();
        return "redirect:/admin/notices";
    }

    @PostMapping("/notices/delete/{id}")
    public String noticeDelete(@PathVariable Long id) {
        noticeMapper.deleteById(id);
        dormBizService.clearCache();
        return "redirect:/admin/notices";
    }

    // -------- 报修 --------
    @GetMapping("/repairs")
    public String repairs(@RequestParam(required = false) Integer status, Model model) {
        model.addAttribute("list", repairOrderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                .eq(status != null, RepairOrder::getStatus, status)
                .orderByDesc(RepairOrder::getCreateTime)));
        model.addAttribute("status", status);
        model.addAttribute("menu", "repair");
        return "back/repair";
    }

    @PostMapping("/repairs/update")
    public String repairUpdate(@RequestParam Long id,
                               @RequestParam Integer status,
                               @RequestParam(required = false) String reply) {
        RepairOrder order = repairOrderMapper.selectById(id);
        if (order != null) {
            order.setStatus(status);
            order.setReply(reply);
            repairOrderMapper.updateById(order);
            dormBizService.clearCache();
        }
        return "redirect:/admin/repairs";
    }

    // -------- 请假 --------
    @GetMapping("/leaves")
    public String leaves(Model model) {
        model.addAttribute("list", leaveRecordMapper.selectList(new LambdaQueryWrapper<LeaveRecord>()
                .orderByDesc(LeaveRecord::getCreateTime)));
        model.addAttribute("menu", "leave");
        return "back/leave";
    }

    @PostMapping("/leaves/audit")
    public String leaveAudit(@RequestParam Long id,
                             @RequestParam Integer status,
                             @RequestParam(required = false) String auditRemark) {
        LeaveRecord record = leaveRecordMapper.selectById(id);
        if (record != null) {
            record.setStatus(status);
            record.setAuditRemark(auditRemark);
            leaveRecordMapper.updateById(record);
            dormBizService.clearCache();
        }
        return "redirect:/admin/leaves";
    }

    // -------- 访客 --------
    @GetMapping("/visitors")
    public String visitors(Model model) {
        model.addAttribute("list", visitorRecordMapper.selectList(new LambdaQueryWrapper<VisitorRecord>()
                .orderByDesc(VisitorRecord::getCreateTime)));
        model.addAttribute("students", studentMapper.selectList(null));
        model.addAttribute("menu", "visitor");
        return "back/visitor";
    }

    @PostMapping("/visitors/save")
    public String visitorSave(VisitorRecord record) {
        if (record.getVisitTime() == null) {
            record.setVisitTime(LocalDateTime.now());
        }
        if (record.getStatus() == null) {
            record.setStatus(0);
        }
        if (record.getId() == null) {
            visitorRecordMapper.insert(record);
        } else {
            visitorRecordMapper.updateById(record);
        }
        return "redirect:/admin/visitors";
    }

    @PostMapping("/visitors/leave/{id}")
    public String visitorLeave(@PathVariable Long id) {
        VisitorRecord record = visitorRecordMapper.selectById(id);
        if (record != null) {
            record.setStatus(1);
            record.setLeaveTime(LocalDateTime.now());
            visitorRecordMapper.updateById(record);
        }
        return "redirect:/admin/visitors";
    }

    // -------- 卫生 --------
    @GetMapping("/hygiene")
    public String hygiene(Model model) {
        model.addAttribute("list", hygieneCheckMapper.selectList(new LambdaQueryWrapper<HygieneCheck>()
                .orderByDesc(HygieneCheck::getCheckDate)));
        model.addAttribute("rooms", roomMapper.selectList(null));
        model.addAttribute("menu", "hygiene");
        return "back/hygiene";
    }

    @PostMapping("/hygiene/save")
    public String hygieneSave(HygieneCheck check) {
        if (check.getCheckDate() == null) {
            check.setCheckDate(LocalDate.now());
        }
        if (check.getId() == null) {
            hygieneCheckMapper.insert(check);
        } else {
            hygieneCheckMapper.updateById(check);
        }
        return "redirect:/admin/hygiene";
    }

    // -------- 设置 --------
    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("configs", dormBizService.configMap());
        model.addAttribute("menu", "settings");
        return "back/settings";
    }

    @PostMapping("/settings/save")
    public String settingsSave(@RequestParam String siteName,
                               @RequestParam String curfewTime,
                               @RequestParam String hotline,
                               @RequestParam String allowRegister) {
        saveConfig("site_name", siteName);
        saveConfig("curfew_time", curfewTime);
        saveConfig("hotline", hotline);
        saveConfig("allow_register", allowRegister);
        dormBizService.clearCache();
        return "redirect:/admin/settings";
    }

    private void saveConfig(String key, String value) {
        SysConfig cfg = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
        if (cfg == null) {
            cfg = new SysConfig();
            cfg.setConfigKey(key);
            cfg.setConfigValue(value);
            sysConfigMapper.insert(cfg);
        } else {
            cfg.setConfigValue(value);
            sysConfigMapper.updateById(cfg);
        }
    }
}
