package com.shoerp.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shoerp.common.BusinessException;
import com.shoerp.common.PageResult;
import com.shoerp.entity.User;
import com.shoerp.repository.UserRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResult<User> page(String keyword, String role, int page, int size) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("username"), like),
                        cb.like(root.get("realName"), like),
                        cb.like(root.get("email"), like)));
            }
            if (role != null && !role.isBlank()) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<User> result = userRepository.findAll(spec,
                PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "id")));
        result.getContent().forEach(u -> u.setPassword(null));
        return PageResult.of(result);
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "User not found"));
        user.setPassword(null);
        return user;
    }

    @Transactional
    public User save(User payload) {
        User user;
        if (payload.getId() == null) {
            if (userRepository.existsByUsername(payload.getUsername())) {
                throw new BusinessException("Username already exists");
            }
            user = new User();
            String raw = payload.getPassword() == null || payload.getPassword().isBlank()
                    ? "123456" : payload.getPassword();
            user.setPassword(passwordEncoder.encode(raw));
        } else {
            user = userRepository.findById(payload.getId())
                    .orElseThrow(() -> new BusinessException(404, "User not found"));
            if (payload.getPassword() != null && !payload.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(payload.getPassword()));
            }
        }
        user.setUsername(payload.getUsername());
        user.setRealName(payload.getRealName());
        user.setEmail(payload.getEmail());
        user.setPhone(payload.getPhone());
        user.setRole(payload.getRole() == null ? "SALES" : payload.getRole());
        user.setEnabled(payload.getEnabled() == null || payload.getEnabled());
        user.setRemark(payload.getRemark());
        User saved = userRepository.save(user);
        saved.setPassword(null);
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "User not found"));
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new BusinessException("Cannot delete the default admin");
        }
        userRepository.deleteById(id);
    }
}
