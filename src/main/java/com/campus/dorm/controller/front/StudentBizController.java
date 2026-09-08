package com.campus.dorm.controller.front;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.dorm.common.BusinessException;
import com.campus.dorm.entity.HygieneCheck;
import com.campus.dorm.entity.LeaveRecord;
import com.campus.dorm.entity.RepairOrder;
import com.campus.dorm.entity.Student;
import com.campus.dorm.entity.SysUser;
import com.campus.dorm.entity.VisitorRecord;
import com.campus.dorm.mapper.HygieneCheckMapper;
import com.campus.dorm.mapper.LeaveRecordMapper;
import com.campus.dorm.mapper.RepairOrderMapper;
import com.campus.dorm.mapper.VisitorRecordMapper;
import com.campus.dorm.service.MyDormService;
import com.campus.dorm.service.UserService;
import com.campus.dorm.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class StudentBizController {

    private final MyDormService myDormService;
    private final UserService userService;
    private final RepairOrderMapper repairOrderMapper;
    private final LeaveRecordMapper leaveRecordMapper;
    private final VisitorRecordMapper visitorRecordMapper;
    private final HygieneCheckMapper hygieneCheckMapper;

    @GetMapping("/my-dorm")
    public String myDorm(Model model) {
        SysUser user = SecurityUtil.getCurrentUser();
        model.addAttribute("dorm", myDormService.loadMyDorm(user.getId()));
        model.addAttribute("navActive", "dorm");
        return "front/my-dorm";
    }

    @GetMapping("/repair")
    public String repairPage(Model model) {
        Student student = myDormService.requireStudent(SecurityUtil.getCurrentUserId());
        List<RepairOrder> list = repairOrderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getStudentId, student.getId())
                .orderByDesc(RepairOrder::getCreateTime));
        model.addAttribute("orders", list);
        model.addAttribute("navActive", "repair");
        return "front/repair";
    }

    @PostMapping("/repair/submit")
    public String repairSubmit(@RequestParam String category,
                               @RequestParam String title,
                               @RequestParam String description,
                               RedirectAttributes ra) {
        try {
            Student student = myDormService.requireStudent(SecurityUtil.getCurrentUserId());
            RepairOrder order = new RepairOrder();
            order.setStudentId(student.getId());
            order.setRoomId(myDormService.resolveRoomId(SecurityUtil.getCurrentUserId()));
            order.setCategory(category);
            order.setTitle(title);
            order.setDescription(description);
            order.setStatus(0);
            repairOrderMapper.insert(order);
            ra.addFlashAttribute("okMsg", "报修已提交");
        } catch (BusinessException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/repair";
    }

    @GetMapping("/leave")
    public String leavePage(Model model) {
        Student student = myDormService.requireStudent(SecurityUtil.getCurrentUserId());
        model.addAttribute("records", leaveRecordMapper.selectList(new LambdaQueryWrapper<LeaveRecord>()
                .eq(LeaveRecord::getStudentId, student.getId())
                .orderByDesc(LeaveRecord::getCreateTime)));
        model.addAttribute("navActive", "leave");
        return "front/leave";
    }

    @PostMapping("/leave/submit")
    public String leaveSubmit(@RequestParam Integer leaveType,
                              @RequestParam String reason,
                              @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime startTime,
                              @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime endTime,
                              RedirectAttributes ra) {
        try {
            Student student = myDormService.requireStudent(SecurityUtil.getCurrentUserId());
            LeaveRecord record = new LeaveRecord();
            record.setStudentId(student.getId());
            record.setLeaveType(leaveType);
            record.setReason(reason);
            record.setStartTime(startTime);
            record.setEndTime(endTime);
            record.setStatus(0);
            leaveRecordMapper.insert(record);
            ra.addFlashAttribute("okMsg", "申请已提交，等待审核");
        } catch (BusinessException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/leave";
    }

    @GetMapping("/visitor")
    public String visitorPage(Model model) {
        Student student = myDormService.requireStudent(SecurityUtil.getCurrentUserId());
        model.addAttribute("records", visitorRecordMapper.selectList(new LambdaQueryWrapper<VisitorRecord>()
                .eq(VisitorRecord::getStudentId, student.getId())
                .orderByDesc(VisitorRecord::getCreateTime)));
        model.addAttribute("navActive", "visitor");
        return "front/visitor";
    }

    @PostMapping("/visitor/submit")
    public String visitorSubmit(@RequestParam String visitorName,
                                @RequestParam(required = false) String visitorPhone,
                                @RequestParam(required = false) String idCardTail,
                                @RequestParam String reason,
                                @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime visitTime,
                                RedirectAttributes ra) {
        try {
            Student student = myDormService.requireStudent(SecurityUtil.getCurrentUserId());
            VisitorRecord record = new VisitorRecord();
            record.setStudentId(student.getId());
            record.setVisitorName(visitorName);
            record.setVisitorPhone(visitorPhone);
            record.setIdCardTail(idCardTail);
            record.setReason(reason);
            record.setVisitTime(visitTime == null ? LocalDateTime.now() : visitTime);
            record.setStatus(0);
            visitorRecordMapper.insert(record);
            ra.addFlashAttribute("okMsg", "访客已登记");
        } catch (BusinessException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/visitor";
    }

    @GetMapping("/hygiene")
    public String hygiene(Model model) {
        Long roomId = myDormService.resolveRoomId(SecurityUtil.getCurrentUserId());
        List<HygieneCheck> list = roomId == null ? List.of() :
                hygieneCheckMapper.selectList(new LambdaQueryWrapper<HygieneCheck>()
                        .eq(HygieneCheck::getRoomId, roomId)
                        .orderByDesc(HygieneCheck::getCheckDate));
        model.addAttribute("records", list);
        model.addAttribute("navActive", "hygiene");
        return "front/hygiene";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("user", SecurityUtil.getCurrentUser());
        model.addAttribute("student", userService.findStudentByUserId(SecurityUtil.getCurrentUserId()));
        model.addAttribute("navActive", "profile");
        return "front/profile";
    }

    @PostMapping("/profile/save")
    public String profileSave(@RequestParam(required = false) String realName,
                              @RequestParam(required = false) String phone,
                              @RequestParam(required = false) String email,
                              @RequestParam(required = false) String newPassword,
                              RedirectAttributes ra) {
        try {
            userService.updateProfile(SecurityUtil.getCurrentUserId(), realName, phone, email, newPassword);
            ra.addFlashAttribute("okMsg", "保存成功");
        } catch (BusinessException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/profile";
    }
}
