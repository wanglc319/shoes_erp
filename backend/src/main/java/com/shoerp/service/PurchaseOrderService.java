package com.shoerp.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shoerp.common.BusinessException;
import com.shoerp.common.Dates;
import com.shoerp.common.Numbers;
import com.shoerp.common.PageQuery;
import com.shoerp.common.PageResult;
import com.shoerp.common.StatusCatalog;
import com.shoerp.dto.PurchaseRequest;
import com.shoerp.entity.PurchaseOrder;
import com.shoerp.entity.PurchaseOrderItem;
import com.shoerp.entity.Supplier;
import com.shoerp.repository.PurchaseOrderRepository;
import com.shoerp.repository.SupplierRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseRepository;

    private final SupplierRepository supplierRepository;

    private final DocumentNoGenerator noGenerator;

    public PurchaseOrderService(PurchaseOrderRepository purchaseRepository, SupplierRepository supplierRepository,
                                DocumentNoGenerator noGenerator) {
        this.purchaseRepository = purchaseRepository;
        this.supplierRepository = supplierRepository;
        this.noGenerator = noGenerator;
    }

    @Transactional(readOnly = true)
    public PageResult<PurchaseOrder> page(String keyword, String status, Long supplierId, int page, int size) {
        Specification<PurchaseOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("purchaseNo"), like),
                        cb.like(root.get("supplierName"), like)));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (supplierId != null) {
                predicates.add(cb.equal(root.get("supplierId"), supplierId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<PurchaseOrder> result = purchaseRepository.findAll(spec, PageQuery.of(page, size));
        result.getContent().forEach(p -> p.getItems().size());
        return PageResult.of(result);
    }

    @Transactional(readOnly = true)
    public PurchaseOrder getById(Long id) {
        PurchaseOrder purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Purchase order not found"));
        purchase.getItems().size();
        return purchase;
    }

    @Transactional
    public PurchaseOrder save(PurchaseRequest request) {
        PurchaseOrder purchase = request.getId() == null
                ? new PurchaseOrder()
                : purchaseRepository.findById(request.getId())
                        .orElseThrow(() -> new BusinessException(404, "Purchase order not found"));

        String purchaseNo = request.getPurchaseNo();
        boolean generated = purchaseNo == null || purchaseNo.isBlank();
        if (generated) {
            purchaseNo = nextAvailableNo();
        } else if (request.getId() == null && purchaseRepository.existsByPurchaseNo(purchaseNo)) {
            throw new BusinessException("Purchase number already exists: " + purchaseNo);
        } else if (request.getId() != null
                && !purchaseNo.equals(purchase.getPurchaseNo())
                && purchaseRepository.existsByPurchaseNo(purchaseNo)) {
            throw new BusinessException("Purchase number already exists: " + purchaseNo);
        }

        Supplier supplier = resolveSupplier(request.getSupplierId());

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Purchase order must contain at least one item");
        }

        purchase.setPurchaseNo(purchaseNo);
        purchase.setSupplierId(supplier.getId());
        purchase.setSupplierName(supplier.getName());
        purchase.setPurchaseDate(parseDate(request.getPurchaseDate(), LocalDate.now()));
        purchase.setExpectedDate(parseDate(request.getExpectedDate(), null));
        purchase.setCurrency(request.getCurrency() == null ? "CNY" : request.getCurrency());
        purchase.setPaymentTerms(request.getPaymentTerms());
        purchase.setStatus(request.getStatus() == null ? "DRAFT" : StatusCatalog.require(request.getStatus(), StatusCatalog.PURCHASE, "Status"));
        purchase.setBuyer(request.getBuyer());
        purchase.setRemark(request.getRemark());

        purchase.getItems().clear();
        BigDecimal total = BigDecimal.ZERO;
        if (request.getItems() != null) {
            for (PurchaseRequest.ItemPayload payload : request.getItems()) {
                PurchaseOrderItem item = new PurchaseOrderItem();
                item.setPurchaseOrder(purchase);
                item.setShoeId(payload.getShoeId());
                item.setStyleNo(payload.getStyleNo());
                item.setProductName(payload.getProductName());
                item.setColor(payload.getColor());
                item.setSizeRange(payload.getSizeRange());
                item.setQuantity(Numbers.nonNegative(payload.getQuantity(), "Quantity"));
                item.setUnit(payload.getUnit() == null ? "PRS" : payload.getUnit());
                item.setUnitPrice(Numbers.nonNegative(payload.getUnitPrice(), "Unit price"));
                item.setRemark(payload.getRemark());
                BigDecimal amount = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                item.setAmount(scale(amount));
                total = total.add(item.getAmount());
                purchase.getItems().add(item);
            }
        }
        purchase.setTotalAmount(scale(total));
        return purchaseRepository.save(purchase);
    }

    @Transactional
    public PurchaseOrder updateStatus(Long id, String status) {
        PurchaseOrder purchase = getById(id);
        purchase.setStatus(StatusCatalog.require(status, StatusCatalog.PURCHASE, "Status"));
        return purchaseRepository.save(purchase);
    }

    @Transactional
    public void delete(Long id) {
        if (!purchaseRepository.existsById(id)) {
            throw new BusinessException(404, "Purchase order not found");
        }
        purchaseRepository.deleteById(id);
    }

    private Supplier resolveSupplier(Long supplierId) {
        if (supplierId == null) {
            throw new BusinessException("Supplier is required");
        }
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new BusinessException(404, "Supplier not found"));
    }

    private String nextAvailableNo() {
        String candidate = noGenerator.nextPurchaseNo();
        while (purchaseRepository.existsByPurchaseNo(candidate)) {
            candidate = noGenerator.nextPurchaseNo();
        }
        return candidate;
    }

    private LocalDate parseDate(String value, LocalDate defaultValue) {
        return Dates.parse(value, defaultValue);
    }

    private BigDecimal scale(BigDecimal value) {
        return Numbers.scale(value);
    }
}
