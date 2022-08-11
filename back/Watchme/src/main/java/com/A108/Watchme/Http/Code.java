package com.A108.Watchme.Http;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Code {
    C200 (200, "SUCCESS"),
    C300 (300, "INVALID"),
    C500 (500, "INTERNAL SERVER ERROR"),
    C501 (501, "INVALID JWT"),
    C502 (502, "LOGIN FAILED"),
    C503 (503, "NOT MEMBER"),

    // 520 에서부터 ROOM관련 ERROR
    C510 (510, "NO GROUP"),
    C511(511, "NO CATEGORY"),
    C512(512, "FILE UPLOAD FAILED"),
    C520 (520, "NO SEARCH DATA"),
    C521 (521, "INVALID VALUE"),
    C522 (522, "NO ROOM"),
    C523 (523,"NOT PARTICIPANTS"),
    C524 (524, "NAME ALREADY EXIST"),
    C525 (525, "ROOM PWD NOT INTEGER"),
    C526 (526,"INVALID ROOM PEOPLE NUM"),
    C527 (527, "INVALID END DATE"),
    C528 (528, "NOT IMAGE FILE"),
    C529 (529, "IMAGE TOO BIG"),
    C530 (530, "NOT ROOM OWNER"),
    C531 (531, "FAILED CREATE STUDY LOG"),
    C532 (532, "FAILED SAVE STUDY LOG"),
    C533 (533,"NO SPRINT"),
    C534 (534, "ALREADY APPLIED"),
    C535 (535, "NOT MY GROUP"),
    C536 (536, "NOT GROUP MANAGER"),
    C537(537, "NOT STATUS YES"),
    C538(538, "NOT ENOUGH POINT"),
    C539(539, "SPRINT NOT RUNNING"),
    C540(540, "NOT APPLIED"),
    C541(541, "SPRINT NOT RUNNING"),
    C543(543, "ALREADY SPRINT ALIVE"),
    C544(544, "START DATE ERROR"),
    C550(550, "TOO MANY PEOPLE"),
    C551(551, "ROOM PWD FAILED"),
    C564(504, "Invalid Group pwd"),
    C565(505, "Not Authorized"),
    C566(506, "Banned"),
    C567(507, "Applied Already"),
    C509(509, "ALREADY MEMBER"),
    C599(599, "DATE PARSING FAILED" ),
    C598(598, "INVALID POINT VALUE" ),
    C597(597, "KAKAO PAY ERROR" ),
    C596(596, "DTO ERROR");

    private int errCode;

    private final String message;


}
