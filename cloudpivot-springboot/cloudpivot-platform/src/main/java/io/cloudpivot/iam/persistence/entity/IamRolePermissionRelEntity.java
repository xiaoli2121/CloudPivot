package io.cloudpivot.iam.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import io.cloudpivot.common.persistence.BaseEntity;

@TableName("iam_role_permission_rel")
public class IamRolePermissionRelEntity extends BaseEntity {

    private Long roleId;
    private String resourceType;
    private String permissionCode;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }
}
