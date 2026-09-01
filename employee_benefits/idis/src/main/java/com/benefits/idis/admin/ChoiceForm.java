package com.benefits.idis.admin;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 선택지 입력. 엔티티가 아니라 화면 DTO 라 Setter 를 둔다. */
@Getter
@Setter
@NoArgsConstructor
public class ChoiceForm {

    private String content;

    /** 이미지 선택에서만 쓴다. 업로드가 끝난 뒤 받은 경로가 들어온다. */
    private String imagePath;
}
