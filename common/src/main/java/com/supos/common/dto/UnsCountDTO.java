package com.supos.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnsCountDTO {
    private int countChildren;
    private int countDirectChildren;
    private boolean hasChildren;

    public UnsCountDTO(int countChildren, int countDirectChildren) {
        this.countChildren = countChildren;
        this.countDirectChildren = countDirectChildren;
    }
}
