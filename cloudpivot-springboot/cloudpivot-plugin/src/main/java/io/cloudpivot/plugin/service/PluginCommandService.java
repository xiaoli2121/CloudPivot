package io.cloudpivot.plugin.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import io.cloudpivot.common.api.BadRequestException;
import io.cloudpivot.common.api.NotFoundException;
import io.cloudpivot.plugin.persistence.entity.PluginRegistryEntity;
import io.cloudpivot.plugin.persistence.mapper.PluginRegistryMapper;

@Service
public class PluginCommandService {

    private final PluginRegistryMapper pluginRegistryMapper;

    public PluginCommandService(PluginRegistryMapper pluginRegistryMapper) {
        this.pluginRegistryMapper = pluginRegistryMapper;
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> request, long actorUserId) {
        String pluginCode = requiredString(request, "pluginCode");
        assertUniquePluginCode(pluginCode);
        PluginRegistryEntity entity = new PluginRegistryEntity();
        applyCreateAudit(entity, actorUserId);
        fillPlugin(entity, request, true);
        pluginRegistryMapper.insert(entity);
        return pluginResponse(entity);
    }

    @Transactional
    public Map<String, Object> update(Long id, Map<String, Object> request, long actorUserId) {
        PluginRegistryEntity entity = requiredPlugin(id);
        fillPlugin(entity, request, false);
        LocalDateTime now = LocalDateTime.now();
        pluginRegistryMapper.update(
                null,
                new LambdaUpdateWrapper<PluginRegistryEntity>()
                        .eq(PluginRegistryEntity::getId, id)
                        .set(PluginRegistryEntity::getPluginName, entity.getPluginName())
                        .set(PluginRegistryEntity::getPluginType, entity.getPluginType())
                        .set(PluginRegistryEntity::getPluginVersion, entity.getPluginVersion())
                        .set(PluginRegistryEntity::getEntryPoint, entity.getEntryPoint())
                        .set(PluginRegistryEntity::getDescription, entity.getDescription())
                        .set(PluginRegistryEntity::getUpdatedBy, actorUserId)
                        .set(PluginRegistryEntity::getUpdatedTime, now));
        entity.setUpdatedBy(actorUserId);
        entity.setUpdatedTime(now);
        return pluginResponse(entity);
    }

    @Transactional
    public Map<String, Object> updateStatus(Long id, String statusCode, long actorUserId) {
        PluginRegistryEntity entity = requiredPlugin(id);
        LocalDateTime now = LocalDateTime.now();
        pluginRegistryMapper.update(
                null,
                new LambdaUpdateWrapper<PluginRegistryEntity>()
                        .eq(PluginRegistryEntity::getId, id)
                        .set(PluginRegistryEntity::getStatusCode, statusCode)
                        .set(PluginRegistryEntity::getUpdatedBy, actorUserId)
                        .set(PluginRegistryEntity::getUpdatedTime, now));
        entity.setStatusCode(statusCode);
        entity.setUpdatedBy(actorUserId);
        entity.setUpdatedTime(now);
        return pluginResponse(entity);
    }

    @Transactional
    public void delete(Long id, long actorUserId) {
        requiredPlugin(id);
        pluginRegistryMapper.update(
                null,
                new LambdaUpdateWrapper<PluginRegistryEntity>()
                        .eq(PluginRegistryEntity::getId, id)
                        .set(PluginRegistryEntity::getDeletedFlag, 1)
                        .set(PluginRegistryEntity::getUpdatedBy, actorUserId)
                        .set(PluginRegistryEntity::getUpdatedTime, LocalDateTime.now()));
    }

    private PluginRegistryEntity requiredPlugin(Long id) {
        PluginRegistryEntity entity = pluginRegistryMapper.selectOne(new LambdaQueryWrapper<PluginRegistryEntity>()
                .eq(PluginRegistryEntity::getDeletedFlag, 0)
                .eq(PluginRegistryEntity::getId, id));
        if (entity == null) {
            throw new NotFoundException("Plugin not found: " + id);
        }
        return entity;
    }

    private void assertUniquePluginCode(String pluginCode) {
        PluginRegistryEntity existing = pluginRegistryMapper.selectOne(new LambdaQueryWrapper<PluginRegistryEntity>()
                .eq(PluginRegistryEntity::getDeletedFlag, 0)
                .eq(PluginRegistryEntity::getPluginCode, pluginCode));
        if (existing != null) {
            throw new BadRequestException("Plugin code already exists: " + pluginCode);
        }
    }

    private void fillPlugin(PluginRegistryEntity entity, Map<String, Object> request, boolean includeCodeAndStatus) {
        if (includeCodeAndStatus) {
            entity.setPluginCode(requiredString(request, "pluginCode"));
            entity.setStatusCode(requiredString(request, "statusCode"));
        }
        entity.setPluginName(requiredString(request, "pluginName"));
        entity.setPluginType(requiredString(request, "pluginType"));
        entity.setPluginVersion(requiredString(request, "pluginVersion"));
        entity.setEntryPoint(requiredString(request, "entryPoint"));
        entity.setDescription(requiredString(request, "description"));
    }

    public Map<String, Object> pluginResponse(PluginRegistryEntity entity) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", entity.getId());
        map.put("pluginCode", entity.getPluginCode());
        map.put("pluginName", entity.getPluginName());
        map.put("pluginType", entity.getPluginType());
        map.put("pluginVersion", entity.getPluginVersion());
        map.put("status", entity.getStatusCode());
        map.put("statusCode", entity.getStatusCode());
        map.put("entryPoint", entity.getEntryPoint());
        map.put("description", entity.getDescription());
        return map;
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
}
