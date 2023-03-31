package com.culturestamp.back.auth.api.service;

import com.culturestamp.back.auth.api.dto.GoogleOAuthTokenDto;
import com.culturestamp.back.auth.api.dto.OAuthAttributes;
import com.culturestamp.back.auth.config.properties.GoogleOAuthProperties;
import com.culturestamp.back.auth.config.util.JWTUtils;
import com.culturestamp.back.dto.UserServiceResponse;
import com.culturestamp.back.entity.User;
import com.culturestamp.back.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;
import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;

import static com.culturestamp.back.entity.Role.USER;


@Service
public class UserOAuthService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final JWTUtils jwtUtils;
    private final GoogleIdTokenVerifier verifier;
    private final HttpSession httpSession;

    @Autowired
    private final GoogleOAuthProperties googleOAuthProperties;

    public UserOAuthService(@Value("${app.googleClientId}") String clientId, UserRepository userRepository,
        JWTUtils jwtUtils, HttpSession httpSession, GoogleOAuthProperties googleOAuthProperties) {
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        this.httpSession = httpSession;
        this.googleOAuthProperties = googleOAuthProperties;
        NetHttpTransport transport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();
        verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
            .setAudience(Collections.singletonList(clientId))
            .build();
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
            .getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        User user = saveOrUpdate(attributes);
        httpSession.setAttribute("user", user);

        return new DefaultOAuth2User(
            Collections.singleton(new SimpleGrantedAuthority(user.getRole())),
            attributes.getAttributes(),
            attributes.getNameAttributeKey());
    }


    public User getUser(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public String loginOAuthGoogle(HttpServletRequest request) throws Exception {
        GoogleOAuthTokenDto tokenDto = exchangeCodeToToken(request);

        User user = verifyIDToken(tokenDto.getIdToken());
        if (user == null) {
            throw new IllegalArgumentException();
        }
        user = createOrUpdateUser(user);
        return jwtUtils.createToken(new UserServiceResponse(user), false);
    }


    public GoogleOAuthTokenDto exchangeCodeToToken(HttpServletRequest request) throws Exception {
        String code = request.getParameter("code");

        System.out.println("code :" + code);
        HttpHeaders headers = new HttpHeaders();
        RestTemplate restTemplate = new RestTemplate();

        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> parameters = setParameter(code);

        HttpEntity<MultiValueMap<String,String>> restRequest = new HttpEntity<>(parameters, headers);

        URI uri = URI.create("https://oauth2.googleapis.com/token");
        ResponseEntity<GoogleOAuthTokenDto> restResponse = restTemplate.postForEntity(uri, restRequest, GoogleOAuthTokenDto.class);
        GoogleOAuthTokenDto tokenDto = restResponse.getBody();

        return tokenDto;
    }


    public MultiValueMap<String, String> setParameter(String code) {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

        parameters.add("code", code);
        parameters.add("client_id", googleOAuthProperties.getClientId());
        parameters.add("client_secret", googleOAuthProperties.getClientSecret());
        parameters.add("redirect_uri", googleOAuthProperties.getRedirectUri());
        parameters.add("access_type", googleOAuthProperties.getAccessType());
        parameters.add("grant_type", "authorization_code");

        return parameters;
    }

    @Transactional
    public User createOrUpdateUser(User user) {
        UserServiceResponse existingUserService = userRepository.findByEmail(user.getEmail());
        if (existingUserService == null) {
            UserServiceResponse userServiceResponse = new UserServiceResponse(user);
            userServiceResponse.setRole(USER.getKey());
            existingUserService = userServiceResponse;
        }
        existingUserService.setEmail(user.getEmail());
        System.out.println(user.getEmail());
        return userRepository.save(new User(existingUserService));
    }

    private User verifyIDToken(String idToken) {
        try {
            GoogleIdToken idTokenObj = verifier.verify(idToken);
            if (idTokenObj == null) {
                return null;
            }
            GoogleIdToken.Payload payload = idTokenObj.getPayload();
            String email = payload.getEmail();
            String nickname = (String) payload.get("name");
            return new User(nickname, email);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public User saveOrUpdateTest(Map<String, String> params) {
        String email = params.get("email");
        String nickname = params.get("name");
        UserServiceResponse userServiceResponse = userRepository.findByEmail(email);
        User user = null;
        if (userServiceResponse == null) {
            user = new User(nickname, email);
        } else {
            user = new User(userServiceResponse);
        }
        user.update(nickname);
        return userRepository.save(user);
    }


    private User saveOrUpdate(OAuthAttributes attributes) {
        UserServiceResponse userServiceResponse = userRepository.findByEmail(attributes.getEmail());
        User user = null;
        if (userServiceResponse == null) {
            user = attributes.toEntity();
        } else {
            user = new User(userServiceResponse);
        }
        user.update(attributes.getName());
        return userRepository.save(user);
    }
}