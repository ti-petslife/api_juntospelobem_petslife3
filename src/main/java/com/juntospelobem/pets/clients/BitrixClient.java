package com.juntospelobem.pets.clients;

import com.juntospelobem.pets.dtos.CardResponse;
import com.juntospelobem.pets.dtos.ClienteDados;
import com.juntospelobem.pets.exceptions.ClienteNaoEncontradoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class BitrixClient {

    private final RestClient restClient;
    private final String bitrixWebhookUrl;
    
    private final int spaClientesId;
    private final int spaCardsId;
    
    private final int categoriaClientesId;
    private final int categoriaCardsId;

   public BitrixClient(
            @Value("${bitrix.webhook.url}") String bitrixWebhookUrl,
            @Value("${bitrix.spa.clientes-id}") int spaClientesId,
            @Value("${bitrix.spa.cards-id}") int spaCardsId,
            @Value("${bitrix.spa.category-id.clientes}") int categoriaClientesId,
            @Value("${bitrix.spa.category-id.cards}") int categoriaCardsId) {
        
        this.bitrixWebhookUrl = bitrixWebhookUrl.endsWith("/") ? bitrixWebhookUrl : bitrixWebhookUrl + "/";
        this.spaClientesId = spaClientesId;
        this.spaCardsId = spaCardsId;
        this.categoriaClientesId = categoriaClientesId;
        this.categoriaCardsId = categoriaCardsId;
        
        this.restClient = RestClient.create(); 
    }

    public String buscarEmailPorDocumento(String documento) {
        String url = this.bitrixWebhookUrl + "crm.item.list.json?" +
                "entityTypeId=" + this.spaClientesId +
                "&filter[categoryId]=" + this.categoriaClientesId +
                "&filter[ufCrm78_1782267126]=" + aplicarMascaraBitrix(documento) + 
                "&select[]=ufCrm78_1782267174"; 

        try {
            Map<String, Object> response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("result")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.get("result");
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
                
                if (items != null && !items.isEmpty()) {
                    Map<String, Object> contato = items.get(0);
                    return (String) contato.get("ufCrm78_1782267174"); 
                }
            }
            
            throw new ClienteNaoEncontradoException("Nenhum cliente encontrado com o documento informado.");

        } catch (ClienteNaoEncontradoException e) {
            throw e; 
        } catch (Exception e) {
            throw new RuntimeException("Erro de comunicação com o servidor do Bitrix.", e);
        }
    }
public List<CardResponse> buscarCardsPorDocumento(String documento) {
        
        String docLimpoPesquisa = documento != null ? documento.replaceAll("\\D", "") : "";
        
        if (docLimpoPesquisa.isEmpty()) return List.of();

        String baseUrl = this.bitrixWebhookUrl + "crm.item.list.json";

        String url = UriComponentsBuilder.fromUriString(baseUrl) 
                .queryParam("entityTypeId", this.spaCardsId)
                .queryParam("filter[categoryId]", this.categoriaCardsId)
                .queryParam("select[]", "id")
                .queryParam("select[]", "ufCrm78_1782267707")
                .queryParam("select[]", "stageId")
                .queryParam("select[]", "createdTime")
                .queryParam("select[]", "opportunity")
                .queryParam("select[]", "ufCrm96Numnota")
                .queryParam("select[]", "ufCrm96Linkcupom")
                .queryParam("select[]", "ufCrm96Qtcupons")
                .queryParam("select[]", "UF_CRM_96_CGC") 
                .queryParam("select[]", "ufCrm96Cgc")
                .toUriString();

        try {
            Map<String, Object> response = restClient.get().uri(url).retrieve().body(Map.class);

            if (response != null && response.containsKey("result")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.get("result");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");

                if (items != null && !items.isEmpty()) {
                    
                    return items.stream()
                            .filter(deal -> {
                                Object cgcBitrix = deal.get("ufCrm96Cgc");
                                if (cgcBitrix == null) cgcBitrix = deal.get("UF_CRM_96_CGC");
                                
                                if (cgcBitrix == null) return false; 

                                String docLimpoBitrix = cgcBitrix.toString().replaceAll("\\D", "");
                                
                                return docLimpoPesquisa.equals(docLimpoBitrix);
                            })
                            .map(this::converterParaCardResponse)
                            .toList(); 
                }
            }
            return List.of(); 

        } catch (Exception e) {
            System.err.println("Erro ao comunicar com o Bitrix: " + e.getMessage());
            return List.of();
        }
    }

   public ClienteDados buscarDadosClientePorDocumento(String documento) {
    String documentoFormatado = aplicarMascaraBitrix(documento);
    
    String url = UriComponentsBuilder.fromUriString(this.bitrixWebhookUrl + "crm.item.list.json")
            .queryParam("entityTypeId", this.spaClientesId)
            .queryParam("filter[categoryId]", this.categoriaClientesId)
            .queryParam("filter[ufCrm78_1782267126]", documentoFormatado)
            .queryParam("select[]", "ufCrm78_1782267707")
            .queryParam("select[]", "ufCrm78_1782267174")
            .build()
            .encode() 
            .toUriString();

    try {
        Map<String, Object> response = restClient.get().uri(url).retrieve().body(Map.class);
        if (response != null && response.containsKey("result")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) response.get("result");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
            
            if (items != null && !items.isEmpty()) {
                Map<String, Object> contato = items.get(0);
                System.out.println("Chaves do Bitrix: " + contato.keySet());
                String id = contato.get("ufCrm78_1782267707") != null ? contato.get("ufCrm78_1782267707").toString() : "";
                String email = (String) contato.get("ufCrm78_1782267174");
                
                return new ClienteDados(id, email);
            }
        }
        throw new ClienteNaoEncontradoException("Nenhum cliente encontrado com o documento informado.");
    } catch (ClienteNaoEncontradoException e) {
        throw e;
    } catch (Exception e) {
        throw new RuntimeException("Erro de comunicação com o servidor do Bitrix.", e);
    }
}

    private String aplicarMascaraBitrix(String documento) {
        if (documento == null) return "";

        String apenasNumeros = documento.replaceAll("\\D", "");

        if (apenasNumeros.length() == 11) {
            return apenasNumeros.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        } else if (apenasNumeros.length() == 14) {
            return apenasNumeros.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
        }

        return documento;
    }

   private CardResponse converterParaCardResponse(Map<String, Object> deal) {
    String id = deal.get("id") != null ? deal.get("id").toString() : null;
    String status = (String) deal.get("stageId");
    String dataCriacao = (String) deal.get("createdTime");
    
    String link = obterValorSeguro(deal, "ufCrm96Linkcupom", "UF_CRM_96_LINKCUPOM"); 
    
    String numNotaStr = obterValorSeguro(deal, "ufCrm96Numnota", "UF_CRM_96_NUMNOTA");
    Integer numeroNotaFiscal = numNotaStr != null ? Integer.valueOf(numNotaStr) : 0; 
    
    String qtCuponsStr = obterValorSeguro(deal, "ufCrm96Qtcupons", "UF_CRM_96_QTCUPONS");
    Integer quantidadeCupons = qtCuponsStr != null ? Integer.valueOf(qtCuponsStr) : 0;
    
    BigDecimal valorTotal = deal.get("opportunity") != null ? new BigDecimal(deal.get("opportunity").toString()) : BigDecimal.ZERO;

    return new CardResponse(id, status, dataCriacao, numeroNotaFiscal, valorTotal, link, quantidadeCupons);
}

    private String obterValorSeguro(Map<String, Object> deal, String chaveMinusc, String chaveMaiusc) {
        if (deal.containsKey(chaveMaiusc) && deal.get(chaveMaiusc) != null) {
            return deal.get(chaveMaiusc).toString();
        }
        if (deal.containsKey(chaveMinusc) && deal.get(chaveMinusc) != null) {
            return deal.get(chaveMinusc).toString();
        }
        return null;
    }
}