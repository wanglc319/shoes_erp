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
import com.shoerp.common.StatusCatalog;
import com.shoerp.entity.Shoe;
import com.shoerp.entity.ShoeImage;
import com.shoerp.repository.ShoeRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class ShoeService {

    private final ShoeRepository shoeRepository;

    public ShoeService(ShoeRepository shoeRepository) {
        this.shoeRepository = shoeRepository;
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

    @Transactional
    public Shoe save(Shoe payload) {
        Shoe shoe;
        if (payload.getId() == null) {
            if (payload.getStyleNo() == null || payload.getStyleNo().isBlank()) {
                throw new BusinessException("Style No is required");
            }
            if (shoeRepository.existsByStyleNo(payload.getStyleNo())) {
                throw new BusinessException("Style No already exists");
            }
            shoe = new Shoe();
        } else {
            shoe = getById(payload.getId());
            if (payload.getStyleNo() != null
                    && !payload.getStyleNo().equals(shoe.getStyleNo())
                    && shoeRepository.existsByStyleNo(payload.getStyleNo())) {
                throw new BusinessException("Style No already exists");
            }
        }
        if (payload.getName() == null || payload.getName().isBlank()) {
            throw new BusinessException("Shoe name is required");
        }
        shoe.setStyleNo(payload.getStyleNo());
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
