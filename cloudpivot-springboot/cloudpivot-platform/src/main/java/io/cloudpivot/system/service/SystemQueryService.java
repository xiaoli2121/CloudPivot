package io.cloudpivot.system.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cloudpivot.system.api.dto.AnnouncementSummary;
import io.cloudpivot.system.api.dto.DictionaryItem;
import io.cloudpivot.system.api.dto.DictionarySummary;
import io.cloudpivot.auth.service.RedisService;
import io.cloudpivot.system.persistence.entity.SystemAnnouncementEntity;
import io.cloudpivot.system.persistence.entity.SystemDictEntity;
import io.cloudpivot.system.persistence.entity.SystemDictItemEntity;
import io.cloudpivot.system.persistence.mapper.SystemAnnouncementMapper;
import io.cloudpivot.system.persistence.mapper.SystemDictItemMapper;
import io.cloudpivot.system.persistence.mapper.SystemDictMapper;

@Service
public class SystemQueryService {

    private final SystemDictMapper systemDictMapper;
    private final SystemDictItemMapper systemDictItemMapper;
    private final SystemAnnouncementMapper systemAnnouncementMapper;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    public SystemQueryService(
            SystemDictMapper systemDictMapper,
            SystemDictItemMapper systemDictItemMapper,
            SystemAnnouncementMapper systemAnnouncementMapper,
            RedisService redisService,
            ObjectMapper objectMapper) {
        this.systemDictMapper = systemDictMapper;
        this.systemDictItemMapper = systemDictItemMapper;
        this.systemAnnouncementMapper = systemAnnouncementMapper;
        this.redisService = redisService;
        this.objectMapper = objectMapper;
    }

    public List<DictionarySummary> dictionaries() {
        List<SystemDictEntity> dicts = systemDictMapper.selectList(new LambdaQueryWrapper<SystemDictEntity>()
                .eq(SystemDictEntity::getDeletedFlag, 0)
                .orderByAsc(SystemDictEntity::getId));

        Map<Long, String> dictCodeById = dicts.stream()
                .collect(Collectors.toMap(SystemDictEntity::getId, SystemDictEntity::getDictCode));

        Map<Long, List<DictionaryItem>> itemsByDictId = new java.util.LinkedHashMap<>();
        List<Long> missingDictIds = new java.util.ArrayList<>();
        for (SystemDictEntity dict : dicts) {
            List<DictionaryItem> cachedItems = readCachedItems(dict.getDictCode());
            itemsByDictId.put(dict.getId(), cachedItems);
            if (cachedItems == null) {
                missingDictIds.add(dict.getId());
            }
        }

        if (!missingDictIds.isEmpty()) {
            Map<Long, List<DictionaryItem>> queriedItems = systemDictItemMapper.selectList(new LambdaQueryWrapper<SystemDictItemEntity>()
                            .eq(SystemDictItemEntity::getDeletedFlag, 0)
                            .in(SystemDictItemEntity::getDictId, missingDictIds)
                            .orderByAsc(SystemDictItemEntity::getDictId, SystemDictItemEntity::getSortNo, SystemDictItemEntity::getId))
                    .stream()
                    .collect(Collectors.groupingBy(
                            SystemDictItemEntity::getDictId,
                            Collectors.mapping(
                                    item -> new DictionaryItem(item.getItemLabel(), item.getItemValue()),
                                    Collectors.toList())));

            for (Long dictId : missingDictIds) {
                List<DictionaryItem> items = queriedItems.getOrDefault(dictId, List.of());
                itemsByDictId.put(dictId, items);
                cacheItems(dictCodeById.get(dictId), items);
            }
        }

        return dicts.stream()
                .map(dict -> new DictionarySummary(
                        dict.getDictCode(),
                        dict.getDictName(),
                        itemsByDictId.getOrDefault(dict.getId(), List.of())))
                .toList();
    }

    public List<AnnouncementSummary> announcements() {
        return systemAnnouncementMapper.selectList(new LambdaQueryWrapper<SystemAnnouncementEntity>()
                        .eq(SystemAnnouncementEntity::getDeletedFlag, 0)
                        .orderByAsc(SystemAnnouncementEntity::getId))
                .stream()
                .map(announcement -> new AnnouncementSummary(
                        announcement.getId(),
                        announcement.getTitle(),
                        announcement.getLevelCode(),
                        announcement.getPublisherName(),
                        announcement.getPublishTime()))
                .toList();
    }

    private List<DictionaryItem> readCachedItems(String dictCode) {
        String json = redisService.get(dictItemsKey(dictCode));
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<DictionaryItem>>() {
            });
        } catch (JsonProcessingException exception) {
            redisService.delete(dictItemsKey(dictCode));
            return null;
        }
    }

    private void cacheItems(String dictCode, List<DictionaryItem> items) {
        try {
            redisService.set(dictItemsKey(dictCode), objectMapper.writeValueAsString(items), java.time.Duration.ofHours(1));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to cache dictionary items.", exception);
        }
    }

    private String dictItemsKey(String dictCode) {
        return "cp:dict:items:" + dictCode;
    }
}
