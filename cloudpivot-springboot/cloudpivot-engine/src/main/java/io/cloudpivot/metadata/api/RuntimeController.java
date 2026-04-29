package io.cloudpivot.metadata.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.cloudpivot.common.api.ApiResponse;
import io.cloudpivot.metadata.api.dto.RuntimeEntryResponse;
import io.cloudpivot.metadata.service.MetadataStudioService;

@RestController
@RequestMapping("/api/runtime")
public class RuntimeController {

    private final MetadataStudioService metadataStudioService;

    public RuntimeController(MetadataStudioService metadataStudioService) {
        this.metadataStudioService = metadataStudioService;
    }

    @GetMapping("/apps/{appCode}/entry")
    public ApiResponse<RuntimeEntryResponse> entry(@PathVariable String appCode) {
        return ApiResponse.success(metadataStudioService.runtimeEntry(appCode));
    }
}
