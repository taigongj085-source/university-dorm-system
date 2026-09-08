package com.campus.dorm.security;

import cn.hutool.crypto.digest.DigestUtil;
import org.springframework.security.crypto.password.PasswordEncoder;

public class Md5PasswordEncoder implements PasswordEncoder {
    @Override
    public String encode(CharSequence rawPassword) {
        return DigestUtil.md5Hex(rawPassword == null ? "" : rawPassword.toString());
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null) {
            return false;
        }
        return encode(rawPassword).equalsIgnoreCase(encodedPassword);
    }
}
