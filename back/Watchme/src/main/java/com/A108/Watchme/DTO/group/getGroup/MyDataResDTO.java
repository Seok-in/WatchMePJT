package com.A108.Watchme.DTO.group.getGroup;

import lombok.*;

import java.util.Date;
import java.util.List;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@Builder
public class MyDataResDTO {
    private Integer role;
    private Integer assign;
    private Integer studyTime;
    private List<Integer> penalty;
    private String joinDate;
}
