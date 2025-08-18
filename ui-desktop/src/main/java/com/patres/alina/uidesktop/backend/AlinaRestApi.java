package com.patres.alina.uidesktop.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.Feign;
import feign.auth.BasicAuthRequestInterceptor;
import feign.form.FormEncoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;

import static com.patres.alina.uidesktop.settings.SettingsMangers.SERVER_SETTINGS;

public class AlinaRestApi {

    private static volatile AlinaRestClient alinaRestClient;

    private AlinaRestApi() {
    }

    public static AlinaRestClient getAlinaRestClient() {
        if (alinaRestClient == null) {
            synchronized (AlinaRestApi.class) {
                if (alinaRestClient == null) {
                    final ObjectMapper mapper = createMapper();
                    final String address = SERVER_SETTINGS.getSettings().serverAddress();
                    final String password = SERVER_SETTINGS.getSettings().serverPassword(); // Poprawione, aby użyć serverPassword
                    alinaRestClient = Feign.builder()
                            .client(new OkHttpClient()) // To enable PATCH requests
                            .encoder(new FormEncoder(new JacksonEncoder()))
                            .decoder(new JacksonDecoder(mapper))
                            .requestInterceptor(new BasicAuthRequestInterceptor("user", password))
                            .target(AlinaRestClient.class, address);
                }
            }
        }
        return alinaRestClient;
    }

    private static ObjectMapper createMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule());
    }

}