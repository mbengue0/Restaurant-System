package com.daust.restaurant.infrastructure.persistence;

import com.daust.restaurant.domain.User;
import com.daust.restaurant.domain.UserId;

final class UserMapper {

    private UserMapper() {
    }

    static User toDomain(UserJpaEntity entity) {
        return User.reconstitute(
                UserId.of(entity.getId()),
                entity.getUsername(),
                entity.getHashedPassword(),
                entity.getFullName(),
                entity.getRole(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getLastLoginAt(),
                entity.isMustChangePassword());
    }

    static UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(
                user.getId().value(),
                user.getUsername(),
                user.getHashedPassword(),
                user.getFullName(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt(),
                user.getLastLoginAt(),
                user.isMustChangePassword());
    }
}
