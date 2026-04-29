package io.cloudpivot.metadata.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import io.cloudpivot.common.persistence.BaseEntity;

@TableName("meta_component")
public class MetaComponentEntity extends BaseEntity {

    private Long pageId;
    private String componentCode;
    private String componentType;
    private Long parentId;
    private Integer sortNo;
    private String componentProps;

    public Long getPageId() {
        return pageId;
    }

    public void setPageId(Long pageId) {
        this.pageId = pageId;
    }

    public String getComponentCode() {
        return componentCode;
    }

    public void setComponentCode(String componentCode) {
        this.componentCode = componentCode;
    }

    public String getComponentType() {
        return componentType;
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public String getComponentProps() {
        return componentProps;
    }

    public void setComponentProps(String componentProps) {
        this.componentProps = componentProps;
    }
}
