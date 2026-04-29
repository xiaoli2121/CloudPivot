package io.cloudpivot.metadata.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.cloudpivot.common.api.ApiResponse;
import io.cloudpivot.metadata.api.dto.PortalAppSummary;
import io.cloudpivot.metadata.service.MetadataQueryService;

@RestController
@RequestMapping("/api/portal")
public class PortalController {

    private final MetadataQueryService metadataQueryService;

    public PortalController(MetadataQueryService metadataQueryService) {
        this.metadataQueryService = metadataQueryService;
    }

    @GetMapping("/apps")
    public ApiResponse<List<PortalAppSummary>> apps() {
        return ApiResponse.success(metadataQueryService.portalApps());
    }
}
