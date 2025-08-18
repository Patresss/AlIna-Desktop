package com.patres.alina.server.integration.spotify;


import com.patres.alina.server.integration.IntegrationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/integrations/spotify")
public class SpotifyController {

    private final IntegrationService integrationService;

    public SpotifyController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @GetMapping("/callback")
    public String callback(@RequestParam String code, @RequestParam String state) {
        integrationService.updateIntegrationAuth(state, code);
        return "Authentication complete<br>" +
                "You may now close this window<br> Code: <br>" + code;
    }

}