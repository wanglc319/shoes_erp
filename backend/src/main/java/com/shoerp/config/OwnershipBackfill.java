package com.shoerp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 历史数据归属人回填：引入 owner_id 之前的数据统一归管理员（id=1），
 * 避免升级后旧数据在普通用户视角下"消失"。
 */
@Component
@Order(100)
public class OwnershipBackfill implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(OwnershipBackfill.class);

    private static final String[] TABLES = {
            "customer", "quotation", "sales_order", "shipment", "purchase_order", "payment"
    };

    private final JdbcTemplate jdbcTemplate;

    public OwnershipBackfill(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        for (String table : TABLES) {
            try {
                int updated = jdbcTemplate.update(
                        "UPDATE " + table + " SET owner_id = 1 WHERE owner_id IS NULL");
                if (updated > 0) {
                    log.info("Backfilled {} rows of {} with owner_id=1", updated, table);
                }
            } catch (Exception ex) {
                log.warn("Ownership backfill skipped for {}: {}", table, ex.getMessage());
            }
        }
    }
}
