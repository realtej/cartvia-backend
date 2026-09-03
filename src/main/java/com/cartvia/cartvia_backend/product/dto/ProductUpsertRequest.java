package com.cartvia.cartvia_backend.product.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class ProductUpsertRequest {

    @NotBlank
    @Size(max = 50)
    private String barcode;

    @NotBlank
    private String name;

    private String description;

    private String category;

    @NotNull
    @PositiveOrZero
    private BigDecimal price;

    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal discountPct;

    @URL
    private String imageUrl;

    @PositiveOrZero
    private BigDecimal expectedWeightG;

    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal weightTolerancePct;

    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal gstSlabPct;

    private UUID storeId;
}
