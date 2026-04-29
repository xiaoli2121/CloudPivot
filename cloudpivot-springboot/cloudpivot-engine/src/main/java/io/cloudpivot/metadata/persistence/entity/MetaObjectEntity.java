package io.cloudpivot.metadata.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import io.cloudpivot.common.persistence.BaseEntity;

@TableName("meta_object")
public class MetaObjectEntity extends BaseEntity {

    private Long appId;
    private String objectCode;
    private String objectName;
    private String storeType;
    private String primaryFieldCode;
    private String statusCode;

    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public String getObjectCode() {
        return objectCode;
    }

    public void setObjectCode(String objectCode) {
        this.objectCode = objectCode;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getStoreType() {
        return storeType;
    }

    public void setStoreType(String storeType) {
        this.storeType = storeType;
    }

    public String getPrimaryFieldCode() {
        return primaryFieldCode;
    }

    public void setPrimaryFieldCode(String primaryFieldCode) {
        this.primaryFieldCode = primaryFieldCode;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }
}
