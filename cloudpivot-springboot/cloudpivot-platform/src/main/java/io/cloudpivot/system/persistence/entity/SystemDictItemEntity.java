package io.cloudpivot.system.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import io.cloudpivot.common.persistence.BaseEntity;

@TableName("sys_dict_item")
public class SystemDictItemEntity extends BaseEntity {

    private Long dictId;
    private String itemLabel;
    private String itemValue;
    private Integer sortNo;

    public Long getDictId() {
        return dictId;
    }

    public void setDictId(Long dictId) {
        this.dictId = dictId;
    }

    public String getItemLabel() {
        return itemLabel;
    }

    public void setItemLabel(String itemLabel) {
        this.itemLabel = itemLabel;
    }

    public String getItemValue() {
        return itemValue;
    }

    public void setItemValue(String itemValue) {
        this.itemValue = itemValue;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }
}
