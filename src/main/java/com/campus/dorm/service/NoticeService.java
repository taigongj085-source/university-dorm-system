package com.campus.dorm.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.dorm.common.BusinessException;
import com.campus.dorm.entity.Notice;
import com.campus.dorm.mapper.NoticeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeMapper noticeMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final StatsCacheService statsCacheService;

    @Value("${dorm.redis.notices-key:dorm:home:notices}")
    private String noticesKey;

    @Value("${dorm.redis.cache-ttl-seconds:60}")
    private long ttlSeconds;

    public List<Notice> listHomeNotices(int limit) {
        try {
            String cached = stringRedisTemplate.opsForValue().get(noticesKey);
            if (StrUtil.isNotBlank(cached)) {
                List<Long> ids = Arrays.stream(cached.split(","))
                        .map(String::trim)
                        .filter(StrUtil::isNotBlank)
                        .map(Long::valueOf)
                        .collect(Collectors.toList());
                if (!ids.isEmpty()) {
                    List<Notice> list = noticeMapper.selectByIds(ids);
                    var map = list.stream().collect(Collectors.toMap(Notice::getId, n -> n, (a, b) -> a));
                    List<Notice> ordered = ids.stream().map(map::get).filter(Objects::nonNull).collect(Collectors.toList());
                    if (!ordered.isEmpty()) {
                        return ordered.size() > limit ? ordered.subList(0, limit) : ordered;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("读取 Redis 公告缓存失败，回退数据库: {}", e.getMessage());
        }
        List<Notice> list = noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, 1)
                .orderByDesc(Notice::getIsTop)
                .orderByDesc(Notice::getCreateTime)
                .last("LIMIT " + limit));
        try {
            String ids = list.stream().map(n -> String.valueOf(n.getId())).collect(Collectors.joining(","));
            if (StrUtil.isNotBlank(ids)) {
                stringRedisTemplate.opsForValue().set(noticesKey, ids, ttlSeconds, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("写入 Redis 公告缓存失败: {}", e.getMessage());
        }
        return list;
    }

    public Page<Notice> pagePublished(String q, String category, long page, long size) {
        LambdaQueryWrapper<Notice> qw = new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, 1);
        if (StrUtil.isNotBlank(q)) {
            qw.and(w -> w.like(Notice::getTitle, q).or().like(Notice::getContent, q));
        }
        if (StrUtil.isNotBlank(category)) {
            qw.eq(Notice::getCategory, category);
        }
        qw.orderByDesc(Notice::getIsTop).orderByDesc(Notice::getCreateTime);
        return noticeMapper.selectPage(new Page<>(page, size), qw);
    }

    public Page<Notice> adminPage(String q, long page, long size) {
        LambdaQueryWrapper<Notice> qw = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(q)) {
            qw.like(Notice::getTitle, q);
        }
        qw.orderByDesc(Notice::getIsTop).orderByDesc(Notice::getId);
        return noticeMapper.selectPage(new Page<>(page, size), qw);
    }

    public Notice getPublished(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null || notice.getStatus() == null || notice.getStatus() != 1) {
            throw new BusinessException("公告不存在或未发布");
        }
        return notice;
    }

    public Notice getById(Long id) {
        return noticeMapper.selectById(id);
    }

    public void save(Notice form) {
        if (StrUtil.isBlank(form.getTitle()) || StrUtil.isBlank(form.getContent())) {
            throw new BusinessException("标题和内容不能为空");
        }
        if (form.getStatus() == null) {
            form.setStatus(1);
        }
        if (form.getIsTop() == null) {
            form.setIsTop(0);
        }
        if (StrUtil.isBlank(form.getPublisher())) {
            form.setPublisher("宿管中心");
        }
        if (form.getId() == null) {
            noticeMapper.insert(form);
        } else {
            noticeMapper.updateById(form);
        }
        clearNoticeCache();
    }

    public void delete(Long id) {
        noticeMapper.deleteById(id);
        clearNoticeCache();
    }

    private void clearNoticeCache() {
        try {
            stringRedisTemplate.delete(noticesKey);
        } catch (Exception e) {
            log.warn("清理公告缓存失败: {}", e.getMessage());
        }
        statsCacheService.clearCache();
    }
}
