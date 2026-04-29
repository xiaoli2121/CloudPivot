package io.cloudpivot.plugin.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import io.cloudpivot.common.api.NotFoundException;
import io.cloudpivot.common.api.PageResponse;
import io.cloudpivot.plugin.api.dto.PluginSummary;
import io.cloudpivot.plugin.persistence.entity.PluginRegistryEntity;
import io.cloudpivot.plugin.persistence.mapper.PluginRegistryMapper;

@Service
public class PluginRegistryService {

    private final PluginRegistryMapper pluginRegistryMapper;

    public PluginRegistryService(PluginRegistryMapper pluginRegistryMapper) {
        this.pluginRegistryMapper = pluginRegistryMapper;
    }

    public List<PluginSummary> registry() {
        return pluginRegistryMapper.selectList(new LambdaQueryWrapper<PluginRegistryEntity>()
                        .eq(PluginRegistryEntity::getDeletedFlag, 0)
                        .orderByAsc(PluginRegistryEntity::getId))
                .stream()
                .map(plugin -> new PluginSummary(
                        plugin.getPluginCode(),
                        plugin.getPluginName(),
                        plugin.getPluginType(),
                        plugin.getPluginVersion(),
                        plugin.getStatusCode(),
                        plugin.getEntryPoint(),
                        plugin.getDescription()))
                .toList();
    }

    public PageResponse<Map<String, Object>> plugins() {
        List<Map<String, Object>> records = pluginRegistryMapper.selectList(new LambdaQueryWrapper<PluginRegistryEntity>()
                        .eq(PluginRegistryEntity::getDeletedFlag, 0)
                        .orderByAsc(PluginRegistryEntity::getId))
                .stream()
                .map(this::detailMap)
                .toList();
        return new PageResponse<>(records, records.size());
    }

    public Map<String, Object> pluginDetail(Long id) {
        PluginRegistryEntity entity = pluginRegistryMapper.selectOne(new LambdaQueryWrapper<PluginRegistryEntity>()
                .eq(PluginRegistryEntity::getDeletedFlag, 0)
                .eq(PluginRegistryEntity::getId, id));
        if (entity == null) {
            throw new NotFoundException("Plugin not found: " + id);
        }
        return detailMap(entity);
    }

    private Map<String, Object> detailMap(PluginRegistryEntity plugin) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", plugin.getId());
        map.put("pluginCode", plugin.getPluginCode());
        map.put("pluginName", plugin.getPluginName());
        map.put("pluginType", plugin.getPluginType());
        map.put("pluginVersion", plugin.getPluginVersion());
        map.put("status", plugin.getStatusCode());
        map.put("statusCode", plugin.getStatusCode());
        map.put("entryPoint", plugin.getEntryPoint());
        map.put("description", plugin.getDescription());
        return map;
    }
}
