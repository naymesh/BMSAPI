package org.generationcp.bms.ontology.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.generationcp.bms.ontology.validator.NotNull;
import org.generationcp.bms.ontology.validator.UniqueField;

public class MethodRequest {

    @UniqueField(fields = "@Fields:name")
    @NotNull(fields = "@Fields:name")
    private String name;

    private String description;

    @JsonIgnore
    private Integer id;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
  
    public String toString() {
        return "Method [id=" + this.id + ", name=" + this.getName() + ", description=" + this.getDescription() + "]";
    }
}
