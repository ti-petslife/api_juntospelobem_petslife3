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
        ClienteDados dados = buscarDadosClientePorDocumento(documento);
        return dados.email(); 
    }

    public List<CardResponse> buscarCardsPorDocumento(String documento) {
        String docLimpoPesquisa = documento != null ? documento.replaceAll("\\D", "") : "";
        if (docLimpoPesquisa.isEmpty()) return List.of();

        String documentoFormatado = aplicarMascaraBitrix(documento);
        
        String docCodificado = java.net.URLEncoder.encode(documentoFormatado, java.nio.charset.StandardCharsets.UTF_8);

        String url = this.bitrixWebhookUrl + "crm.item.list.json?" +
                "entityTypeId=" + this.spaCardsId +
                "&filter[categoryId]=" + this.categoriaCardsId +
                "&filter[ufCrm96_CGC]=" + docCodificado + 
                "&select[]=id" +
                "&select[]=ufCrm78_1782267707" +
                "&select[]=stageId" +
                "&select[]=createdTime" +
                "&select[]=opportunity" +
                "&select[]=ufCrm96Numnota" +
                "&select[]=ufCrm96Linkcupom" +
                "&select[]=ufCrm96Qtcupons" +
                "&order[id]=desc"; 

        try {
            Map<String, Object> response = restClient.get().uri(url).retrieve().body(Map.class);

            if (response != null && response.containsKey("result")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.get("result");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");

                if (items != null && !items.isEmpty()) {
                    return items.stream()
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
        
        String docCodificado = java.net.URLEncoder.encode(documentoFormatado, java.nio.charset.StandardCharsets.UTF_8);

        String url = this.bitrixWebhookUrl + "crm.item.list.json?" +
                "entityTypeId=" + this.spaClientesId +
                "&filter[categoryId]=" + this.categoriaClientesId +
                "&filter[ufCrm78_1782267126]=" + docCodificado +
                "&select[]=ufCrm78_1782267707" +
                "&select[]=ufCrm78_1782267174" +
                "&order[id]=desc";

        try {
            Map<String, Object> response = restClient.get().uri(url).retrieve().body(Map.class);
            if (response != null && response.containsKey("result")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.get("result");
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
                
                if (items != null && !items.isEmpty()) {
                    System.out.println("🚨 ATENÇÃO: O Bitrix retornou " + items.size() + " cadastros para o documento informado!");

                    return items.stream()
                            .filter(item -> {
                                String email = (String) item.get("ufCrm78_1782267174");
                                return email != null && !email.trim().isEmpty() && email.contains("@");
                            })
                            .findFirst() 
                            .map(item -> {
                                String id = item.get("ufCrm78_1782267707") != null ? item.get("ufCrm78_1782267707").toString() : "";
                                String email = (String) item.get("ufCrm78_1782267174");
                                System.out.println("✅ Email validado e selecionado: " + email);
                                return new ClienteDados(id, email);
                            })
                            .orElseGet(() -> {
                                
                                System.out.println("⚠️ Nenhum e-mail válido encontrado. Usando fallback de segurança.");
                                Map<String, Object> contatoFallback = items.get(0);
                                String id = contatoFallback.get("ufCrm78_1782267707") != null ? contatoFallback.get("ufCrm78_1782267707").toString() : "";
                                String email = (String) contatoFallback.get("ufCrm78_1782267174");
                                return new ClienteDados(id, email);
                            });
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