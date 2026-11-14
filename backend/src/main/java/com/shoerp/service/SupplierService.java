package com.shoerp.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shoerp.common.BusinessException;
import com.shoerp.common.Numbers;
import com.shoerp.common.PageQuery;
import com.shoerp.common.PageResult;
import com.shoerp.entity.Supplier;
import com.shoerp.repository.PurchaseOrderRepository;
import com.shoerp.repository.ShoeRepository;
import com.shoerp.repository.SupplierRepository;
import com.shoerp.security.CurrentUser;

import jakarta.persistence.criteria.Predicate;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    private final ShoeRepository shoeRepository;

    private final PurchaseOrderRepository purchaseRepository;

    private final DocumentNoGenerator noGenerator;

    public SupplierService(SupplierRepository supplierRepository, ShoeRepository shoeRepository,
                           PurchaseOrderRepository purchaseRepository, DocumentNoGenerator noGenerator) {
        this.supplierRepository = supplierRepository;
        this.shoeRepository = shoeRepository;
        this.purchaseRepository = purchaseRepository;
        this.noGenerator = noGenerator;
    }

    @Transactional(readOnly = true)
    public PageResult<Supplier> page(String keyword, String type, Boolean enabled, int page, int size) {
        Specification<Supplier> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("code"), like),
                        cb.like(root.get("name"), like),
                        cb.like(root.get("nameEn"), like),
                        cb.like(root.get("contactPerson"), like),
                        cb.like(root.get("country"), like)));
            }
            if (type != null && !type.isBlank()) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }
            if (CurrentUser.needsOwnershipFilter()) {
                predicates.add(cb.equal(root.get("ownerId"), CurrentUser.id()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Supplier> result = supplierRepository.findAll(spec, PageQuery.of(page, size));
        return PageResult.of(result);
    }

    @Transactional(readOnly = true)
    public List<Supplier> listEnabled() {
        return supplierRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .filter(s -> Boolean.TRUE.equals(s.getEnabled()))
                .filter(s -> CurrentUser.owns(s.getOwnerId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Supplier getById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Supplier not found"));
        if (!CurrentUser.owns(supplier.getOwnerId())) {
            throw new BusinessException(403, "You can only access your own suppliers");
        }
        return supplier;
    }

    @Transactional
    public Supplier save(Supplier payload) {
        Supplier supplier;
        if (payload.getId() == null) {
            if (payload.getCode() == null || payload.getCode().isBlank()) {
                payload.setCode(noGenerator.nextSupplierCode());
            }
            if (supplierRepository.existsByCode(payload.getCode())) {
                throw new BusinessException("Supplier code already exists");
            }
            supplier = new Supplier();
        } else {
            supplier = getById(payload.getId());
            if (payload.getCode() != null
                    && !payload.getCode().equals(supplier.getCode())
                    && supplierRepository.existsByCode(payload.getCode())) {
                throw new BusinessException("Supplier code already exists");
            }
        }
        if (payload.getName() == null || payload.getName().isBlank()) {
            throw new BusinessException("Supplier name is required");
        }
        if (supplier.getOwnerId() == null) {
            supplier.setOwnerId(CurrentUser.id());
        }
        supplier.setCode(payload.getCode());
        supplier.setName(payload.getName());
        supplier.setNameEn(payload.getNameEn());
        supplier.setContactPerson(payload.getContactPerson());
        supplier.setPhone(payload.getPhone());
        supplier.setEmail(payload.getEmail());
        supplier.setAddress(payload.getAddress());
        supplier.setCountry(payload.getCountry());
        supplier.setPaymentTerms(payload.getPaymentTerms());
        supplier.setType(payload.getType());
        supplier.setShortCode(payload.getShortCode());
        supplier.setRating(payload.getRating() == null ? 5 : Numbers.nonNegative(payload.getRating(), "Rating"));
        supplier.setEnabled(payload.getEnabled() == null || payload.getEnabled());
        supplier.setRemark(payload.getRemark());
        return supplierRepository.save(supplier);
    }

    @Transactional
    public void delete(Long id) {
        getById(id);
        if (shoeRepository.existsBySupplierId(id)) {
            throw new BusinessException("Supplier is referenced by shoes and cannot be deleted");
        }
        if (purchaseRepository.existsBySupplierId(id)) {
            throw new BusinessException("Supplier is referenced by purchase orders and cannot be deleted");
        }
        supplierRepository.deleteById(id);
    }
}
