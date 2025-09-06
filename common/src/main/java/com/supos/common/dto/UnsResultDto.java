package com.supos.common.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UnsResultDto {


    List<CreateTopicDto> allDefinitions;

    List<CreateTopicDto> matchResults;

}
