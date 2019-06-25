package org.ibp.api.java.role;

import org.generationcp.middleware.service.api.user.RoleSearchDto;
import org.ibp.api.domain.role.RoleDto;

import java.util.List;

public interface RoleService {
	
	List<RoleDto> getAllRoles();

	List<RoleDto> getRoles(RoleSearchDto roleSearchDto);

}
