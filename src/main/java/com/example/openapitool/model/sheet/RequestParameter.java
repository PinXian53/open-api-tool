package com.example.openapitool.model.sheet;

import lombok.*;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
public class RequestParameter extends PayloadParameter {
    String required;
}
