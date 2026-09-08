package com.campus.dorm.controller.front;

import com.campus.dorm.common.BusinessException;
import com.campus.dorm.entity.SysUser;
import com.campus.dorm.security.DormUserDetails;
import com.campus.dorm.service.SysUserService;
import com.campus.dorm.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String redirect,
                            @RequestParam(required = false) String error,
                            Model model) {
        if (SecurityUtil.getCurrentUser() != null) {
            return "redirect:" + (redirect == null || redirect.isBlank() ? "/" : redirect);
        }
        model.addAttribute("redirect", redirect == null ? "/" : redirect);
        if ("1".equals(error)) {
            model.addAttribute("errorMsg", "账号或密码错误");
        }
        return "front/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (SecurityUtil.getCurrentUser() != null) {
            return "redirect:/";
        }
        return "front/register";
    }

    @PostMapping("/doRegister")
    public String doRegister(@RequestParam String username,
                             @RequestParam String password,
                             @RequestParam(name = "confirm", required = false) String confirm,
                             @RequestParam(name = "password2", required = false) String password2,
                             @RequestParam String realName,
                             @RequestParam(required = false) String phone,
                             HttpServletRequest request,
                             RedirectAttributes ra) {
        String pwd2 = confirm != null ? confirm : password2;
        try {
            SysUser user = sysUserService.register(username, password, pwd2, realName, phone);
            DormUserDetails details = new DormUserDetails(user);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            request.getSession().setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());
            return "redirect:/";
        } catch (BusinessException e) {
            ra.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/register";
        }
    }
}
