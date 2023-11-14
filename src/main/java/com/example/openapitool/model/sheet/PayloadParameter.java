package com.example.openapitool.model.sheet;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
public class PayloadParameter {
    String sequence;
    String name;
    String type;
    Integer maxLength;
    String description;
    List<Object> enumValues;
}
