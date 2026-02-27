package com.example.project_backend04.service;

import com.example.project_backend04.dto.response.Auth.GoogleUserData;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class GoogleApiService {

    @Value("${google.client-id}")
    private String clientId;

    public GoogleUserData getUserInfo(String idTokenString) {
        try {

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(clientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                System.out.println("Invalid ID token");
                System.out.println("Expected clientId: " + clientId);
                return null;
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            GoogleUserData data = new GoogleUserData();
            data.setId(payload.getSubject());
            data.setFullName((String) payload.get("name"));
            data.setEmail(payload.getEmail());
            data.setAvatar((String) payload.get("picture"));

            return data;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}