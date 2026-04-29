package io.cloudpivot.metadata.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import io.cloudpivot.common.api.BadRequestException;
import io.cloudpivot.common.api.NotFoundException;
import io.cloudpivot.metadata.persistence.entity.MetaAppEntity;
import io.cloudpivot.metadata.persistence.entity.MetaObjectEntity;
import io.cloudpivot.metadata.persistence.entity.MetaObjectFieldEntity;
import io.cloudpivot.metadata.persistence.entity.MetaPublishVersionEntity;
import io.cloudpivot.metadata.persistence.mapper.MetaAppMapper;
import io.cloudpivot.metadata.persistence.mapper.MetaObjectFieldMapper;
import io.cloudpivot.metadata.persistence.mapper.MetaObjectMapper;
import io.cloudpivot.metadata.persistence.mapper.MetaPublishVersionMapper;

@Service
public class MetadataCommandService {

    private final MetaAppMapper metaAppMapper;
    private final MetaObjectMapper metaObjectMapper;
    private final MetaObjectFieldMapper metaObjectFieldMapper;
    private final MetaPublishVersionMapper metaPublishVersionMapper;

    public MetadataCommandService(
            MetaAppMapper metaAppMapper,
            MetaObjectMapper metaObjectMapper,
            MetaObjectFieldMapper metaObjectFieldMapper,
            MetaPublishVersionMapper metaPublishVersionMapper) {
        this.metaAppMapper = metaAppMapper;
        this.metaObjectMapper = metaObjectMapper;
        this.metaObjectFieldMapper = metaObjectFieldMapper;
        this.metaPublishVersionMapper = metaPublishVersionMapper;
    }

    @Transactional
    public Map<String, Object> createApp(Map<String, Object> request, long actorUserId) {
        String appCode = requiredString(request, "appCode");
        assertUniqueAppCode(appCode);
        MetaAppEntity entity = new MetaAppEntity();
        applyCreateAudit(entity, actorUserId);
        entity.setAppCode(appCode);
        entity.setAppName(requiredString(request, "appName"));
        entity.setOwnerName(requiredString(request, "ownerName"));
        entity.setAppStatus(requiredString(request, "appStatus"));
        metaAppMapper.insert(entity);
        return appResponse(entity);
    }

    public Map<String, Object> appDetail(String appCode) {
        return appResponse(requiredApp(appCode));
    }

    @Transactional
    public Map<String, Object> updateApp(String appCode, Map<String, Object> request, long actorUserId) {
        MetaAppEntity entity = requiredApp(appCode);
        String appName = requiredString(request, "appName");
        String ownerName = requiredString(request, "ownerName");
        String appStatus = requiredString(request, "appStatus");
        LocalDateTime now = LocalDateTime.now();
        metaAppMapper.update(
                null,
                new LambdaUpdateWrapper<MetaAppEntity>()
                        .eq(MetaAppEntity::getId, entity.getId())
                        .set(MetaAppEntity::getAppName, appName)
                        .set(MetaAppEntity::getOwnerName, ownerName)
                        .set(MetaAppEntity::getAppStatus, appStatus)
                        .set(MetaAppEntity::getUpdatedBy, actorUserId)
                        .set(MetaAppEntity::getUpdatedTime, now));
        entity.setAppName(appName);
        entity.setOwnerName(ownerName);
        entity.setAppStatus(appStatus);
        entity.setUpdatedBy(actorUserId);
        entity.setUpdatedTime(now);
        return appResponse(entity);
    }

    @Transactional
    public void deleteApp(String appCode, long actorUserId) {
        MetaAppEntity entity = requiredApp(appCode);
        metaAppMapper.update(
                null,
                new LambdaUpdateWrapper<MetaAppEntity>()
                        .eq(MetaAppEntity::getId, entity.getId())
                        .set(MetaAppEntity::getDeletedFlag, 1)
                        .set(MetaAppEntity::getUpdatedBy, actorUserId)
                        .set(MetaAppEntity::getUpdatedTime, LocalDateTime.now()));
    }

    public List<Map<String, Object>> objectsByApp(String appCode) {
        MetaAppEntity app = requiredApp(appCode);
        return metaObjectMapper.selectList(new LambdaQueryWrapper<MetaObjectEntity>()
                        .eq(MetaObjectEntity::getDeletedFlag, 0)
                        .eq(MetaObjectEntity::getAppId, app.getId())
                        .orderByAsc(MetaObjectEntity::getId))
                .stream()
                .map(this::objectResponse)
                .toList();
    }

    public Map<String, Object> objectDetail(Long objectId) {
        MetaObjectEntity object = requiredObject(objectId);
        Map<String, Object> response = objectResponse(object);
        List<Map<String, Object>> fields = metaObjectFieldMapper.selectList(new LambdaQueryWrapper<MetaObjectFieldEntity>()
                        .eq(MetaObjectFieldEntity::getDeletedFlag, 0)
                        .eq(MetaObjectFieldEntity::getObjectId, objectId)
                        .orderByAsc(MetaObjectFieldEntity::getSortNo, MetaObjectFieldEntity::getId))
                .stream()
                .map(this::fieldResponse)
                .toList();
        response.put("fields", fields);
        return response;
    }

    @Transactional
    public Map<String, Object> createObject(String appCode, Map<String, Object> request, long actorUserId) {
        MetaAppEntity app = requiredApp(appCode);
        MetaObjectEntity entity = new MetaObjectEntity();
        applyCreateAudit(entity, actorUserId);
        entity.setAppId(app.getId());
        entity.setObjectCode(requiredString(request, "objectCode"));
        entity.setObjectName(requiredString(request, "objectName"));
        entity.setStoreType(requiredString(request, "storeType"));
        entity.setPrimaryFieldCode(requiredString(request, "primaryFieldCode"));
        entity.setStatusCode(requiredString(request, "statusCode"));
        metaObjectMapper.insert(entity);
        return objectResponse(entity);
    }

    @Transactional
    public Map<String, Object> updateObject(Long objectId, Map<String, Object> request, long actorUserId) {
        MetaObjectEntity entity = requiredObject(objectId);
        String objectName = requiredString(request, "objectName");
        String storeType = requiredString(request, "storeType");
        String primaryFieldCode = requiredString(request, "primaryFieldCode");
        String statusCode = requiredString(request, "statusCode");
        LocalDateTime now = LocalDateTime.now();
        metaObjectMapper.update(
                null,
                new LambdaUpdateWrapper<MetaObjectEntity>()
                        .eq(MetaObjectEntity::getId, objectId)
                        .set(MetaObjectEntity::getObjectName, objectName)
                        .set(MetaObjectEntity::getStoreType, storeType)
                        .set(MetaObjectEntity::getPrimaryFieldCode, primaryFieldCode)
                        .set(MetaObjectEntity::getStatusCode, statusCode)
                        .set(MetaObjectEntity::getUpdatedBy, actorUserId)
                        .set(MetaObjectEntity::getUpdatedTime, now));
        entity.setObjectName(objectName);
        entity.setStoreType(storeType);
        entity.setPrimaryFieldCode(primaryFieldCode);
        entity.setStatusCode(statusCode);
        entity.setUpdatedBy(actorUserId);
        entity.setUpdatedTime(now);
        return objectResponse(entity);
    }

    @Transactional
    public void deleteObject(Long objectId, long actorUserId) {
        requiredObject(objectId);
        metaObjectMapper.update(
                null,
                new LambdaUpdateWrapper<MetaObjectEntity>()
                        .eq(MetaObjectEntity::getId, objectId)
                        .set(MetaObjectEntity::getDeletedFlag, 1)
                        .set(MetaObjectEntity::getUpdatedBy, actorUserId)
                        .set(MetaObjectEntity::getUpdatedTime, LocalDateTime.now()));
    }

    @Transactional
    public Map<String, Object> createField(Long objectId, Map<String, Object> request, long actorUserId) {
        requiredObject(objectId);
        MetaObjectFieldEntity entity = new MetaObjectFieldEntity();
        applyCreateAudit(entity, actorUserId);
        entity.setObjectId(objectId);
        entity.setFieldCode(requiredString(request, "fieldCode"));
        entity.setFieldName(requiredString(request, "fieldName"));
        entity.setFieldType(requiredString(request, "fieldType"));
        entity.setRequiredFlag(booleanValue(request.get("requiredFlag")) ? 1 : 0);
        entity.setSortNo(requiredInteger(request, "sortNo"));
        metaObjectFieldMapper.insert(entity);
        return fieldResponse(entity);
    }

    @Transactional
    public Map<String, Object> updateField(Long fieldId, Map<String, Object> request, long actorUserId) {
        MetaObjectFieldEntity entity = requiredField(fieldId);
        String fieldName = requiredString(request, "fieldName");
        String fieldType = requiredString(request, "fieldType");
        Integer requiredFlag = booleanValue(request.get("requiredFlag")) ? 1 : 0;
        Integer sortNo = requiredInteger(request, "sortNo");
        LocalDateTime now = LocalDateTime.now();
        metaObjectFieldMapper.update(
                null,
                new LambdaUpdateWrapper<MetaObjectFieldEntity>()
                        .eq(MetaObjectFieldEntity::getId, fieldId)
                        .set(MetaObjectFieldEntity::getFieldName, fieldName)
                        .set(MetaObjectFieldEntity::getFieldType, fieldType)
                        .set(MetaObjectFieldEntity::getRequiredFlag, requiredFlag)
                        .set(MetaObjectFieldEntity::getSortNo, sortNo)
                        .set(MetaObjectFieldEntity::getUpdatedBy, actorUserId)
                        .set(MetaObjectFieldEntity::getUpdatedTime, now));
        entity.setFieldName(fieldName);
        entity.setFieldType(fieldType);
        entity.setRequiredFlag(requiredFlag);
        entity.setSortNo(sortNo);
        entity.setUpdatedBy(actorUserId);
        entity.setUpdatedTime(now);
        return fieldResponse(entity);
    }

    @Transactional
    public void deleteField(Long fieldId, long actorUserId) {
        requiredField(fieldId);
        metaObjectFieldMapper.update(
                null,
                new LambdaUpdateWrapper<MetaObjectFieldEntity>()
                        .eq(MetaObjectFieldEntity::getId, fieldId)
                        .set(MetaObjectFieldEntity::getDeletedFlag, 1)
                        .set(MetaObjectFieldEntity::getUpdatedBy, actorUserId)
                        .set(MetaObjectFieldEntity::getUpdatedTime, LocalDateTime.now()));
    }

    public List<Map<String, Object>> publishVersions(String appCode) {
        MetaAppEntity app = requiredApp(appCode);
        return metaPublishVersionMapper.selectList(new LambdaQueryWrapper<MetaPublishVersionEntity>()
                        .eq(MetaPublishVersionEntity::getDeletedFlag, 0)
                        .eq(MetaPublishVersionEntity::getAppId, app.getId())
                        .orderByDesc(MetaPublishVersionEntity::getPublishedTime, MetaPublishVersionEntity::getId))
                .stream()
                .map(version -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", version.getId());
                    map.put("versionCode", version.getVersionCode());
                    map.put("versionStatus", version.getVersionStatus());
                    map.put("snapshotSummary", version.getSnapshotSummary());
                    map.put("publishedTime", version.getPublishedTime());
                    return map;
                })
                .toList();
    }

    private MetaAppEntity requiredApp(String appCode) {
        MetaAppEntity app = metaAppMapper.selectOne(new LambdaQueryWrapper<MetaAppEntity>()
                .eq(MetaAppEntity::getDeletedFlag, 0)
                .eq(MetaAppEntity::getAppCode, appCode));
        if (app == null) {
            throw new NotFoundException("Application not found: " + appCode);
        }
        return app;
    }

    private MetaObjectEntity requiredObject(Long objectId) {
        MetaObjectEntity object = metaObjectMapper.selectOne(new LambdaQueryWrapper<MetaObjectEntity>()
                .eq(MetaObjectEntity::getDeletedFlag, 0)
                .eq(MetaObjectEntity::getId, objectId));
        if (object == null) {
            throw new NotFoundException("Object not found: " + objectId);
        }
        return object;
    }

    private MetaObjectFieldEntity requiredField(Long fieldId) {
        MetaObjectFieldEntity field = metaObjectFieldMapper.selectOne(new LambdaQueryWrapper<MetaObjectFieldEntity>()
                .eq(MetaObjectFieldEntity::getDeletedFlag, 0)
                .eq(MetaObjectFieldEntity::getId, fieldId));
        if (field == null) {
            throw new NotFoundException("Field not found: " + fieldId);
        }
        return field;
    }

    private void assertUniqueAppCode(String appCode) {
        MetaAppEntity existing = metaAppMapper.selectOne(new LambdaQueryWrapper<MetaAppEntity>()
                .eq(MetaAppEntity::getDeletedFlag, 0)
                .eq(MetaAppEntity::getAppCode, appCode));
        if (existing != null) {
            throw new BadRequestException("Application code already exists: " + appCode);
        }
    }

    private Map<String, Object> appResponse(MetaAppEntity entity) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", entity.getId());
        map.put("appCode", entity.getAppCode());
        map.put("appName", entity.getAppName());
        map.put("owner", entity.getOwnerName());
        map.put("ownerName", entity.getOwnerName());
        map.put("status", entity.getAppStatus());
        map.put("appStatus", entity.getAppStatus());
        return map;
    }

    private Map<String, Object> objectResponse(MetaObjectEntity entity) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", entity.getId());
        map.put("appId", entity.getAppId());
        map.put("objectCode", entity.getObjectCode());
        map.put("objectName", entity.getObjectName());
        map.put("storeType", entity.getStoreType());
        map.put("primaryFieldCode", entity.getPrimaryFieldCode());
        map.put("statusCode", entity.getStatusCode());
        return map;
    }

    private Map<String, Object> fieldResponse(MetaObjectFieldEntity entity) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", entity.getId());
        map.put("objectId", entity.getObjectId());
        map.put("fieldCode", entity.getFieldCode());
        map.put("fieldName", entity.getFieldName());
        map.put("fieldType", entity.getFieldType());
        map.put("requiredFlag", entity.getRequiredFlag() != null && entity.getRequiredFlag() == 1);
        map.put("sortNo", entity.getSortNo());
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

    private Integer requiredInteger(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value == null) {
            throw new BadRequestException("Field is required: " + key);
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private boolean booleanValue(Object value) {
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }
}
