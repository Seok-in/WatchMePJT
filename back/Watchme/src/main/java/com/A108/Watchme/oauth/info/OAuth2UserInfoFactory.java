package com.A108.Watchme.oauth.info;


import com.A108.Watchme.VO.ENUM.ProviderType;
import com.A108.Watchme.oauth.info.impl.GoogleOAuth2UserInfo;
import com.A108.Watchme.oauth.info.impl.KakaoOAuth2UserInfo;
import com.A108.Watchme.oauth.info.impl.NaverOAuth2UserInfo;

import java.util.Map;

public class OAuth2UserInfoFactory {
    public static OAuth2UserInfo getOAuth2UserInfo(ProviderType providerType, Map<String, Object> attributes) {
        switch (providerType) {
            case GOOGLE: return new GoogleOAuth2UserInfo(attributes);
            case NAVER: return new NaverOAuth2UserInfo(attributes);
            case KAKAO: return new KakaoOAuth2UserInfo(attributes);
            default: throw new IllegalArgumentException("Invalid Provider Type.");
        }
    }
}
