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
import com.shoerp.dto.QuotationRequest;
import com.shoerp.entity.Customer;
import com.shoerp.entity.Quotation;
import com.shoerp.entity.QuotationItem;
import com.shoerp.repository.CustomerRepository;
import com.shoerp.repository.QuotationRepository;
import com.shoerp.repository.SalesOrderRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class QuotationService {

    private final QuotationRepository quotationRepository;

    private final CustomerRepository customerRepository;

    private final SalesOrderRepository orderRepository;

    private final DocumentNoGenerator noGenerator;

    public QuotationService(QuotationRepository quotationRepository, CustomerRepository customerRepository,
                            SalesOrderRepository orderRepository, DocumentNoGenerator noGenerator) {
        this.quotationRepository = quotationRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
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
        Page<Quotation> result = quotationRepository.findAll(spec, PageQuery.of(page, size));
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
        if (request.getId() != null
                && !quoteNo.equals(quotation.getQuoteNo())
                && quotationRepository.existsByQuoteNo(quoteNo)) {
            throw new BusinessException("Quotation number already exists: " + quoteNo);
        }

        Customer customer = resolveCustomer(request.getCustomerId());

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Quotation must contain at least one item");
        }

        quotation.setQuoteNo(quoteNo);
        quotation.setCustomerId(customer.getId());
        quotation.setCustomerName(customer.getName());
        quotation.setQuoteDate(parseDate(request.getQuoteDate(), LocalDate.now()));
        quotation.setValidUntil(parseDate(request.getValidUntil(), null));
        quotation.setTradeTerms(request.getTradeTerms());
        quotation.setCurrency(request.getCurrency() == null ? "USD" : request.getCurrency());
        quotation.setPaymentTerms(request.getPaymentTerms());
        quotation.setStatus(request.getStatus() == null ? "DRAFT" : StatusCatalog.require(request.getStatus(), StatusCatalog.QUOTATION, "Status"));
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
                item.setQuantity(Numbers.nonNegative(payload.getQuantity(), "Quantity"));
                item.setUnit(payload.getUnit() == null ? "PRS" : payload.getUnit());
                item.setUnitPrice(Numbers.nonNegative(payload.getUnitPrice(), "Unit price"));
                item.setCostPrice(Numbers.nonNegativeOrNull(payload.getCostPrice(), "Cost price"));
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
        if (orderRepository.existsByQuotationId(id)) {
            throw new BusinessException("Quotation is referenced by sales orders and cannot be deleted");
        }
        quotationRepository.deleteById(id);
    }

    @Transactional
    public Quotation updateStatus(Long id, String status) {
        Quotation quotation = getById(id);
        quotation.setStatus(StatusCatalog.require(status, StatusCatalog.QUOTATION, "Status"));
        return quotationRepository.save(quotation);
    }

    private Customer resolveCustomer(Long customerId) {
        if (customerId == null) {
            throw new BusinessException("Customer is required");
        }
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(404, "Customer not found"));
    }

    private LocalDate parseDate(String value, LocalDate defaultValue) {
        return Dates.parse(value, defaultValue);
    }

    private BigDecimal scale(BigDecimal value) {
        return Numbers.scale(value);
    }
}
