package com.campus.dorm.config;

import com.campus.dorm.common.BusinessException;
import com.campus.dorm.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Object handleBusiness(BusinessException e, HttpServletRequest request, RedirectAttributes ra) {
        String accept = request.getHeader("Accept");
        String xhr = request.getHeader("X-Requested-With");
        boolean json = (accept != null && accept.contains("application/json"))
                || "XMLHttpRequest".equalsIgnoreCase(xhr)
                || (request.getRequestURI() != null && request.getRequestURI().startsWith("/api/"));
        if (json) {
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(Result.fail(e.getMessage()));
        }
        ra.addFlashAttribute("errorMsg", e.getMessage());
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            referer = "/";
        }
        return new RedirectView(referer);
    }
}
