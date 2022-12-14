package com.A108.Watchme.oauth.service;

import com.A108.Watchme.Exception.CustomException;
import com.A108.Watchme.Http.Code;
import com.A108.Watchme.Repository.MemberInfoRepository;
import com.A108.Watchme.Repository.MemberRepository;
import com.A108.Watchme.VO.ENUM.Gender;
import com.A108.Watchme.VO.ENUM.ProviderType;
import com.A108.Watchme.VO.ENUM.Role;
import com.A108.Watchme.VO.ENUM.Status;
import com.A108.Watchme.VO.Entity.member.Member;
import com.A108.Watchme.VO.Entity.member.MemberInfo;
import com.A108.Watchme.oauth.entity.RoleType;
import com.A108.Watchme.oauth.entity.UserPrincipal;
import com.A108.Watchme.oauth.exception.OAuthProviderMissMatchException;
import com.A108.Watchme.oauth.info.OAuth2UserInfo;
import com.A108.Watchme.oauth.info.OAuth2UserInfoFactory;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final MemberRepository memberRepository;
    private final MemberInfoRepository memberInfoRepository;
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(userRequest);

        try {
            return process(userRequest, user);
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OAuth2User process(OAuth2UserRequest userRequest, OAuth2User user) {
        ProviderType providerType = ProviderType.valueOf(userRequest.getClientRegistration().getRegistrationId().toUpperCase());

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(providerType, user.getAttributes());

        Optional<Member> savedUser = memberRepository.findByEmail(userInfo.getEmail());
        // ????????? ???????????? ????????? ????????? ?????????
        if (savedUser.isPresent()) {
            // EMAIL??? ????????? ????????? ????????????
            if ((savedUser.get().getProviderType()).toString().equals("EMAIL")) {
                // EMAIL?????? ??????????????? ???????????? ????????????.
                savedUser.get().setProviderType(providerType);
            }
            // ??? ?????? ?????? ????????? ????????? ?????? ??? ?????? ?????? ?????????????????? ????????????.
            if (!providerType.equals(savedUser.get().getProviderType())) {
                throw new CustomException(Code.C509);
            }
        }


        // ?????? ?????? ????????? ??????
        else {
            Member newMember = createUser(userInfo, providerType);
            return UserPrincipal.create(newMember, user.getAttributes(), userInfo.getImageUrl());
        }

        return UserPrincipal.create(savedUser.get(), user.getAttributes(), userInfo.getImageUrl());
    }

    private Member createUser(OAuth2UserInfo userInfo, ProviderType providerType) {
        Member member = Member.builder()
                .email(userInfo.getEmail())
                .nickName(userInfo.getNickName())
                .role(Role.MEMBER)
                .pwd("12345")
                .status(Status.YES)
                .providerType(providerType)
                .build();
        memberInfoRepository.save(MemberInfo.builder()
                .member(member)
                .imageLink(userInfo.getImageUrl())
                .build());
        return member;
    }
}
