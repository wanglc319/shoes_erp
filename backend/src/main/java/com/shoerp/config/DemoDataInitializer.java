package com.shoerp.config;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.shoerp.dto.OrderRequest;
import com.shoerp.dto.QuotationRequest;
import com.shoerp.dto.ShipmentRequest;
import com.shoerp.entity.Customer;
import com.shoerp.entity.Shoe;
import com.shoerp.entity.Supplier;
import com.shoerp.entity.User;
import com.shoerp.repository.CustomerRepository;
import com.shoerp.repository.ShoeRepository;
import com.shoerp.repository.SupplierRepository;
import com.shoerp.repository.UserRepository;
import com.shoerp.service.OrderService;
import com.shoerp.service.PermissionService;
import com.shoerp.service.QuotationService;
import com.shoerp.service.ShipmentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataInitializer implements CommandLineRunner {

    private final AppProperties appProperties;

    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    private final SupplierRepository supplierRepository;

    private final CustomerRepository customerRepository;

    private final ShoeRepository shoeRepository;

    private final QuotationService quotationService;

    private final OrderService orderService;

    private final ShipmentService shipmentService;

    private final PermissionService permissionService;

    @Override
    public void run(String... args) {
        if (!appProperties.isInitDemoData()) {
            return;
        }
        permissionService.seedDefaults();
        if (userRepository.count() > 0) {
            return;
        }
        log.info("Initializing demo data...");
        seedUsers();
        List<Supplier> suppliers = seedSuppliers();
        List<Customer> customers = seedCustomers();
        List<Shoe> shoes = seedShoes(suppliers);
        seedDocuments(customers, shoes);
        log.info("Demo data initialized. Login: admin / admin123");
    }

    private void seedUsers() {
        user("admin", "admin123", "系统管理员", "ADMIN", "admin@shoerp.com");
        user("sales", "123456", "王晓琳", "SALES", "sales@shoerp.com");
        user("ops", "123456", "陈志远", "OPS", "ops@shoerp.com");
        user("finance", "123456", "李嘉宁", "FINANCE", "finance@shoerp.com");
    }

    private void user(String username, String password, String realName, String role, String email) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setRole(role);
        user.setEmail(email);
        user.setEnabled(true);
        userRepository.save(user);
    }

    private List<Supplier> seedSuppliers() {
        return List.of(
                supplier("SUP-FJ-001", "泉州远航鞋业", "Quanzhou Yuanhang Footwear", "林建国",
                        "China", "Factory", 5, "30% deposit, 70% before shipment"),
                supplier("SUP-GD-002", "东莞锐步模具", "Dongguan Ruibu Mould", "赵敏",
                        "China", "Mould", 4, "Net 30"),
                supplier("SUP-ZJ-003", "温州博雅皮料", "Wenzhou Boya Leather", "周海涛",
                        "China", "Material", 4, "TT in advance"),
                supplier("SUP-VN-004", "Hai Phong Shoes Co.", "Hai Phong Shoes Co.", "Nguyen Linh",
                        "Vietnam", "Factory", 5, "FOB Haiphong, 30/70")
        );
    }

    private Supplier supplier(String code, String name, String nameEn, String contact, String country,
                              String type, int rating, String paymentTerms) {
        Supplier s = new Supplier();
        s.setCode(code);
        s.setName(name);
        s.setNameEn(nameEn);
        s.setContactPerson(contact);
        s.setPhone("86-13800000000");
        s.setEmail(code.toLowerCase() + "@supplier.com");
        s.setCountry(country);
        s.setType(type);
        s.setRating(rating);
        s.setPaymentTerms(paymentTerms);
        s.setEnabled(true);
        return supplierRepository.save(s);
    }

    private List<Customer> seedCustomers() {
        return List.of(
                customer("CUS-DE-001", "Nordic Step GmbH", "Nordic Step GmbH", "Anna Keller",
                        "Germany", "A", "T/T 30% deposit, 70% BL copy", new BigDecimal("180000")),
                customer("CUS-US-002", "Pacific Trail Inc.", "Pacific Trail Inc.", "James Miller",
                        "USA", "A", "L/C at sight", new BigDecimal("250000")),
                customer("CUS-UK-003", "Harbor Walk Ltd.", "Harbor Walk Ltd.", "Emily Wright",
                        "UK", "B", "T/T 40/60", new BigDecimal("90000")),
                customer("CUS-AU-004", "Outback Gear Pty", "Outback Gear Pty", "Tom Harris",
                        "Australia", "B", "FOB Ningbo, T/T", new BigDecimal("70000")),
                customer("CUS-AE-005", "Desert Line Trading", "Desert Line Trading", "Omar Al Farsi",
                        "UAE", "C", "CAD", new BigDecimal("40000"))
        );
    }

    private Customer customer(String code, String name, String nameEn, String contact, String country,
                              String level, String paymentTerms, BigDecimal credit) {
        Customer c = new Customer();
        c.setCode(code);
        c.setName(name);
        c.setNameEn(nameEn);
        c.setContactPerson(contact);
        c.setPhone("+1-555-0100");
        c.setEmail(code.toLowerCase() + "@buyer.com");
        c.setCountry(country);
        c.setLevel(level);
        c.setPaymentTerms(paymentTerms);
        c.setCreditLimit(credit);
        c.setEnabled(true);
        return customerRepository.save(c);
    }

    private List<Shoe> seedShoes(List<Supplier> suppliers) {
        Long factory = suppliers.get(0).getId();
        Long vn = suppliers.get(3).getId();
        return List.of(
                shoe("SN-24018", "城市通勤运动鞋", "Urban Commute Sneaker", "Sneaker", "Unisex",
                        "Mesh + PU", "EVA + Rubber", "White/Black", "36-45",
                        new BigDecimal("8.20"), new BigDecimal("12.80"), 600, factory),
                shoe("SN-24026", "轻量跑鞋", "Lite Run Shoe", "Running", "Men",
                        "Knit upper", "Phylon + Rubber", "Neon Green", "39-46",
                        new BigDecimal("9.50"), new BigDecimal("14.60"), 800, factory),
                shoe("BT-23011", "工装马丁靴", "Work Martin Boot", "Boot", "Men",
                        "Crazy Horse Leather", "TPR", "Brown", "39-46",
                        new BigDecimal("14.80"), new BigDecimal("21.50"), 400, factory),
                shoe("SD-25007", "女士夏季凉鞋", "Ladies Summer Sandal", "Sandal", "Women",
                        "PU + Mesh", "EVA", "Beige", "35-41",
                        new BigDecimal("4.60"), new BigDecimal("7.90"), 1000, vn),
                shoe("CS-24102", "儿童休闲鞋", "Kids Casual Shoe", "Casual", "Kids",
                        "Synthetic leather", "EVA", "Navy/Red", "27-35",
                        new BigDecimal("5.10"), new BigDecimal("8.40"), 1200, vn),
                shoe("HK-24088", "高帮篮球鞋", "High-top Basketball Shoe", "Basketball", "Men",
                        "Synthetic leather + Mesh", "Rubber", "Black/Gold", "40-47",
                        new BigDecimal("11.20"), new BigDecimal("17.80"), 500, factory)
        );
    }

    private Shoe shoe(String styleNo, String name, String nameEn, String category, String gender,
                      String upper, String sole, String color, String sizeRange,
                      BigDecimal cost, BigDecimal sell, int moq, Long supplierId) {
        Shoe s = new Shoe();
        s.setStyleNo(styleNo);
        s.setName(name);
        s.setNameEn(nameEn);
        s.setCategory(category);
        s.setGender(gender);
        s.setUpperMaterial(upper);
        s.setSoleMaterial(sole);
        s.setColor(color);
        s.setSizeRange(sizeRange);
        s.setCostPrice(cost);
        s.setSellingPrice(sell);
        s.setMoq(moq);
        s.setSupplierId(supplierId);
        s.setStatus("ACTIVE");
        s.setEnabled(true);
        return shoeRepository.save(s);
    }

    private void seedDocuments(List<Customer> customers, List<Shoe> shoes) {
        Shoe sneaker = shoes.get(0);
        Shoe run = shoes.get(1);
        Shoe boot = shoes.get(2);
        Shoe sandal = shoes.get(3);
        Shoe kids = shoes.get(4);

        QuotationRequest q1 = quote("QT2026030001", customers.get(0).getId(), "2026-03-12", "2026-04-12",
                "FOB Ningbo", "USD", customers.get(0).getPaymentTerms(), "SENT", "王晓琳",
                List.of(qItem(sneaker, 1200), qItem(run, 800)));
        quotationService.save(q1);

        QuotationRequest q2 = quote("QT2026040002", customers.get(1).getId(), "2026-04-08", "2026-05-08",
                "CIF Los Angeles", "USD", customers.get(1).getPaymentTerms(), "WON", "王晓琳",
                List.of(qItem(boot, 600), qItem(sneaker, 1500)));
        quotationService.save(q2);

        QuotationRequest q3 = quote("QT2026050003", customers.get(2).getId(), "2026-05-20", "2026-06-20",
                "FOB Shenzhen", "USD", customers.get(2).getPaymentTerms(), "DRAFT", "王晓琳",
                List.of(qItem(sandal, 2000), qItem(kids, 1600)));
        quotationService.save(q3);

        OrderRequest o1 = order("SO2026040101", customers.get(1).getId(), "2026-04-18", "2026-07-15",
                "CIF Los Angeles", "USD", customers.get(1).getPaymentTerms(), "CONFIRMED", "IN_PRODUCTION",
                "王晓琳", new BigDecimal("12000"),
                List.of(oItem(boot, 600), oItem(sneaker, 1500)));
        var savedOrder1 = orderService.save(o1);

        OrderRequest o2 = order("SO2026050202", customers.get(0).getId(), "2026-05-06", "2026-08-01",
                "FOB Ningbo", "USD", customers.get(0).getPaymentTerms(), "PARTIAL_SHIPPED", "COMPLETED",
                "王晓琳", new BigDecimal("8000"),
                List.of(oItem(sneaker, 1200), oItem(run, 800)));
        var savedOrder2 = orderService.save(o2);

        OrderRequest o3 = order("SO2026060303", customers.get(3).getId(), "2026-06-02", "2026-09-10",
                "FOB Ningbo", "USD", customers.get(3).getPaymentTerms(), "PENDING", "NOT_STARTED",
                "王晓琳", new BigDecimal("3000"),
                List.of(oItem(sandal, 1800)));
        orderService.save(o3);

        ShipmentRequest s1 = ship("SH2026070001", savedOrder2.getId(), "2026-07-12", "MSKU7788123",
                "COSU1234567890", "COSCO PACIFIC", "Ningbo", "Hamburg", "DEPARTED",
                List.of(sItem(sneaker, 1200, 100, new BigDecimal("1480"), new BigDecimal("1320"))));
        shipmentService.save(s1);

        ShipmentRequest s2 = ship("SH2026080002", savedOrder1.getId(), "2026-08-05", "TCLU5566778",
                "MAEU9876543210", "MAERSK SEOUL", "Ningbo", "Los Angeles", "PREPARING",
                List.of(sItem(boot, 600, 50, new BigDecimal("980"), new BigDecimal("860"))));
        shipmentService.save(s2);
    }

    private QuotationRequest quote(String no, Long customerId, String date, String validUntil,
                                   String terms, String currency, String payment, String status,
                                   String owner, List<QuotationRequest.ItemPayload> items) {
        QuotationRequest request = new QuotationRequest();
        request.setQuoteNo(no);
        request.setCustomerId(customerId);
        request.setQuoteDate(date);
        request.setValidUntil(validUntil);
        request.setTradeTerms(terms);
        request.setCurrency(currency);
        request.setPaymentTerms(payment);
        request.setStatus(status);
        request.setSalesOwner(owner);
        request.setItems(items);
        return request;
    }

    private QuotationRequest.ItemPayload qItem(Shoe shoe, int qty) {
        QuotationRequest.ItemPayload item = new QuotationRequest.ItemPayload();
        item.setShoeId(shoe.getId());
        item.setStyleNo(shoe.getStyleNo());
        item.setProductName(shoe.getNameEn());
        item.setMaterial(shoe.getUpperMaterial());
        item.setColor(shoe.getColor());
        item.setSizeRange(shoe.getSizeRange());
        item.setQuantity(qty);
        item.setUnit("PRS");
        item.setUnitPrice(shoe.getSellingPrice());
        item.setCostPrice(shoe.getCostPrice());
        return item;
    }

    private OrderRequest order(String no, Long customerId, String date, String delivery,
                               String terms, String currency, String payment, String status,
                               String production, String owner, BigDecimal deposit,
                               List<OrderRequest.ItemPayload> items) {
        OrderRequest request = new OrderRequest();
        request.setOrderNo(no);
        request.setCustomerId(customerId);
        request.setOrderDate(date);
        request.setDeliveryDate(delivery);
        request.setTradeTerms(terms);
        request.setCurrency(currency);
        request.setPaymentTerms(payment);
        request.setStatus(status);
        request.setProductionStatus(production);
        request.setSalesOwner(owner);
        request.setDepositAmount(deposit);
        request.setItems(items);
        return request;
    }

    private OrderRequest.ItemPayload oItem(Shoe shoe, int qty) {
        OrderRequest.ItemPayload item = new OrderRequest.ItemPayload();
        item.setShoeId(shoe.getId());
        item.setStyleNo(shoe.getStyleNo());
        item.setProductName(shoe.getNameEn());
        item.setColor(shoe.getColor());
        item.setSizeRange(shoe.getSizeRange());
        item.setQuantity(qty);
        item.setUnit("PRS");
        item.setUnitPrice(shoe.getSellingPrice());
        item.setCostPrice(shoe.getCostPrice());
        return item;
    }

    private ShipmentRequest ship(String no, Long orderId, String date, String container, String bl,
                                 String vessel, String pol, String pod, String status,
                                 List<ShipmentRequest.ItemPayload> items) {
        ShipmentRequest request = new ShipmentRequest();
        request.setShipmentNo(no);
        request.setOrderId(orderId);
        request.setShipDate(date);
        request.setContainerNo(container);
        request.setBlNo(bl);
        request.setVessel(vessel);
        request.setPortOfLoading(pol);
        request.setPortOfDestination(pod);
        request.setStatus(status);
        request.setItems(items);
        return request;
    }

    private ShipmentRequest.ItemPayload sItem(Shoe shoe, int qty, int cartons,
                                              BigDecimal gw, BigDecimal nw) {
        ShipmentRequest.ItemPayload item = new ShipmentRequest.ItemPayload();
        item.setStyleNo(shoe.getStyleNo());
        item.setProductName(shoe.getNameEn());
        item.setColor(shoe.getColor());
        item.setSizeRange(shoe.getSizeRange());
        item.setQuantity(qty);
        item.setUnit("PRS");
        item.setCartons(cartons);
        item.setGrossWeight(gw);
        item.setNetWeight(nw);
        item.setUnitPrice(shoe.getSellingPrice());
        return item;
    }
}
