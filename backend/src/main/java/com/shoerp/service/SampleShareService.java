package com.shoerp.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shoerp.common.BusinessException;
import com.shoerp.dto.PublicAlbum;
import com.shoerp.dto.SampleShareRequest;
import com.shoerp.entity.SampleShare;
import com.shoerp.entity.Shoe;
import com.shoerp.entity.ShoeImage;
import com.shoerp.repository.SampleShareRepository;
import com.shoerp.security.CurrentUser;

@Service
public class SampleShareService {

    private static final int MAX_SHOES = 200;

    private final SampleShareRepository repository;

    private final ShoeService shoeService;

    public SampleShareService(SampleShareRepository repository, ShoeService shoeService) {
        this.repository = repository;
        this.shoeService = shoeService;
    }

    @Transactional
    public SampleShare create(SampleShareRequest request) {
        if (request.getShoeIds() == null || request.getShoeIds().isEmpty()) {
            throw new BusinessException("Select at least one sample");
        }
        Set<Long> ids = new LinkedHashSet<>(request.getShoeIds());
        ids.removeIf(id -> id == null);
        if (ids.isEmpty()) {
            throw new BusinessException("Select at least one sample");
        }
        if (ids.size() > MAX_SHOES) {
            throw new BusinessException("A single album supports up to " + MAX_SHOES + " samples");
        }
        SampleShare share = new SampleShare();
        share.setToken(UUID.randomUUID().toString().replace("-", ""));
        share.setTitle(request.getTitle() == null || request.getTitle().isBlank()
                ? "Sample Album" : request.getTitle().trim());
        share.setShoeIds(ids.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(""));
        Integer days = request.getExpireDays();
        share.setExpireAt(days == null || days <= 0 ? null : LocalDate.now().plusDays(days));
        share.setEnabled(true);
        share.setRemark(request.getRemark());
        share.setOwnerId(CurrentUser.id());
        return repository.save(share);
    }

    @Transactional(readOnly = true)
    public List<SampleShare> list() {
        return CurrentUser.isAdmin()
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findByOwnerIdOrderByCreatedAtDesc(CurrentUser.id());
    }

    @Transactional
    public void delete(Long id) {
        SampleShare share = repository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Album not found"));
        if (!CurrentUser.owns(share.getOwnerId())) {
            throw new BusinessException(403, "You can only delete your own albums");
        }
        repository.delete(share);
    }

    /**
     * 公开访问：校验启用状态与有效期，只返回展示字段。
     */
    @Transactional(readOnly = true)
    public PublicAlbum publicView(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(404, "Album not found");
        }
        SampleShare share = repository.findByToken(token.trim())
                .orElseThrow(() -> new BusinessException(404, "Album not found"));
        if (!Boolean.TRUE.equals(share.getEnabled())) {
            throw new BusinessException(404, "Album has been disabled");
        }
        if (share.getExpireAt() != null && share.getExpireAt().isBefore(LocalDate.now())) {
            throw new BusinessException(410, "Album link has expired");
        }
        List<Long> ids = new ArrayList<>();
        Arrays.stream(share.getShoeIds().split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .forEach(value -> {
                    try {
                        ids.add(Long.valueOf(value));
                    } catch (NumberFormatException ignored) {
                        // 忽略脏数据
                    }
                });
        List<PublicAlbum.PublicShoe> shoes = new ArrayList<>();
        for (Shoe shoe : shoeService.listByIds(ids)) {
            shoes.add(new PublicAlbum.PublicShoe(
                    shoe.getStyleNo(),
                    shoe.getName(),
                    shoe.getNameEn(),
                    shoe.getCategory(),
                    shoe.getGender(),
                    shoe.getUpperMaterial(),
                    shoe.getSoleMaterial(),
                    shoe.getColor(),
                    shoe.getSizeRange(),
                    shoe.getDescription(),
                    imageUrls(shoe)));
        }
        return new PublicAlbum(share.getTitle(), share.getExpireAt(), shoes);
    }

    private List<String> imageUrls(Shoe shoe) {
        List<String> urls = new ArrayList<>();
        for (ShoeImage image : shoe.getImages()) {
            if (image.getImageUrl() != null && !image.getImageUrl().isBlank()) {
                urls.add(image.getImageUrl());
            }
        }
        if (urls.isEmpty() && shoe.getImageUrl() != null && !shoe.getImageUrl().isBlank()) {
            urls.add(shoe.getImageUrl());
        }
        return urls;
    }
}
