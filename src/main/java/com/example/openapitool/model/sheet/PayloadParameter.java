package com.example.openapitool.model.sheet;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class PayloadParameter {
    Integer level;
    String levelValue;
    String name;
    String type;
    Integer maxLength;
    String description;
}
