package io.cloudpivot.system.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import io.cloudpivot.common.persistence.BaseEntity;

@TableName("sys_announcement")
public class SystemAnnouncementEntity extends BaseEntity {

    private String title;
    private String levelCode;
    private String publisherName;
    private String publishTime;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLevelCode() {
        return levelCode;
    }

    public void setLevelCode(String levelCode) {
        this.levelCode = levelCode;
    }

    public String getPublisherName() {
        return publisherName;
    }

    public void setPublisherName(String publisherName) {
        this.publisherName = publisherName;
    }

    public String getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(String publishTime) {
        this.publishTime = publishTime;
    }
}
