package com.ranked4.game.game_service.controller;

import java.security.Principal;
import java.util.UUID;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import com.ranked4.game.game_service.dto.GifReactionEvent;
import com.ranked4.game.game_service.dto.GifReactionMessage;
import com.ranked4.game.game_service.service.GifService;
import com.ranked4.game.game_service.util.GameSessionRegistry;

@Controller
public class GameGifSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final GifService gifService;
    private final GameSessionRegistry sessionRegistry;

    public GameGifSocketController(SimpMessagingTemplate messagingTemplate,
                                    GifService gifService,
                                    GameSessionRegistry sessionRegistry) {
        this.messagingTemplate = messagingTemplate;
        this.gifService = gifService;
        this.sessionRegistry = sessionRegistry;
    }

    @MessageMapping("/game.gif/{gameId}")
    public void handleGifReaction(@DestinationVariable UUID gameId,
                                @Payload GifReactionMessage message,
                                Principal principal,
                                StompHeaderAccessor accessor) {

        if (!gameId.equals(message.getGameId())) {
            return;
        }

        if (!sessionRegistry.isPlayerConnectedToGame(message.getPlayerId(), gameId)) {
            return;
        }

        var gifOpt = gifService.getByCode(message.getGifCode());
        if (gifOpt.isEmpty()) {
            return;
        }

        var gif = gifOpt.get();

        GifReactionEvent event = new GifReactionEvent();
        event.setGameId(gameId);
        event.setPlayerId(message.getPlayerId());
        event.setGifCode(gif.getCode());
        event.setAssetPath(gif.getAssetPath());
        event.setTimestamp(System.currentTimeMillis());

        String topic = "/topic/game/" + gameId + "/gif";
        messagingTemplate.convertAndSend(topic, event);
    }
}