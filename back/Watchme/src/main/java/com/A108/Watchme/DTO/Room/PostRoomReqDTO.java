package com.A108.Watchme.DTO.Room;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.*;

@Getter @Setter
public class PostRoomReqDTO {
    @NotBlank(message = "방 이름을 입력하세요.")
    private String roomName;
    @NotBlank(message = "규칙을 설정하세요.")
    private String mode;
    private Integer roomPwd;
    private String description;
    @NotBlank(message = "카테고리를 입력하세요")
    private String categoryName;
    @NotNull(message = "인원을 입력하세요.")
    @Positive(message = "인원은 0보다 큰 값이 들어가야 합니다.")
    @Max(value = 25)
    private Integer num;
    private String endTime;
}
