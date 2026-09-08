package com.campus.dorm.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.dorm.entity.SysConfig;
import com.campus.dorm.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SysConfigService {

    private final SysConfigMapper sysConfigMapper;

    public List<SysConfig> listAll() {
        return sysConfigMapper.selectList(new LambdaQueryWrapper<SysConfig>()
                .orderByAsc(SysConfig::getId));
    }

    public Map<String, String> asMap() {
        Map<String, String> map = new HashMap<>();
        for (SysConfig c : listAll()) {
            map.put(c.getConfigKey(), c.getConfigValue());
        }
        return map;
    }

    public String getValue(String key, String defaultValue) {
        SysConfig cfg = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key));
        if (cfg == null || StrUtil.isBlank(cfg.getConfigValue())) {
            return defaultValue;
        }
        return cfg.getConfigValue();
    }

    public void saveBatch(Map<String, String> values) {
        if (values == null) {
            return;
        }
        for (Map.Entry<String, String> e : values.entrySet()) {
            if (StrUtil.isBlank(e.getKey())) {
                continue;
            }
            SysConfig cfg = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                    .eq(SysConfig::getConfigKey, e.getKey()));
            if (cfg == null) {
                cfg = new SysConfig();
                cfg.setConfigKey(e.getKey());
                cfg.setConfigValue(StrUtil.nullToDefault(e.getValue(), ""));
                sysConfigMapper.insert(cfg);
            } else {
                SysConfig upd = new SysConfig();
                upd.setId(cfg.getId());
                upd.setConfigValue(StrUtil.nullToDefault(e.getValue(), ""));
                sysConfigMapper.updateById(upd);
            }
        }
    }
}
