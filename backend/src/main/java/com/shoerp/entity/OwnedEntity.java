package com.shoerp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * 带归属人的业务单据基类。ownerId 为空时视为管理员/历史数据。
 */
@Getter
@Setter
@MappedSuperclass
public abstract class OwnedEntity extends BaseEntity {

    @Column(name = "owner_id")
    private Long ownerId;
}
