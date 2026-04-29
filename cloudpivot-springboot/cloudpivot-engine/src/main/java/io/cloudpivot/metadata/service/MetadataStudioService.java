package io.cloudpivot.metadata.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cloudpivot.metadata.api.dto.DesignerSaveRequest;
import io.cloudpivot.metadata.api.dto.DesignerSaveResponse;
import io.cloudpivot.metadata.api.dto.DesignerSchemaResponse;
import io.cloudpivot.metadata.api.dto.LowCodeView;
import io.cloudpivot.metadata.api.dto.PublishVersionRequest;
import io.cloudpivot.metadata.api.dto.PublishVersionResponse;
import io.cloudpivot.metadata.api.dto.RuntimeEntryResponse;
import io.cloudpivot.metadata.persistence.entity.MetaAppEntity;
import io.cloudpivot.metadata.persistence.entity.MetaComponentEntity;
import io.cloudpivot.metadata.persistence.entity.MetaObjectEntity;
import io.cloudpivot.metadata.persistence.entity.MetaObjectFieldEntity;
import io.cloudpivot.metadata.persistence.entity.MetaPageEntity;
import io.cloudpivot.metadata.persistence.entity.MetaPublishVersionEntity;
import io.cloudpivot.metadata.persistence.mapper.MetaAppMapper;
import io.cloudpivot.metadata.persistence.mapper.MetaComponentMapper;
import io.cloudpivot.metadata.persistence.mapper.MetaObjectFieldMapper;
import io.cloudpivot.metadata.persistence.mapper.MetaObjectMapper;
import io.cloudpivot.metadata.persistence.mapper.MetaPageMapper;
import io.cloudpivot.metadata.persistence.mapper.MetaPublishVersionMapper;

@Service
public class MetadataStudioService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final MetaAppMapper metaAppMapper;
    private final MetaObjectMapper metaObjectMapper;
    private final MetaObjectFieldMapper metaObjectFieldMapper;
    private final MetaPageMapper metaPageMapper;
    private final MetaComponentMapper metaComponentMapper;
    private final MetaPublishVersionMapper metaPublishVersionMapper;
    private final ObjectMapper objectMapper;

    public MetadataStudioService(
            MetaAppMapper metaAppMapper,
            MetaObjectMapper metaObjectMapper,
            MetaObjectFieldMapper metaObjectFieldMapper,
            MetaPageMapper metaPageMapper,
            MetaComponentMapper metaComponentMapper,
            MetaPublishVersionMapper metaPublishVersionMapper,
            ObjectMapper objectMapper) {
        this.metaAppMapper = metaAppMapper;
        this.metaObjectMapper = metaObjectMapper;
        this.metaObjectFieldMapper = metaObjectFieldMapper;
        this.metaPageMapper = metaPageMapper;
        this.metaComponentMapper = metaComponentMapper;
        this.metaPublishVersionMapper = metaPublishVersionMapper;
        this.objectMapper = objectMapper;
    }

    public DesignerSchemaResponse designerSchema(String appCode) {
        MetaAppEntity app = requiredApp(appCode);
        MetaObjectEntity object = firstObject(app.getId());
        MetaPageEntity page = firstPage(app.getId());
        List<LowCodeView.ComponentDetail> components = page == null ? List.of() : componentDetails(page.getId());

        return new DesignerSchemaResponse(
                appDetail(app),
                object == null ? null : objectDetail(object),
                page == null ? null : pageDetail(page, components),
                publishedVersion(app.getId()));
    }

    @Transactional
    public DesignerSaveResponse saveDesigner(String appCode, DesignerSaveRequest request) {
        MetaAppEntity app = requiredApp(appCode);
        MetaPageEntity page = upsertPage(app.getId(), request);
        replaceComponents(page.getId(), request.components());
        return new DesignerSaveResponse(
                appCode,
                page.getPageName(),
                page.getRoutePath(),
                request.components() == null ? 0 : request.components().size());
    }

    @Transactional
    public PublishVersionResponse publish(String appCode, PublishVersionRequest request) {
        MetaAppEntity app = requiredApp(appCode);
        RuntimeEntryResponse snapshot = runtimeEntryFromDraft(app);
        LocalDateTime now = LocalDateTime.now();

        MetaPublishVersionEntity version = new MetaPublishVersionEntity();
        version.setId(nextPublishVersionId());
        version.setAppId(app.getId());
        version.setVersionCode(nextVersionCode(app.getId()));
        version.setVersionStatus("PUBLISHED");
        version.setSnapshotSummary(request.versionNote() == null || request.versionNote().isBlank()
                ? "Published from designer studio"
                : request.versionNote());
        version.setSnapshotContent(writeJson(snapshot));
        version.setPublishedTime(now);
        applyAudit(version, now);
        metaPublishVersionMapper.insert(version);

        return new PublishVersionResponse(
                app.getAppCode(),
                version.getVersionCode(),
                version.getVersionStatus(),
                version.getSnapshotSummary());
    }

    public RuntimeEntryResponse runtimeEntry(String appCode) {
        MetaAppEntity app = requiredApp(appCode);
        MetaPublishVersionEntity version = latestPublishedVersion(app.getId());
        if (version != null && version.getSnapshotContent() != null && !version.getSnapshotContent().isBlank()) {
            return readSnapshot(version.getSnapshotContent());
        }
        return runtimeEntryFromDraft(app);
    }

    private RuntimeEntryResponse runtimeEntryFromDraft(MetaAppEntity app) {
        MetaPageEntity page = firstPage(app.getId());
        List<LowCodeView.ComponentDetail> components = page == null ? List.of() : componentDetails(page.getId());
        return new RuntimeEntryResponse(
                app.getAppCode(),
                app.getAppName(),
                page == null ? null : pageDetail(page, components),
                components,
                publishedVersion(app.getId()));
    }

    private MetaAppEntity requiredApp(String appCode) {
        MetaAppEntity app = metaAppMapper.selectOne(new LambdaQueryWrapper<MetaAppEntity>()
                .eq(MetaAppEntity::getDeletedFlag, 0)
                .eq(MetaAppEntity::getAppCode, appCode));
        if (app == null) {
            throw new IllegalArgumentException("App not found: " + appCode);
        }
        return app;
    }

    private MetaObjectEntity firstObject(Long appId) {
        return metaObjectMapper.selectOne(new LambdaQueryWrapper<MetaObjectEntity>()
                .eq(MetaObjectEntity::getDeletedFlag, 0)
                .eq(MetaObjectEntity::getAppId, appId)
                .orderByAsc(MetaObjectEntity::getId)
                .last("limit 1"));
    }

    private MetaPageEntity firstPage(Long appId) {
        return metaPageMapper.selectOne(new LambdaQueryWrapper<MetaPageEntity>()
                .eq(MetaPageEntity::getDeletedFlag, 0)
                .eq(MetaPageEntity::getAppId, appId)
                .orderByAsc(MetaPageEntity::getId)
                .last("limit 1"));
    }

    private LowCodeView.AppDetail appDetail(MetaAppEntity app) {
        return new LowCodeView.AppDetail(
                app.getId(),
                app.getAppCode(),
                app.getAppName(),
                app.getOwnerName(),
                app.getAppStatus());
    }

    private LowCodeView.ObjectDetail objectDetail(MetaObjectEntity object) {
        List<LowCodeView.FieldDetail> fields = metaObjectFieldMapper.selectList(new LambdaQueryWrapper<MetaObjectFieldEntity>()
                        .eq(MetaObjectFieldEntity::getDeletedFlag, 0)
                        .eq(MetaObjectFieldEntity::getObjectId, object.getId())
                        .orderByAsc(MetaObjectFieldEntity::getSortNo, MetaObjectFieldEntity::getId))
                .stream()
                .map(field -> new LowCodeView.FieldDetail(
                        field.getId(),
                        field.getFieldCode(),
                        field.getFieldName(),
                        field.getFieldType(),
                        field.getRequiredFlag() != null && field.getRequiredFlag() == 1,
                        field.getSortNo()))
                .toList();

        return new LowCodeView.ObjectDetail(
                object.getId(),
                object.getObjectCode(),
                object.getObjectName(),
                object.getStoreType(),
                object.getPrimaryFieldCode(),
                object.getStatusCode(),
                fields);
    }

    private LowCodeView.PageDetail pageDetail(MetaPageEntity page, List<LowCodeView.ComponentDetail> components) {
        return new LowCodeView.PageDetail(
                page.getId(),
                page.getPageCode(),
                page.getPageName(),
                page.getPageType(),
                page.getRoutePath(),
                page.getStatusCode(),
                components);
    }

    private List<LowCodeView.ComponentDetail> componentDetails(Long pageId) {
        List<MetaComponentEntity> components = metaComponentMapper.selectList(new LambdaQueryWrapper<MetaComponentEntity>()
                .eq(MetaComponentEntity::getDeletedFlag, 0)
                .eq(MetaComponentEntity::getPageId, pageId)
                .orderByAsc(MetaComponentEntity::getSortNo, MetaComponentEntity::getId));

        Map<Long, String> codeById = new HashMap<>();
        for (MetaComponentEntity component : components) {
            codeById.put(component.getId(), component.getComponentCode());
        }

        return components.stream()
                .map(component -> new LowCodeView.ComponentDetail(
                        component.getComponentCode(),
                        component.getComponentType(),
                        component.getParentId() == null ? null : codeById.get(component.getParentId()),
                        component.getSortNo(),
                        readProps(component.getComponentProps())))
                .toList();
    }

    private LowCodeView.PublishedVersion publishedVersion(Long appId) {
        MetaPublishVersionEntity version = latestPublishedVersion(appId);
        if (version == null) {
            return null;
        }
        return new LowCodeView.PublishedVersion(
                version.getId(),
                version.getVersionCode(),
                version.getVersionStatus(),
                version.getSnapshotSummary(),
                version.getPublishedTime());
    }

    private MetaPublishVersionEntity latestPublishedVersion(Long appId) {
        return metaPublishVersionMapper.selectOne(new LambdaQueryWrapper<MetaPublishVersionEntity>()
                .eq(MetaPublishVersionEntity::getDeletedFlag, 0)
                .eq(MetaPublishVersionEntity::getAppId, appId)
                .eq(MetaPublishVersionEntity::getVersionStatus, "PUBLISHED")
                .orderByDesc(MetaPublishVersionEntity::getPublishedTime, MetaPublishVersionEntity::getId)
                .last("limit 1"));
    }

    private MetaPageEntity upsertPage(Long appId, DesignerSaveRequest request) {
        MetaPageEntity page = firstPage(appId);
        LocalDateTime now = LocalDateTime.now();
        if (page == null) {
            page = new MetaPageEntity();
            page.setId(nextPageId());
            page.setAppId(appId);
            page.setPageCode("page-" + page.getId());
            applyAudit(page, now);
            fillPage(page, request);
            metaPageMapper.insert(page);
            return page;
        }

        metaPageMapper.update(
                null,
                new LambdaUpdateWrapper<MetaPageEntity>()
                        .eq(MetaPageEntity::getId, page.getId())
                        .set(MetaPageEntity::getPageName, request.pageName())
                        .set(MetaPageEntity::getPageType, request.pageType())
                        .set(MetaPageEntity::getRoutePath, request.routePath())
                        .set(MetaPageEntity::getStatusCode, request.statusCode())
                        .set(MetaPageEntity::getUpdatedBy, 1L)
                        .set(MetaPageEntity::getUpdatedTime, now));
        page.setPageName(request.pageName());
        page.setPageType(request.pageType());
        page.setRoutePath(request.routePath());
        page.setStatusCode(request.statusCode());
        return page;
    }

    private void fillPage(MetaPageEntity page, DesignerSaveRequest request) {
        page.setPageName(request.pageName());
        page.setPageType(request.pageType());
        page.setRoutePath(request.routePath());
        page.setStatusCode(request.statusCode());
    }

    private void replaceComponents(Long pageId, List<DesignerSaveRequest.ComponentDraft> components) {
        metaComponentMapper.delete(new LambdaQueryWrapper<MetaComponentEntity>()
                .eq(MetaComponentEntity::getPageId, pageId));

        if (components == null || components.isEmpty()) {
            return;
        }

        long nextId = nextComponentId();
        Map<String, Long> idByCode = new HashMap<>();
        for (DesignerSaveRequest.ComponentDraft component : components) {
            idByCode.put(component.componentCode(), nextId++);
        }

        LocalDateTime now = LocalDateTime.now();
        for (DesignerSaveRequest.ComponentDraft component : components) {
            MetaComponentEntity entity = new MetaComponentEntity();
            entity.setId(idByCode.get(component.componentCode()));
            entity.setPageId(pageId);
            entity.setComponentCode(component.componentCode());
            entity.setComponentType(component.componentType());
            entity.setParentId(component.parentCode() == null ? null : idByCode.get(component.parentCode()));
            entity.setSortNo(component.sortNo());
            entity.setComponentProps(writeJson(component.props() == null ? Collections.emptyMap() : component.props()));
            applyAudit(entity, now);
            metaComponentMapper.insert(entity);
        }
    }

    private long nextPageId() {
        MetaPageEntity latest = metaPageMapper.selectOne(new LambdaQueryWrapper<MetaPageEntity>()
                .orderByDesc(MetaPageEntity::getId)
                .last("limit 1"));
        return latest == null ? 1L : latest.getId() + 1;
    }

    private long nextComponentId() {
        MetaComponentEntity latest = metaComponentMapper.selectOne(new LambdaQueryWrapper<MetaComponentEntity>()
                .orderByDesc(MetaComponentEntity::getId)
                .last("limit 1"));
        return latest == null ? 1L : latest.getId() + 1;
    }

    private long nextPublishVersionId() {
        MetaPublishVersionEntity latest = metaPublishVersionMapper.selectOne(new LambdaQueryWrapper<MetaPublishVersionEntity>()
                .orderByDesc(MetaPublishVersionEntity::getId)
                .last("limit 1"));
        return latest == null ? 1L : latest.getId() + 1;
    }

    private String nextVersionCode(Long appId) {
        Long existingCount = metaPublishVersionMapper.selectCount(new LambdaQueryWrapper<MetaPublishVersionEntity>()
                .eq(MetaPublishVersionEntity::getDeletedFlag, 0)
                .eq(MetaPublishVersionEntity::getAppId, appId));
        return "v1.0." + existingCount;
    }

    private void applyAudit(io.cloudpivot.common.persistence.BaseEntity entity, LocalDateTime now) {
        entity.setCreatedBy(1L);
        entity.setCreatedTime(now);
        entity.setUpdatedBy(1L);
        entity.setUpdatedTime(now);
        entity.setDeletedFlag(0);
        entity.setVersionNo(0L);
    }

    private Map<String, Object> readProps(String content) {
        if (content == null || content.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(content, MAP_TYPE);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to read component props.", exception);
        }
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to write snapshot.", exception);
        }
    }

    private RuntimeEntryResponse readSnapshot(String content) {
        try {
            return objectMapper.readValue(content, RuntimeEntryResponse.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to read published snapshot.", exception);
        }
    }
}
