package com.tbw.cut.dto;

import lombok.Data;
import java.util.List;

@Data
public class AnchorSubscribeDTO {
    /**
     * 主播UID列表
     */
    private List<String> uids;
}