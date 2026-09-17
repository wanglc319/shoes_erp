package com.shoerp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "shoe_image")
public class ShoeImage extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shoe_id", nullable = false)
    private Shoe shoe;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(length = 160)
    private String title;

    @Column(name = "primary_image", nullable = false)
    private Boolean primaryImage = false;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
