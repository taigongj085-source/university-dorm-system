package com.campus.dorm.controller.front;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.dorm.entity.Notice;
import com.campus.dorm.mapper.NoticeMapper;
import com.campus.dorm.service.NoticeService;
import com.campus.dorm.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final NoticeService noticeService;
    private final SysConfigService sysConfigService;
    private final NoticeMapper noticeMapper;

    @GetMapping({"/", "/index"})
    public String index(Model model) {
        List<Notice> notices = noticeService.listHomeNotices(5);
        model.addAttribute("notices", notices);
        Map<String, String> configs = new HashMap<>();
        configs.put("hotline", sysConfigService.getValue("hotline", "400-800-2026"));
        configs.put("curfew_time", sysConfigService.getValue("curfew_time", "23:00"));
        configs.put("site_name", sysConfigService.getValue("site_name", "大学宿舍管理系统"));
        model.addAttribute("configs", configs);
        model.addAttribute("navActive", "home");
        return "front/index";
    }

    @GetMapping("/guide")
    public String guide(Model model) {
        Map<String, String> configs = new HashMap<>();
        configs.put("hotline", sysConfigService.getValue("hotline", "400-800-2026"));
        configs.put("curfew_time", sysConfigService.getValue("curfew_time", "23:00"));
        model.addAttribute("configs", configs);
        model.addAttribute("navActive", "guide");
        return "front/guide";
    }

    @GetMapping("/notices")
    public String notices(@RequestParam(required = false) String category,
                          @RequestParam(required = false) String q,
                          @RequestParam(defaultValue = "1") int page,
                          Model model) {
        LambdaQueryWrapper<Notice> qw = new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, 1)
                .eq(StrUtil.isNotBlank(category), Notice::getCategory, category)
                .like(StrUtil.isNotBlank(q), Notice::getTitle, q)
                .orderByDesc(Notice::getIsTop)
                .orderByDesc(Notice::getCreateTime);
        model.addAttribute("pageData", noticeMapper.selectPage(new Page<>(page, 10), qw));
        model.addAttribute("category", category);
        model.addAttribute("q", q);
        model.addAttribute("navActive", "notices");
        return "front/notices";
    }

    @GetMapping({"/notice/{id}", "/notices/{id}"})
    public String noticeDetail(@PathVariable Long id, Model model) {
        model.addAttribute("notice", noticeMapper.selectById(id));
        model.addAttribute("navActive", "notices");
        return "front/notice-detail";
    }
}
