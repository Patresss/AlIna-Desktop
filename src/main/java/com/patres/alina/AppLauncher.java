package com.patres.alina;

import com.patres.alina.server.ServerApplication;
import com.patres.alina.server.message.ChatMessageController;
import com.patres.alina.uidesktop.Launcher;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Optional;

@SpringBootApplication
public class AppLauncher {

    public static ConfigurableApplicationContext SPRING_CONTEXT;

    public static void main(String[] args) throws InterruptedException {
        SPRING_CONTEXT = new SpringApplicationBuilder(AppLauncher.class)
                .web(WebApplicationType.NONE)
                .run();
        Launcher.launchFxApp(args);

    }

    public static <T> T getBean(Class<T> type) {
        return SPRING_CONTEXT.getBean(type);
    }
}
