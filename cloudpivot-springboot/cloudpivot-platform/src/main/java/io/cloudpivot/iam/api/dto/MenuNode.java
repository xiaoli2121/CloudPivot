package io.cloudpivot.iam.api.dto;

import java.util.List;

public record MenuNode(
        String code,
        String name,
        String path,
        List<MenuNode> children) {
}
