package com.shoerp.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "shipment", uniqueConstraints = @UniqueConstraint(columnNames = "shipment_no"))
public class Shipment extends BaseEntity {

    @Column(name = "shipment_no", nullable = false, length = 40)
    private String shipmentNo;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_no", length = 40)
    private String orderNo;

    @Column(name = "customer_name", length = 120)
    private String customerName;

    @Column(name = "ship_date", nullable = false)
    private LocalDate shipDate;

    @Column(name = "container_no", length = 60)
    private String containerNo;

    @Column(name = "bl_no", length = 60)
    private String blNo;

    @Column(name = "vessel", length = 80)
    private String vessel;

    @Column(name = "port_of_loading", length = 80)
    private String portOfLoading;

    @Column(name = "port_of_destination", length = 80)
    private String portOfDestination;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity = 0;

    @Column(name = "total_cartons")
    private Integer totalCartons = 0;

    @Column(name = "total_amount", precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "PREPARING";

    @Column(length = 500)
    private String remark;

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<ShipmentItem> items = new ArrayList<>();
}
