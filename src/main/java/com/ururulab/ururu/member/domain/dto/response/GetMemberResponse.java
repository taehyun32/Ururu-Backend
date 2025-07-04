package com.ururulab.ururu.member.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ururulab.ururu.member.domain.entity.Member;

import java.time.LocalDate;
import java.time.ZonedDateTime;

public record GetMemberResponse(
        Long id,
        String email,
        String nickname,
        String gender,
        LocalDate birth,
        String phone,
        @JsonProperty("profile_image") String profileImage,
        @JsonProperty("social_provider") String socialProvider,
        String role,
        int point,
        @JsonProperty("is_deleted") boolean isDeleted,
        @JsonProperty("created_at") ZonedDateTime createdAt,
        @JsonProperty("updated_at") ZonedDateTime updatedAt
) {
    public static GetMemberResponse from(final Member member) {
        return new GetMemberResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getGender() != null ? member.getGender().name() : null,
                member.getBirth(),
                member.getPhone(),
                member.getProfileImage(),
                member.getSocialProvider().name(),
                member.getRole().name(),
                member.getPoint(),
                member.isDeleted(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
