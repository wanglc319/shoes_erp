package com.shoerp.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shoerp.common.BusinessException;
import com.shoerp.common.PageResult;
import com.shoerp.dto.QuotationRequest;
import com.shoerp.entity.Customer;
import com.shoerp.entity.Quotation;
import com.shoerp.entity.QuotationItem;
import com.shoerp.repository.CustomerRepository;
import com.shoerp.repository.QuotationRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class QuotationService {

    private final QuotationRepository quotationRepository;

    private final CustomerRepository customerRepository;

    private final DocumentNoGenerator noGenerator;

    public QuotationService(QuotationRepository quotationRepository, CustomerRepository customerRepository,
                            DocumentNoGenerator noGenerator) {
        this.quotationRepository = quotationRepository;
        this.customerRepository = customerRepository;
        this.noGenerator = noGenerator;
    }

    @Transactional(readOnly = true)
    public PageResult<Quotation> page(String keyword, String status, Long customerId, int page, int size) {
        Specification<Quotation> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("quoteNo"), like),
                        cb.like(root.get("customerName"), like)));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customerId"), customerId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Quotation> result = quotationRepository.findAll(spec,
                PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "id")));
        result.getContent().forEach(q -> q.getItems().size());
        return PageResult.of(result);
    }

    @Transactional(readOnly = true)
    public Quotation getById(Long id) {
        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Quotation not found"));
        quotation.getItems().size();
        return quotation;
    }

    @Transactional
    public Quotation save(QuotationRequest request) {
        Quotation quotation = request.getId() == null
                ? new Quotation()
                : quotationRepository.findById(request.getId())
                        .orElseThrow(() -> new BusinessException(404, "Quotation not found"));

        String quoteNo = request.getQuoteNo();
        if (quoteNo == null || quoteNo.isBlank()) {
            quoteNo = noGenerator.nextQuotationNo();
        }
        if (request.getId() == null && quotationRepository.existsByQuoteNo(quoteNo)) {
            throw new BusinessException("Quotation number already exists: " + quoteNo);
        }

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new BusinessException(404, "Customer not found"));

        quotation.setQuoteNo(quoteNo);
        quotation.setCustomerId(customer.getId());
        quotation.setCustomerName(customer.getName());
        quotation.setQuoteDate(parseDate(request.getQuoteDate(), LocalDate.now()));
        quotation.setValidUntil(parseDate(request.getValidUntil(), null));
        quotation.setTradeTerms(request.getTradeTerms());
        quotation.setCurrency(request.getCurrency() == null ? "USD" : request.getCurrency());
        quotation.setPaymentTerms(request.getPaymentTerms());
        quotation.setStatus(request.getStatus() == null ? "DRAFT" : request.getStatus());
        quotation.setSalesOwner(request.getSalesOwner());
        quotation.setRemark(request.getRemark());

        quotation.getItems().clear();
        BigDecimal total = BigDecimal.ZERO;
        if (request.getItems() != null) {
            for (QuotationRequest.ItemPayload payload : request.getItems()) {
                QuotationItem item = new QuotationItem();
                item.setQuotation(quotation);
                item.setShoeId(payload.getShoeId());
                item.setStyleNo(payload.getStyleNo());
                item.setProductName(payload.getProductName());
                item.setMaterial(payload.getMaterial());
                item.setColor(payload.getColor());
                item.setSizeRange(payload.getSizeRange());
                item.setQuantity(payload.getQuantity() == null ? 0 : payload.getQuantity());
                item.setUnit(payload.getUnit() == null ? "PRS" : payload.getUnit());
                item.setUnitPrice(scale(payload.getUnitPrice()));
                item.setCostPrice(payload.getCostPrice());
                item.setRemark(payload.getRemark());
                BigDecimal amount = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                item.setAmount(scale(amount));
                total = total.add(item.getAmount());
                quotation.getItems().add(item);
            }
        }
        quotation.setTotalAmount(scale(total));
        return quotationRepository.save(quotation);
    }

    @Transactional
    public void delete(Long id) {
        if (!quotationRepository.existsById(id)) {
            throw new BusinessException(404, "Quotation not found");
        }
        quotationRepository.deleteById(id);
    }

    @Transactional
    public Quotation updateStatus(Long id, String status) {
        Quotation quotation = getById(id);
        quotation.setStatus(status);
        return quotationRepository.save(quotation);
    }

    private LocalDate parseDate(String value, LocalDate defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return LocalDate.parse(value.substring(0, 10));
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
