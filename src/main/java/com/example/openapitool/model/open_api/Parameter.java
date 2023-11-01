package com.example.openapitool.model.open_api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Parameter {
    Integer level;
    String name;
    String type;
    String description;
    Boolean required;
    List<Object> enumValues;
    Integer minLength;
    Integer maxLength;
    Object example;
}
