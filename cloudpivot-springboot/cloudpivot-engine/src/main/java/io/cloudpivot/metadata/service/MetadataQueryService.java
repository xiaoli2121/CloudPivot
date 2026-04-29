package io.cloudpivot.metadata.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.cloudpivot.metadata.api.dto.AppSummary;
import io.cloudpivot.metadata.api.dto.PortalAppSummary;
import io.cloudpivot.metadata.persistence.entity.MetaAppEntity;
import io.cloudpivot.metadata.persistence.entity.MetaPageEntity;
import io.cloudpivot.metadata.persistence.entity.MetaPublishVersionEntity;
import io.cloudpivot.metadata.persistence.mapper.MetaAppMapper;
import io.cloudpivot.metadata.persistence.mapper.MetaPageMapper;
import io.cloudpivot.metadata.persistence.mapper.MetaPublishVersionMapper;

@Service
public class MetadataQueryService {

    private final MetaAppMapper metaAppMapper;
    private final MetaPageMapper metaPageMapper;
    private final MetaPublishVersionMapper metaPublishVersionMapper;

    public MetadataQueryService(
            MetaAppMapper metaAppMapper,
            MetaPageMapper metaPageMapper,
            MetaPublishVersionMapper metaPublishVersionMapper) {
        this.metaAppMapper = metaAppMapper;
        this.metaPageMapper = metaPageMapper;
        this.metaPublishVersionMapper = metaPublishVersionMapper;
    }

    public List<AppSummary> apps() {
        return metaAppMapper.selectList(new LambdaQueryWrapper<MetaAppEntity>()
                        .eq(MetaAppEntity::getDeletedFlag, 0)
                        .orderByAsc(MetaAppEntity::getId))
                .stream()
                .map(app -> new AppSummary(
                        app.getId(),
                        app.getAppCode(),
                        app.getAppName(),
                        app.getOwnerName(),
                        app.getAppStatus()))
                .toList();
    }

    public List<PortalAppSummary> portalApps() {
        List<MetaAppEntity> apps = metaAppMapper.selectList(new LambdaQueryWrapper<MetaAppEntity>()
                .eq(MetaAppEntity::getDeletedFlag, 0)
                .eq(MetaAppEntity::getAppStatus, "ACTIVE")
                .orderByAsc(MetaAppEntity::getId));

        Map<Long, MetaPageEntity> pageByAppId = metaPageMapper.selectList(new LambdaQueryWrapper<MetaPageEntity>()
                        .eq(MetaPageEntity::getDeletedFlag, 0)
                        .orderByAsc(MetaPageEntity::getId))
                .stream()
                .collect(Collectors.toMap(
                        MetaPageEntity::getAppId,
                        Function.identity(),
                        (left, right) -> left));

        Map<Long, MetaPublishVersionEntity> versionByAppId = metaPublishVersionMapper.selectList(new LambdaQueryWrapper<MetaPublishVersionEntity>()
                        .eq(MetaPublishVersionEntity::getDeletedFlag, 0)
                        .eq(MetaPublishVersionEntity::getVersionStatus, "PUBLISHED")
                        .orderByDesc(MetaPublishVersionEntity::getPublishedTime, MetaPublishVersionEntity::getId))
                .stream()
                .collect(Collectors.toMap(
                        MetaPublishVersionEntity::getAppId,
                        Function.identity(),
                        (left, right) -> left));

        return apps.stream()
                .map(app -> new PortalAppSummary(
                        app.getAppCode(),
                        app.getAppName(),
                        app.getOwnerName(),
                        pageByAppId.containsKey(app.getId()) ? pageByAppId.get(app.getId()).getRoutePath() : "",
                        versionByAppId.containsKey(app.getId()) ? versionByAppId.get(app.getId()).getVersionCode() : ""))
                .toList();
    }
}
