package io.cloudpivot.plugin.api.dto;

public record PluginSummary(
        String pluginCode,
        String pluginName,
        String pluginType,
        String version,
        String status,
        String entryPoint,
        String description) {
}
