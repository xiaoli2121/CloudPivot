package io.cloudpivot.system.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import io.cloudpivot.auth.security.UserPrincipal;
import io.cloudpivot.auth.service.RedisService;
import io.cloudpivot.common.api.BadRequestException;
import io.cloudpivot.common.api.NotFoundException;
import io.cloudpivot.common.api.PageResponse;
import io.cloudpivot.system.persistence.entity.SysLoginLogEntity;
import io.cloudpivot.system.persistence.entity.SystemAnnouncementEntity;
import io.cloudpivot.system.persistence.entity.SystemDictEntity;
import io.cloudpivot.system.persistence.entity.SystemDictItemEntity;
import io.cloudpivot.system.persistence.mapper.SysLoginLogMapper;
import io.cloudpivot.system.persistence.mapper.SystemAnnouncementMapper;
import io.cloudpivot.system.persistence.mapper.SystemDictItemMapper;
import io.cloudpivot.system.persistence.mapper.SystemDictMapper;

@Service
public class SystemCommandService {

    private final SystemDictMapper systemDictMapper;
    private final SystemDictItemMapper systemDictItemMapper;
    private final SystemAnnouncementMapper systemAnnouncementMapper;
    private final SysLoginLogMapper sysLoginLogMapper;
    private final RedisService redisService;

    public SystemCommandService(
            SystemDictMapper systemDictMapper,
            SystemDictItemMapper systemDictItemMapper,
            SystemAnnouncementMapper systemAnnouncementMapper,
            SysLoginLogMapper sysLoginLogMapper,
            RedisService redisService) {
        this.systemDictMapper = systemDictMapper;
        this.systemDictItemMapper = systemDictItemMapper;
        this.systemAnnouncementMapper = systemAnnouncementMapper;
        this.sysLoginLogMapper = sysLoginLogMapper;
        this.redisService = redisService;
    }

    @Transactional
    public Map<String, Object> createDictionary(Map<String, Object> request, UserPrincipal actor) {
        String dictCode = requiredString(request, "dictCode");
        assertUniqueDictCode(dictCode);
        SystemDictEntity entity = new SystemDictEntity();
        applyCreateAudit(entity, actor.userId());
        entity.setDictCode(dictCode);
        entity.setDictName(requiredString(request, "dictName"));
        systemDictMapper.insert(entity);
        evictDictionaryCache(dictCode);
        return dictionaryResponse(entity);
    }

    @Transactional
    public Map<String, Object> updateDictionary(Long dictId, Map<String, Object> request, UserPrincipal actor) {
        SystemDictEntity entity = requiredDictionary(dictId);
        String dictName = requiredString(request, "dictName");
        LocalDateTime now = LocalDateTime.now();
        systemDictMapper.update(
                null,
                new LambdaUpdateWrapper<SystemDictEntity>()
                        .eq(SystemDictEntity::getId, dictId)
                        .set(SystemDictEntity::getDictName, dictName)
                        .set(SystemDictEntity::getUpdatedBy, actor.userId())
                        .set(SystemDictEntity::getUpdatedTime, now));
        entity.setDictName(dictName);
        entity.setUpdatedBy(actor.userId());
        entity.setUpdatedTime(now);
        evictDictionaryCache(entity.getDictCode());
        return dictionaryResponse(entity);
    }

    @Transactional
    public void deleteDictionary(Long dictId, UserPrincipal actor) {
        SystemDictEntity dict = requiredDictionary(dictId);
        systemDictMapper.update(
                null,
                new LambdaUpdateWrapper<SystemDictEntity>()
                        .eq(SystemDictEntity::getId, dictId)
                        .set(SystemDictEntity::getDeletedFlag, 1)
                        .set(SystemDictEntity::getUpdatedBy, actor.userId())
                        .set(SystemDictEntity::getUpdatedTime, LocalDateTime.now()));
        evictDictionaryCache(dict.getDictCode());
    }

    public List<Map<String, Object>> dictionaryItems(Long dictId) {
        requiredDictionary(dictId);
        return systemDictItemMapper.selectList(new LambdaQueryWrapper<SystemDictItemEntity>()
                        .eq(SystemDictItemEntity::getDeletedFlag, 0)
                        .eq(SystemDictItemEntity::getDictId, dictId)
                        .orderByAsc(SystemDictItemEntity::getSortNo, SystemDictItemEntity::getId))
                .stream()
                .map(this::dictionaryItemResponse)
                .toList();
    }

    @Transactional
    public Map<String, Object> createDictionaryItem(Long dictId, Map<String, Object> request, UserPrincipal actor) {
        SystemDictEntity dict = requiredDictionary(dictId);
        SystemDictItemEntity entity = new SystemDictItemEntity();
        applyCreateAudit(entity, actor.userId());
        entity.setDictId(dictId);
        entity.setItemLabel(requiredString(request, "itemLabel"));
        entity.setItemValue(requiredString(request, "itemValue"));
        entity.setSortNo(requiredInteger(request, "sortNo"));
        systemDictItemMapper.insert(entity);
        evictDictionaryCache(dict.getDictCode());
        return dictionaryItemResponse(entity);
    }

    @Transactional
    public Map<String, Object> updateDictionaryItem(Long dictId, Long itemId, Map<String, Object> request, UserPrincipal actor) {
        SystemDictEntity dict = requiredDictionary(dictId);
        SystemDictItemEntity entity = requiredDictionaryItem(itemId);
        String itemLabel = requiredString(request, "itemLabel");
        String itemValue = requiredString(request, "itemValue");
        Integer sortNo = requiredInteger(request, "sortNo");
        LocalDateTime now = LocalDateTime.now();
        systemDictItemMapper.update(
                null,
                new LambdaUpdateWrapper<SystemDictItemEntity>()
                        .eq(SystemDictItemEntity::getId, itemId)
                        .set(SystemDictItemEntity::getItemLabel, itemLabel)
                        .set(SystemDictItemEntity::getItemValue, itemValue)
                        .set(SystemDictItemEntity::getSortNo, sortNo)
                        .set(SystemDictItemEntity::getUpdatedBy, actor.userId())
                        .set(SystemDictItemEntity::getUpdatedTime, now));
        entity.setItemLabel(itemLabel);
        entity.setItemValue(itemValue);
        entity.setSortNo(sortNo);
        entity.setUpdatedBy(actor.userId());
        entity.setUpdatedTime(now);
        evictDictionaryCache(dict.getDictCode());
        return dictionaryItemResponse(entity);
    }

    @Transactional
    public void deleteDictionaryItem(Long dictId, Long itemId, UserPrincipal actor) {
        SystemDictEntity dict = requiredDictionary(dictId);
        requiredDictionaryItem(itemId);
        systemDictItemMapper.update(
                null,
                new LambdaUpdateWrapper<SystemDictItemEntity>()
                        .eq(SystemDictItemEntity::getId, itemId)
                        .set(SystemDictItemEntity::getDeletedFlag, 1)
                        .set(SystemDictItemEntity::getUpdatedBy, actor.userId())
                        .set(SystemDictItemEntity::getUpdatedTime, LocalDateTime.now()));
        evictDictionaryCache(dict.getDictCode());
    }

    @Transactional
    public Map<String, Object> createAnnouncement(Map<String, Object> request, UserPrincipal actor) {
        SystemAnnouncementEntity entity = new SystemAnnouncementEntity();
        applyCreateAudit(entity, actor.userId());
        fillAnnouncement(entity, request);
        systemAnnouncementMapper.insert(entity);
        return announcementResponse(entity);
    }

    @Transactional
    public Map<String, Object> updateAnnouncement(Long id, Map<String, Object> request, UserPrincipal actor) {
        SystemAnnouncementEntity entity = requiredAnnouncement(id);
        fillAnnouncement(entity, request);
        LocalDateTime now = LocalDateTime.now();
        systemAnnouncementMapper.update(
                null,
                new LambdaUpdateWrapper<SystemAnnouncementEntity>()
                        .eq(SystemAnnouncementEntity::getId, id)
                        .set(SystemAnnouncementEntity::getTitle, entity.getTitle())
                        .set(SystemAnnouncementEntity::getLevelCode, entity.getLevelCode())
                        .set(SystemAnnouncementEntity::getPublisherName, entity.getPublisherName())
                        .set(SystemAnnouncementEntity::getPublishTime, entity.getPublishTime())
                        .set(SystemAnnouncementEntity::getUpdatedBy, actor.userId())
                        .set(SystemAnnouncementEntity::getUpdatedTime, now));
        entity.setUpdatedBy(actor.userId());
        entity.setUpdatedTime(now);
        return announcementResponse(entity);
    }

    @Transactional
    public void deleteAnnouncement(Long id, UserPrincipal actor) {
        requiredAnnouncement(id);
        systemAnnouncementMapper.update(
                null,
                new LambdaUpdateWrapper<SystemAnnouncementEntity>()
                        .eq(SystemAnnouncementEntity::getId, id)
                        .set(SystemAnnouncementEntity::getDeletedFlag, 1)
                        .set(SystemAnnouncementEntity::getUpdatedBy, actor.userId())
                        .set(SystemAnnouncementEntity::getUpdatedTime, LocalDateTime.now()));
    }

    public PageResponse<Map<String, Object>> loginLogs() {
        List<Map<String, Object>> records = sysLoginLogMapper.selectList(new LambdaQueryWrapper<SysLoginLogEntity>()
                        .orderByDesc(SysLoginLogEntity::getEventTime, SysLoginLogEntity::getId))
                .stream()
                .map(log -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", log.getId());
                    map.put("userId", log.getUserId());
                    map.put("loginName", log.getLoginName());
                    map.put("actionCode", log.getActionCode());
                    map.put("resultCode", log.getResultCode());
                    map.put("eventTime", log.getEventTime());
                    return map;
                })
                .toList();
        return new PageResponse<>(records, records.size());
    }

    private void fillAnnouncement(SystemAnnouncementEntity entity, Map<String, Object> request) {
        entity.setTitle(requiredString(request, "title"));
        entity.setLevelCode(requiredString(request, "levelCode"));
        entity.setPublisherName(requiredString(request, "publisherName"));
        entity.setPublishTime(requiredString(request, "publishTime"));
    }

    private SystemDictEntity requiredDictionary(Long dictId) {
        SystemDictEntity dict = systemDictMapper.selectOne(new LambdaQueryWrapper<SystemDictEntity>()
                .eq(SystemDictEntity::getDeletedFlag, 0)
                .eq(SystemDictEntity::getId, dictId));
        if (dict == null) {
            throw new NotFoundException("Dictionary not found: " + dictId);
        }
        return dict;
    }

    private SystemDictItemEntity requiredDictionaryItem(Long itemId) {
        SystemDictItemEntity item = systemDictItemMapper.selectOne(new LambdaQueryWrapper<SystemDictItemEntity>()
                .eq(SystemDictItemEntity::getDeletedFlag, 0)
                .eq(SystemDictItemEntity::getId, itemId));
        if (item == null) {
            throw new NotFoundException("Dictionary item not found: " + itemId);
        }
        return item;
    }

    private SystemAnnouncementEntity requiredAnnouncement(Long id) {
        SystemAnnouncementEntity entity = systemAnnouncementMapper.selectOne(new LambdaQueryWrapper<SystemAnnouncementEntity>()
                .eq(SystemAnnouncementEntity::getDeletedFlag, 0)
                .eq(SystemAnnouncementEntity::getId, id));
        if (entity == null) {
            throw new NotFoundException("Announcement not found: " + id);
        }
        return entity;
    }

    private void assertUniqueDictCode(String dictCode) {
        SystemDictEntity existing = systemDictMapper.selectOne(new LambdaQueryWrapper<SystemDictEntity>()
                .eq(SystemDictEntity::getDeletedFlag, 0)
                .eq(SystemDictEntity::getDictCode, dictCode));
        if (existing != null) {
            throw new BadRequestException("Dictionary code already exists: " + dictCode);
        }
    }

    private Map<String, Object> dictionaryResponse(SystemDictEntity entity) {
        return Map.of(
                "id", entity.getId(),
                "dictCode", entity.getDictCode(),
                "dictName", entity.getDictName());
    }

    private Map<String, Object> dictionaryItemResponse(SystemDictItemEntity entity) {
        return Map.of(
                "id", entity.getId(),
                "dictId", entity.getDictId(),
                "itemLabel", entity.getItemLabel(),
                "itemValue", entity.getItemValue(),
                "sortNo", entity.getSortNo());
    }

    private Map<String, Object> announcementResponse(SystemAnnouncementEntity entity) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", entity.getId());
        map.put("title", entity.getTitle());
        map.put("level", entity.getLevelCode());
        map.put("publisherName", entity.getPublisherName());
        map.put("publishTime", entity.getPublishTime());
        return map;
    }

    private void evictDictionaryCache(String dictCode) {
        redisService.delete("cp:dict:items:" + dictCode);
    }

    private void applyCreateAudit(io.cloudpivot.common.persistence.BaseEntity entity, long actorUserId) {
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedBy(actorUserId);
        entity.setCreatedTime(now);
        entity.setUpdatedBy(actorUserId);
        entity.setUpdatedTime(now);
        entity.setDeletedFlag(0);
        entity.setVersionNo(0L);
    }

    private String requiredString(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new BadRequestException("Field is required: " + key);
        }
        return String.valueOf(value);
    }

    private Integer requiredInteger(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value == null) {
            throw new BadRequestException("Field is required: " + key);
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
