package com.example.openapitool.model.sheet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class OtherParameter {
    String in; // PATH, HEADER, QUERY
    String name;
    String type;
    Integer maxLength;
    String required;
    String description;
}
