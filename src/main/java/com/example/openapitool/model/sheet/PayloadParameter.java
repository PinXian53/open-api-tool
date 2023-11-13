package com.example.openapitool.model.sheet;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class PayloadParameter {
    String sequence;
    String name;
    String type;
    Integer maxLength;
    String description;
}
