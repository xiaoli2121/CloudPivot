package io.cloudpivot.iam.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import io.cloudpivot.common.persistence.BaseEntity;

@TableName("iam_role")
public class IamRoleEntity extends BaseEntity {

    private String roleCode;
    private String roleName;
    private String dataScope;

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDataScope() {
        return dataScope;
    }

    public void setDataScope(String dataScope) {
        this.dataScope = dataScope;
    }
}
