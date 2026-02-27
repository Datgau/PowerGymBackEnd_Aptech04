package com.example.project_backend04.service;

import com.example.project_backend04.dto.response.Auth.FacebookUserData;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FacebookApiService {

    public FacebookUserData getUserInfo(String accessToken) {
        String url = "https://graph.facebook.com/me?fields=id,name,email,picture.type(large)&access_token=" + accessToken;

        RestTemplate restTemplate = new RestTemplate();

        try {
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);

            FacebookUserData data = new FacebookUserData();
            data.setId(response.get("id").asText());
            data.setName(response.get("name").asText());
            data.setEmail(response.has("email") ? response.get("email").asText() : null);
            data.setPictureUrl(response.get("picture").get("data").get("url").asText());

            return data;

        } catch (Exception e) {
            return null;
        }
    }
}
