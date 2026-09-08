package com.campus.dorm.config;

import com.campus.dorm.entity.SysUser;
import com.campus.dorm.service.SysConfigService;
import com.campus.dorm.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final SysConfigService sysConfigService;

    @ModelAttribute("loginUser")
    public SysUser loginUser() {
        return SecurityUtil.toSafeUser(SecurityUtil.getCurrentUser());
    }

    @ModelAttribute("siteConfig")
    public Map<String, String> siteConfig() {
        return sysConfigService.asMap();
    }
}
