package org.ibp.api.brapi.v1.sample;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * Prototype BrAPI call to support sample tracking and related analysis in systems such as GOBII and phenotype/genotype correlation tools.
 *
 * @author Naymesh Mistry
 */
@Api(value = "BrAPI Sample Services")
@Controller
public class SampleListResourceBrapi {

	@Autowired public SampleListService sampleListService;

	@ApiOperation(value = "Create sample list", notes = "Create sample list. ")
	@RequestMapping(value = "/{crop}/brapi/v1/sampleList", method = RequestMethod.POST) @ResponseBody
	public ResponseEntity<Map<String, Object>> createUser(@PathVariable final String crop, @RequestBody SampleListDto dto) {
		dto.setCropName(crop);
		final Map<String, Object> map = this.sampleListService.createSampleList(dto);

		if (map.get("ERROR") != null) {
			return new ResponseEntity<Map<String, Object>>(map, HttpStatus.CONFLICT);
		}
		return new ResponseEntity<Map<String, Object>>(map, HttpStatus.CREATED);
	}
}
