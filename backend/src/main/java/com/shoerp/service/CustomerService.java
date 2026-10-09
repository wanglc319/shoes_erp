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
import com.shoerp.entity.Customer;
import com.shoerp.repository.CustomerRepository;
import com.shoerp.repository.QuotationRepository;
import com.shoerp.repository.SalesOrderRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    private final SalesOrderRepository orderRepository;

    private final QuotationRepository quotationRepository;

    private final DocumentNoGenerator noGenerator;

    public CustomerService(CustomerRepository customerRepository, SalesOrderRepository orderRepository,
                           QuotationRepository quotationRepository, DocumentNoGenerator noGenerator) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.quotationRepository = quotationRepository;
        this.noGenerator = noGenerator;
    }

    @Transactional(readOnly = true)
    public PageResult<Customer> page(String keyword, String country, String level, int page, int size) {
        Specification<Customer> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("code"), like),
                        cb.like(root.get("name"), like),
                        cb.like(root.get("nameEn"), like),
                        cb.like(root.get("contactPerson"), like)));
            }
            if (country != null && !country.isBlank()) {
                predicates.add(cb.equal(root.get("country"), country));
            }
            if (level != null && !level.isBlank()) {
                predicates.add(cb.equal(root.get("level"), level));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Customer> result = customerRepository.findAll(spec, PageQuery.of(page, size));
        return PageResult.of(result);
    }

    @Transactional(readOnly = true)
    public List<Customer> listEnabled() {
        return customerRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .filter(c -> Boolean.TRUE.equals(c.getEnabled()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Customer getById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Customer not found"));
    }

    @Transactional
    public Customer save(Customer payload) {
        Customer customer;
        if (payload.getId() == null) {
            if (payload.getCode() == null || payload.getCode().isBlank()) {
                payload.setCode(noGenerator.nextCustomerCode());
            }
            if (customerRepository.existsByCode(payload.getCode())) {
                throw new BusinessException("Customer code already exists");
            }
            customer = new Customer();
        } else {
            customer = getById(payload.getId());
            if (payload.getCode() != null
                    && !payload.getCode().equals(customer.getCode())
                    && customerRepository.existsByCode(payload.getCode())) {
                throw new BusinessException("Customer code already exists");
            }
        }
        if (payload.getName() == null || payload.getName().isBlank()) {
            throw new BusinessException("Customer name is required");
        }
        customer.setCode(payload.getCode());
        customer.setName(payload.getName());
        customer.setNameEn(payload.getNameEn());
        customer.setContactPerson(payload.getContactPerson());
        customer.setPhone(payload.getPhone());
        customer.setEmail(payload.getEmail());
        customer.setAddress(payload.getAddress());
        customer.setCountry(payload.getCountry());
        customer.setLevel(payload.getLevel() == null ? "B" : payload.getLevel());
        customer.setCreditLimit(Numbers.nonNegativeOrNull(payload.getCreditLimit(), "Credit limit"));
        customer.setPaymentTerms(payload.getPaymentTerms());
        customer.setTaxNo(payload.getTaxNo());
        customer.setEnabled(payload.getEnabled() == null || payload.getEnabled());
        customer.setRemark(payload.getRemark());
        return customerRepository.save(customer);
    }

    @Transactional
    public void delete(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new BusinessException(404, "Customer not found");
        }
        if (orderRepository.existsByCustomerId(id)) {
            throw new BusinessException("Customer is referenced by sales orders and cannot be deleted");
        }
        if (quotationRepository.existsByCustomerId(id)) {
            throw new BusinessException("Customer is referenced by quotations and cannot be deleted");
        }
        customerRepository.deleteById(id);
    }
}
