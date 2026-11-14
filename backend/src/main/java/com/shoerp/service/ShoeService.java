package com.shoerp.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shoerp.common.BusinessException;
import com.shoerp.common.Numbers;
import com.shoerp.common.PageQuery;
import com.shoerp.common.PageResult;
import com.shoerp.common.StatusCatalog;
import com.shoerp.entity.Shoe;
import com.shoerp.entity.ShoeImage;
import com.shoerp.entity.Supplier;
import com.shoerp.repository.ShoeRepository;
import com.shoerp.repository.SupplierRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class ShoeService {

    private final ShoeRepository shoeRepository;

    private final SupplierRepository supplierRepository;

    public ShoeService(ShoeRepository shoeRepository, SupplierRepository supplierRepository) {
        this.shoeRepository = shoeRepository;
        this.supplierRepository = supplierRepository;
    }

    /**
     * 按 SN-年份-工厂代码-序号 生成建议款号，例如 SN-26-01-0001。
     * 工厂代码取自供应商 shortCode，未维护时用 00。
     */
    @Transactional(readOnly = true)
    public String nextStyleNo(Long supplierId) {
        String factoryCode = "00";
        if (supplierId != null) {
            factoryCode = supplierRepository.findById(supplierId)
                    .map(Supplier::getShortCode)
                    .filter(code -> code != null && !code.isBlank())
                    .orElse("00");
        }
        String year = String.format("%02d", LocalDate.now().getYear() % 100);
        String prefix = "SN-" + year + "-" + factoryCode + "-";
        String max = shoeRepository.findMaxStyleNoByPrefix(prefix);
        int sequence = 1;
        if (max != null && max.length() > prefix.length()) {
            try {
                sequence = Integer.parseInt(max.substring(prefix.length())) + 1;
            } catch (NumberFormatException ignored) {
                sequence = 1;
            }
        }
        return prefix + String.format("%04d", sequence);
    }

    @Transactional(readOnly = true)
    public PageResult<Shoe> page(String keyword, String category, String gender, Long supplierId, int page, int size) {
        Specification<Shoe> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("styleNo"), like),
                        cb.like(root.get("name"), like),
                        cb.like(root.get("nameEn"), like),
                        cb.like(root.get("color"), like)));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (gender != null && !gender.isBlank()) {
                predicates.add(cb.equal(root.get("gender"), gender));
            }
            if (supplierId != null) {
                predicates.add(cb.equal(root.get("supplierId"), supplierId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Shoe> result = shoeRepository.findAll(spec, PageQuery.of(page, size));
        result.getContent().forEach(shoe -> shoe.getImages().size());
        return PageResult.of(result);
    }

    @Transactional(readOnly = true)
    public List<Shoe> listEnabled() {
        List<Shoe> shoes = shoeRepository.findAll(Sort.by(Sort.Direction.ASC, "styleNo")).stream()
                .filter(s -> Boolean.TRUE.equals(s.getEnabled()))
                .toList();
        shoes.forEach(shoe -> shoe.getImages().size());
        return shoes;
    }

    @Transactional(readOnly = true)
    public Shoe getById(Long id) {
        Shoe shoe = shoeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Shoe not found"));
        shoe.getImages().size();
        return shoe;
    }

    @Transactional(readOnly = true)
    public Shoe findByStyleNo(String styleNo) {
        if (styleNo == null || styleNo.isBlank()) {
            return null;
        }
        return shoeRepository.findByStyleNo(styleNo.trim()).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Shoe> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Shoe> found = shoeRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Shoe::getId, shoe -> shoe));
        List<Shoe> result = new ArrayList<>();
        for (Long id : ids) {
            Shoe shoe = found.get(id);
            if (shoe != null) {
                shoe.getImages().size();
                result.add(shoe);
            }
        }
        return result;
    }

    /**
     * 供应商展示名：优先「名称 (工厂代码)」，贴纸导出用。
     */
    @Transactional(readOnly = true)
    public Map<Long, String> factoryNames(List<Shoe> shoes) {
        Set<Long> supplierIds = shoes.stream()
                .map(Shoe::getSupplierId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> result = new HashMap<>();
        if (supplierIds.isEmpty()) {
            return result;
        }
        for (Supplier supplier : supplierRepository.findAllById(supplierIds)) {
            String code = supplier.getShortCode();
            result.put(supplier.getId(), code == null || code.isBlank()
                    ? supplier.getName()
                    : supplier.getName() + " (" + code + ")");
        }
        return result;
    }

    @Transactional
    public Shoe save(Shoe payload) {
        Shoe shoe;
        String styleNo = payload.getStyleNo();
        if (payload.getId() == null) {
            if (styleNo == null || styleNo.isBlank()) {
                styleNo = nextStyleNo(payload.getSupplierId());
            }
            if (shoeRepository.existsByStyleNo(styleNo)) {
                throw new BusinessException("Style No already exists");
            }
            shoe = new Shoe();
        } else {
            shoe = getById(payload.getId());
            String targetNo = styleNo == null || styleNo.isBlank() ? shoe.getStyleNo() : styleNo;
            if (!targetNo.equals(shoe.getStyleNo()) && shoeRepository.existsByStyleNo(targetNo)) {
                throw new BusinessException("Style No already exists");
            }
            styleNo = targetNo;
        }
        if (payload.getName() == null || payload.getName().isBlank()) {
            throw new BusinessException("Shoe name is required");
        }
        shoe.setStyleNo(styleNo);
        shoe.setName(payload.getName());
        shoe.setNameEn(payload.getNameEn());
        shoe.setCategory(payload.getCategory());
        shoe.setGender(payload.getGender());
        shoe.setUpperMaterial(payload.getUpperMaterial());
        shoe.setSoleMaterial(payload.getSoleMaterial());
        shoe.setColor(payload.getColor());
        shoe.setSizeRange(payload.getSizeRange());
        shoe.setCostPrice(Numbers.nonNegativeOrNull(payload.getCostPrice(), "Cost price"));
        shoe.setSellingPrice(Numbers.nonNegativeOrNull(payload.getSellingPrice(), "Selling price"));
        shoe.setMoq(Numbers.nonNegativeOrNull(payload.getMoq(), "MOQ"));
        shoe.setSupplierId(payload.getSupplierId());
        shoe.setPriceOptions(payload.getPriceOptions());
        shoe.setDescription(payload.getDescription());
        shoe.setStatus(payload.getStatus() == null ? "ACTIVE" : StatusCatalog.require(payload.getStatus(), StatusCatalog.SHOE, "Status"));
        shoe.setEnabled(payload.getEnabled() == null || payload.getEnabled());
        applyImages(shoe, payload);
        return shoeRepository.save(shoe);
    }

    private void applyImages(Shoe shoe, Shoe payload) {
        if (payload.getImages() == null) {
            if (shoe.getImageUrl() == null && payload.getImageUrl() != null) {
                shoe.setImageUrl(payload.getImageUrl());
            }
            return;
        }
        shoe.getImages().clear();
        int index = 0;
        String primary = null;
        for (ShoeImage payloadImage : payload.getImages()) {
            if (payloadImage.getImageUrl() == null || payloadImage.getImageUrl().isBlank()) {
                continue;
            }
            ShoeImage image = new ShoeImage();
            image.setShoe(shoe);
            image.setImageUrl(payloadImage.getImageUrl().trim());
            image.setTitle(payloadImage.getTitle());
            image.setSortOrder(payloadImage.getSortOrder() == null ? index : payloadImage.getSortOrder());
            boolean isPrimary = Boolean.TRUE.equals(payloadImage.getPrimaryImage());
            image.setPrimaryImage(isPrimary);
            if (isPrimary && primary == null) {
                primary = image.getImageUrl();
            }
            shoe.getImages().add(image);
            index++;
        }
        if (primary == null && !shoe.getImages().isEmpty()) {
            ShoeImage first = shoe.getImages().get(0);
            first.setPrimaryImage(true);
            primary = first.getImageUrl();
        }
        shoe.setImageUrl(primary);
    }

    @Transactional
    public void delete(Long id) {
        if (!shoeRepository.existsById(id)) {
            throw new BusinessException(404, "Shoe not found");
        }
        if (shoeRepository.countItemReferences(id) > 0) {
            throw new BusinessException("Shoe is referenced by documents and cannot be deleted");
        }
        shoeRepository.deleteById(id);
    }
}
