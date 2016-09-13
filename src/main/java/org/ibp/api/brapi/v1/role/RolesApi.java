
package org.ibp.api.brapi.v1.role;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Api(value = "BrAPI Role Services")
@Controller
public class RolesApi {

	@Autowired
	private RoleService roleService;

	@ApiOperation(value = "List all roles", notes = "List all roles in this deployment instance of BMSAPI. ")
	@RequestMapping(value = "brapi/v1/roles", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<List<RoleData>> listRoles() {
		return new ResponseEntity<List<RoleData>>(this.roleService.getAllRoles(), HttpStatus.OK);
	}

}
