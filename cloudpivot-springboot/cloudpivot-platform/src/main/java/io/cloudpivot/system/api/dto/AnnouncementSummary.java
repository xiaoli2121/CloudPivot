package io.cloudpivot.system.api.dto;

public record AnnouncementSummary(
        long announcementId,
        String title,
        String level,
        String publisher,
        String publishedAt) {
}
