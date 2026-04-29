package io.cloudpivot.metadata.persistence.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;

import io.cloudpivot.common.persistence.BaseEntity;

@TableName("meta_publish_version")
public class MetaPublishVersionEntity extends BaseEntity {

    private Long appId;
    private String versionCode;
    private String versionStatus;
    private String snapshotSummary;
    private String snapshotContent;
    private LocalDateTime publishedTime;

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public String getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(String versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionStatus() {
        return versionStatus;
    }

    public void setVersionStatus(String versionStatus) {
        this.versionStatus = versionStatus;
    }

    public String getSnapshotSummary() {
        return snapshotSummary;
    }

    public void setSnapshotSummary(String snapshotSummary) {
        this.snapshotSummary = snapshotSummary;
    }

    public String getSnapshotContent() {
        return snapshotContent;
    }

    public void setSnapshotContent(String snapshotContent) {
        this.snapshotContent = snapshotContent;
    }

    public LocalDateTime getPublishedTime() {
        return publishedTime;
    }

    public void setPublishedTime(LocalDateTime publishedTime) {
        this.publishedTime = publishedTime;
    }
}
