package com.tbw.cut.dto;

import lombok.Data;
import java.util.List;

@Data
public class BilibiliCollectionDTO {
    private Long seasonId;
    private String name;
    private String cover;
    private String description;
    private Integer total;
}