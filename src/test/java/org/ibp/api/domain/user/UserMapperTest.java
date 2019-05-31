package org.ibp.api.domain.user;

import org.generationcp.middleware.service.api.user.UserDto;
import org.ibp.api.java.impl.middleware.UserTestDataGenerator;
import org.ibp.api.java.impl.middleware.user.UserServiceTest;
import org.junit.Test;
import org.modelmapper.ModelMapper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class UserMapperTest extends UserServiceTest {

	@Test
	public void userDetailsMapperTest() {
		final ModelMapper mapper = UserMapper.getInstance();
		final UserDto userDto = UserTestDataGenerator.initializeUserDto(1);
		final UserDetailDto userDetailDto = mapper.map(userDto, UserDetailDto.class);
		System.out.println(userDetailDto);

		assertThat(userDto.getFirstName(), equalTo(userDetailDto.getFirstName()));
		assertThat(userDto.getLastName(), equalTo(userDetailDto.getLastName()));
		assertThat(userDto.getUserId(), equalTo(userDetailDto.getId()));
		assertThat(userDto.getUsername(), equalTo(userDetailDto.getUsername()));
		assertThat(userDto.getRole().getName(), equalTo(userDetailDto.getRole().getName()));
		assertThat((userDto.getStatus() == 0 ? "true" : "false"), equalTo(userDetailDto.getStatus()));
		assertThat(userDto.getEmail(), equalTo(userDetailDto.getEmail()));

	}
}
