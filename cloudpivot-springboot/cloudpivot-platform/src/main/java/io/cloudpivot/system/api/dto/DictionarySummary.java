package io.cloudpivot.system.api.dto;

import java.util.List;

public record DictionarySummary(
        String dictCode,
        String dictName,
        List<DictionaryItem> items) {
}
