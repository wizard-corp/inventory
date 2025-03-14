package com.pat.inventory.application.usecase.product;

import com.pat.inventory.domain.entities.Product;
import com.pat.inventory.domain.shared.exceptions.DomainException;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ProductRowMapper implements RowMapper<Product> {
    public Product mapRow(ResultSet rs, int rowNum) throws SQLException {
        try {
            return Product.existing(
                    rs.getString("product_id"),
                    rs.getString("description"),
                    rs.getInt("status_value")
            );
        } catch (DomainException e) {
            throw new RuntimeException("Error creating Product from database row for product_id: "
                    + rs.getString("product_id"), e);
        }
    }
}