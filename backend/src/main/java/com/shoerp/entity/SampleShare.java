package com.shoerp.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * 样品相册分享：一组样品的只读展示链接，不包含任何价格字段。
 */
@Getter
@Setter
@Entity
@Table(name = "sample_share", uniqueConstraints = @UniqueConstraint(columnNames = "token"))
public class SampleShare extends OwnedEntity {

    @Column(nullable = false, length = 64)
    private String token;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(name = "shoe_ids", nullable = false, columnDefinition = "TEXT")
    private String shoeIds;

    @Column(name = "expire_at")
    private LocalDate expireAt;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(length = 300)
    private String remark;
}
