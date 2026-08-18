package com.juntospelobem.pets.controllers;


import com.juntospelobem.pets.dtos.CardResponse;
import com.juntospelobem.pets.services.CardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {
    /*sera que vai assim */

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }
    /*sera que vai assim */
    @GetMapping
    public ResponseEntity<List<CardResponse>> listarMeusCards(
            @RequestAttribute("documentoCliente") String documento) {

        List<CardResponse> cards = cardService.buscarCardsDoCliente(documento);

        return ResponseEntity.ok(cards);
    }
}
