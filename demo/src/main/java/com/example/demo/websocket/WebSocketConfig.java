package com.example.demo.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {


    private final PhotoService photoService;

    public WebSocketConfig(PhotoService photoService) {
        this.photoService = photoService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new PhotoWebSocketHandler(photoService), "/upload-photo-ws")
                .setAllowedOrigins("*")
                ;
    }
    @Bean
    public WebSocketHandler photoWebSocketHandler() {
        return new WebSocketHandlerDecorator(new PhotoWebSocketHandler(photoService)) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                session.setTextMessageSizeLimit(30 * 1024 * 1024);
                session.setBinaryMessageSizeLimit(30 * 1024 * 1024);
                super.afterConnectionEstablished(session);
            }
        };
    }

}