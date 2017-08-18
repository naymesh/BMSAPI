package org.ibp.api.rest.sample;

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

@Api(value = "Sample Services")
@Controller
@RequestMapping("/sample")
public class SampleListResource {

	public static final String NULL = "null";
	public static final String ERROR = "ERROR";
	@Autowired
	public SampleListService sampleListService;

	@ApiOperation(value = "Create sample list", notes = "Create sample list. ")
	@RequestMapping(value = "/{crop}/sampleList", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Map<String, Object>> createSampleList(@PathVariable final String crop, @RequestBody final SampleListDto dto) {
		dto.setCropName(crop);
		final Map<String, Object> map = this.sampleListService.createSampleList(dto);

		if (map.get(SampleListResource.ERROR) != null || SampleListResource.NULL.equals(map.get("id"))) {
			return new ResponseEntity<>(map, HttpStatus.CONFLICT);
		}
		return new ResponseEntity<>(map, HttpStatus.OK);
	}

	@ApiOperation(value = "Create sample list folder", notes = "Create sample list folder. ")
	@RequestMapping(value = "/{crop}/sampleListFolder", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Map<String, Object>> createSampleListFolder(@RequestBody final String folderName,
		@RequestBody final Integer parentId) {

		final Map<String, Object> map = this.sampleListService.createSampleListFolder(folderName, parentId);

		if (map.get(SampleListResource.ERROR) != null || SampleListResource.NULL.equals(map.get("id"))) {
			return new ResponseEntity<>(map, HttpStatus.CONFLICT);
		}
		return new ResponseEntity<>(map, HttpStatus.OK);
	}

	@ApiOperation(value = "Update sample list folder", notes = "Update sample list folder. ")
	@RequestMapping(value = "/{crop}/sampleListFolder", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<Map<String, Object>> updateSampleListFolderName(@RequestBody final Integer folderId,
		@RequestBody final String newFolderName) {
		Map<String, Object> map = this.sampleListService.updateSampleListFolderName(folderId, newFolderName);
		if (map.get("ERROR") != null) {
			return new ResponseEntity<>(map, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(map, HttpStatus.OK);
	}

	@ApiOperation(value = "Move sample list folder", notes = "Move sample list folder. ")
	@RequestMapping(value = "/{crop}/sampleListFolder/move", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<Map<String, Object>> moveSampleListFolder(@RequestBody final Integer folderId,
		@RequestBody final Integer newParentId) {
		Map<String, Object> map = this.sampleListService.moveSampleListFolder(folderId, newParentId);
		if (map.get("ERROR") != null) {
			return new ResponseEntity<>(map, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(map, HttpStatus.OK);
	}

	@ApiOperation(value = "Delete sample list folder", notes = "Delete sample list folder. ")
	@RequestMapping(value = "/{crop}/sampleListFolder/delete/{folderId}", method = RequestMethod.PUT)
	@ResponseBody
	public ResponseEntity<Map<String, Object>> deleteSampleListFolder(@PathVariable final Integer folderId) {
		Map<String, Object> map = this.sampleListService.deleteSampleListFolder(folderId);
		if (map.get("ERROR") != null) {
			return new ResponseEntity<>(map, HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(map, HttpStatus.OK);
	}
}
