package com.ururulab.ururu.product.domain.dto.request;

public record ProductImageUploadRequest(
        Long productOptionId,
        String originalFilename,
        byte[] data
) {
}
