package com.juntospelobem.pets.services;

import com.juntospelobem.pets.dtos.CardResponse;
import com.juntospelobem.pets.clients.BitrixClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CardService {

    private final BitrixClient bitrixClient;

    public CardService(BitrixClient bitrixClient) {
        this.bitrixClient = bitrixClient;
    }

    
@Cacheable(value = "cardsCache", key = "#documento")
    public List<CardResponse> buscarCardsDoCliente(String documento) {
        
            return bitrixClient.buscarCardsPorDocumento(documento); 
           }
}