package com.shoerp.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shoerp.common.BusinessException;
import com.shoerp.common.PageQuery;
import com.shoerp.common.PageResult;
import com.shoerp.common.StatusCatalog;
import com.shoerp.dto.InquiryRequest;
import com.shoerp.entity.Inquiry;
import com.shoerp.entity.InquiryItem;
import com.shoerp.entity.Shoe;
import com.shoerp.entity.Supplier;
import com.shoerp.repository.InquiryRepository;
import com.shoerp.repository.ShoeRepository;
import com.shoerp.repository.SupplierRepository;
import com.shoerp.security.CurrentUser;

import jakarta.persistence.criteria.Predicate;

@Service
public class InquiryService {

    private final InquiryRepository inquiryRepository;

    private final ShoeRepository shoeRepository;

    private final SupplierRepository supplierRepository;

    private final DocumentNoGenerator noGenerator;

    public InquiryService(InquiryRepository inquiryRepository, ShoeRepository shoeRepository,
                          SupplierRepository supplierRepository, DocumentNoGenerator noGenerator) {
        this.inquiryRepository = inquiryRepository;
        this.shoeRepository = shoeRepository;
        this.supplierRepository = supplierRepository;
        this.noGenerator = noGenerator;
    }

    @Transactional(readOnly = true)
    public PageResult<Inquiry> page(String keyword, String status, Long supplierId, int page, int size) {
        Specification<Inquiry> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("inquiryNo"), like),
                        cb.like(root.get("supplierName"), like)));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (supplierId != null) {
                predicates.add(cb.equal(root.get("supplierId"), supplierId));
            }
            if (CurrentUser.needsOwnershipFilter()) {
                predicates.add(cb.equal(root.get("ownerId"), CurrentUser.id()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Inquiry> result = inquiryRepository.findAll(spec, PageQuery.of(page, size));
        result.getContent().forEach(i -> i.getItems().size());
        return PageResult.of(result);
    }

    @Transactional(readOnly = true)
    public Inquiry getById(Long id) {
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Inquiry not found"));
        if (!CurrentUser.owns(inquiry.getOwnerId())) {
            throw new BusinessException(403, "You can only access your own inquiries");
        }
        inquiry.getItems().size();
        return inquiry;
    }

    /** 从鞋款列表勾选直接生成询价单。 */
    @Transactional
    public Inquiry createFromShoes(Long supplierId, List<Long> shoeIds, String remark) {
        if (shoeIds == null || shoeIds.isEmpty()) {
            throw new BusinessException("Please select at least one shoe");
        }
        InquiryRequest request = new InquiryRequest();
        request.setSupplierId(supplierId);
        request.setRemark(remark);
        List<InquiryRequest.ItemPayload> items = new ArrayList<>();
        for (Long shoeId : shoeIds) {
            Shoe shoe = shoeRepository.findById(shoeId)
                    .orElseThrow(() -> new BusinessException(404, "Shoe not found: " + shoeId));
            InquiryRequest.ItemPayload item = new InquiryRequest.ItemPayload();
            item.setShoeId(shoe.getId());
            item.setStyleNo(shoe.getStyleNo());
            item.setProductName(shoe.getName());
            item.setImageUrl(shoe.getImageUrl());
            item.setUpperMaterial(shoe.getUpperMaterial());
            item.setSoleMaterial(shoe.getSoleMaterial());
            item.setColor(shoe.getColor());
            item.setSizeRange(shoe.getSizeRange());
            item.setQuantity(shoe.getMoq());
            items.add(item);
        }
        request.setItems(items);
        return save(request);
    }

    @Transactional
    public Inquiry save(InquiryRequest request) {
        Inquiry inquiry = request.getId() == null
                ? new Inquiry()
                : inquiryRepository.findById(request.getId())
                        .orElseThrow(() -> new BusinessException(404, "Inquiry not found"));
        if (request.getId() != null && !CurrentUser.owns(inquiry.getOwnerId())) {
            throw new BusinessException(403, "You can only access your own inquiries");
        }

        String inquiryNo = request.getInquiryNo();
        if (inquiryNo == null || inquiryNo.isBlank()) {
            inquiryNo = noGenerator.nextInquiryNo();
        } else if (inquiryRepository.existsByInquiryNo(inquiryNo)
                && !inquiryNo.equals(inquiry.getInquiryNo())) {
            throw new BusinessException("Inquiry number already exists: " + inquiryNo);
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Inquiry must contain at least one item");
        }

        inquiry.setInquiryNo(inquiryNo);
        if (inquiry.getOwnerId() == null) {
            inquiry.setOwnerId(CurrentUser.id());
        }
        inquiry.setSupplierId(request.getSupplierId());
        inquiry.setSupplierName(resolveSupplierName(request.getSupplierId()));
        inquiry.setInquiryDate(parseDate(request.getInquiryDate(), LocalDate.now()));
        inquiry.setDeadline(parseDate(request.getDeadline(), null));
        inquiry.setStatus(request.getStatus() == null
                ? (inquiry.getStatus() == null ? "DRAFT" : inquiry.getStatus())
                : StatusCatalog.require(request.getStatus(), StatusCatalog.INQUIRY, "Status"));
        inquiry.setCurrency(request.getCurrency() == null ? "CNY" : request.getCurrency());
        inquiry.setRemark(request.getRemark());

        inquiry.getItems().clear();
        for (InquiryRequest.ItemPayload payload : request.getItems()) {
            InquiryItem item = new InquiryItem();
            item.setInquiry(inquiry);
            item.setShoeId(payload.getShoeId());
            item.setStyleNo(payload.getStyleNo());
            item.setProductName(payload.getProductName());
            item.setImageUrl(payload.getImageUrl());
            item.setUpperMaterial(payload.getUpperMaterial());
            item.setSoleMaterial(payload.getSoleMaterial());
            item.setColor(payload.getColor());
            item.setSizeRange(payload.getSizeRange());
            item.setQuantity(payload.getQuantity());
            item.setUnit(payload.getUnit() == null ? "PRS" : payload.getUnit());
            item.setTargetPrice(payload.getTargetPrice());
            item.setQuotedPrice(payload.getQuotedPrice());
            item.setRemark(payload.getRemark());
            inquiry.getItems().add(item);
        }
        return inquiryRepository.save(inquiry);
    }

    @Transactional
    public Inquiry updateStatus(Long id, String status) {
        Inquiry inquiry = getById(id);
        inquiry.setStatus(StatusCatalog.require(status, StatusCatalog.INQUIRY, "Status"));
        return inquiryRepository.save(inquiry);
    }

    @Transactional
    public void delete(Long id) {
        Inquiry inquiry = getById(id);
        inquiryRepository.delete(inquiry);
    }

    private String resolveSupplierName(Long supplierId) {
        if (supplierId == null) {
            return null;
        }
        return supplierRepository.findById(supplierId)
                .map(Supplier::getName)
                .orElseThrow(() -> new BusinessException(404, "Supplier not found"));
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception ex) {
            throw new BusinessException(400, "Invalid date: " + value);
        }
    }
}
