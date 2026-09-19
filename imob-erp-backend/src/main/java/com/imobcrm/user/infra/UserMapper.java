package com.imobcrm.user.infra;

import com.imobcrm.user.api.UserResponse;
import com.imobcrm.user.domain.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponseDTO(User user);
}
