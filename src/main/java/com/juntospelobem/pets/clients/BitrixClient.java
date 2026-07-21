package com.juntospelobem.pets.clients;

import com.juntospelobem.pets.dtos.CardResponse;
import com.juntospelobem.pets.dtos.ClienteDados;
import com.juntospelobem.pets.exceptions.ClienteNaoEncontradoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.*;

@Component
public class BitrixClient {

    private final RestClient restClient;
    private final String bitrixWebhookUrl;
    
    private final int spaClientesId;
    private final int spaCardsId;
    
    private final int categoriaClientesId;
    private final int categoriaCardsId;

    private String obterEmailSeguro(Map<String, Object> item) {
        if (item.get("ufCrm78_1782267174") != null) return item.get("ufCrm78_1782267174").toString();
        if (item.get("email") != null) return item.get("email").toString();
        if (item.get("EMAIL") != null) return item.get("EMAIL").toString();
        return null;
    }

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
        Set<String> variacoes = gerarVariacoesDocumento(documento);
        if (variacoes.isEmpty()) return List.of();

        List<Map<String, Object>> items = List.of();

        for (String termoBusca : variacoes) {
            items = executarConsultaCardsBitrix(termoBusca);
            if (!items.isEmpty()) {
                System.out.println("✅ Cards encontrados no Bitrix usando o termo: " + termoBusca);
                break; 
            }
        }

        if (items.isEmpty()) {
            System.out.println("⚠️ Nenhum card encontrado no Bitrix para o documento informado.");
            return List.of();
        }

        return items.stream()
                .map(this::converterParaCardResponse)
                .toList();
    }

    public ClienteDados buscarDadosClientePorDocumento(String documento) {
        Set<String> variacoes = gerarVariacoesDocumento(documento);
        if (variacoes.isEmpty()) {
            throw new ClienteNaoEncontradoException("Documento inválido ou não informado.");
        }

        List<Map<String, Object>> resultadosBusca = List.of();

        for (String termoBusca : variacoes) {
            System.out.println("🔍 Consultando cliente no Bitrix com termo: " + termoBusca);
            resultadosBusca = executarConsultaClientesBitrix(termoBusca);
            if (resultadosBusca != null && !resultadosBusca.isEmpty()) {
                System.out.println("✅ Cliente localizado no Bitrix usando o termo: " + termoBusca);
                break; 
            }
        }

        if (resultadosBusca == null || resultadosBusca.isEmpty()) {
            throw new ClienteNaoEncontradoException("Nenhum cliente encontrado com o documento informado.");
        }

        System.out.println("🚨 ATENÇÃO: O Bitrix retornou " + resultadosBusca.size() + " cadastros!");

        final Map<String, Object> contatoFallback = resultadosBusca.get(0);

        return resultadosBusca.stream()
                .filter(item -> {
                    // Busca o e-mail em qualquer uma das chaves possíveis que o Bitrix possa entregar
                    String email = obterEmailSeguro(item);
                    return email != null && !email.trim().isEmpty() && email.contains("@");
                })
                .findFirst() 
                .map(item -> {
                    String id = item.get("id") != null ? item.get("id").toString() : "";
                    String email = obterEmailSeguro(item);
                    System.out.println("✅ Cliente e E-mail validados com sucesso: " + email);
                    return new ClienteDados(id, email);
                })
                .orElseGet(() -> {
                    System.out.println("⚠️ Usando fallback para os dados do cliente.");
                    String id = contatoFallback.get("id") != null ? contatoFallback.get("id").toString() : "";
                    String email = obterEmailSeguro(contatoFallback);
                    return new ClienteDados(id, email);
                });
    }

    private List<Map<String, Object>> executarConsultaCardsBitrix(String valorCgc) {
        String docCodificado = URLEncoder.encode(valorCgc, StandardCharsets.UTF_8);

        String url = this.bitrixWebhookUrl + "crm.item.list.json?" +
                "entityTypeId=" + this.spaCardsId +
                "&filter[categoryId]=" + this.categoriaCardsId +
                "&filter[ufCrm96Cgc]=" + docCodificado +
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
                if (items != null) return items;
            }
            return List.of();
        } catch (Exception e) {
            System.err.println("Erro ao comunicar com o Bitrix (Cards): " + e.getMessage());
            return List.of();
        }
    }

    // 💡 Ajuste cirúrgico do filtro de Clientes no Bitrix
    // 💡 Ajuste cirúrgico do filtro de Clientes no Bitrix
    private List<Map<String, Object>> executarConsultaClientesBitrix(String valorBusca) {
        String docCodificado = URLEncoder.encode(valorBusca, StandardCharsets.UTF_8);

        // Alinhamento exato com a SPA 1174 e Categoria 152 do seu Bitrix
        String url = this.bitrixWebhookUrl + "crm.item.list.json?" +
                "entityTypeId=" + this.spaClientesId +
                "&filter[categoryId]=" + this.categoriaClientesId +
                "&filter[ufCrm96Cgc]=" + docCodificado + // 🎯 CORREÇÃO: Chave exata do campo CGC no Bitrix
                "&select[]=id" +
                "&select[]=ufCrm78_1782267707" +
                "&select[]=ufCrm78_1782267174" +
                "&select[]=email" +                       // Mapeamento abrangente de e-mail
                "&order[id]=desc";

        try {
            Map<String, Object> response = restClient.get().uri(url).retrieve().body(Map.class);
            if (response != null && response.containsKey("result")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.get("result");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
                if (items != null) return items;
            }
            return List.of();
        } catch (Exception e) {
            throw new RuntimeException("Erro de comunicação com o servidor do Bitrix.", e);
        }
    }

    private Set<String> gerarVariacoesDocumento(String documento) {
        if (documento == null || documento.isBlank()) return Collections.emptySet();

        String apenasNumeros = documento.replaceAll("\\D", "");
        Set<String> variacoes = new LinkedHashSet<>();

        variacoes.add(aplicarMascaraBitrix(documento));

        if (apenasNumeros.length() > 0 && apenasNumeros.length() <= 11) {
            variacoes.add(String.format("%11s", apenasNumeros).replace(' ', '0'));
        } else if (apenasNumeros.length() > 11 && apenasNumeros.length() <= 14) {
            variacoes.add(String.format("%14s", apenasNumeros).replace(' ', '0'));
        } else {
            variacoes.add(apenasNumeros);
        }

        String semZerosIniciais = apenasNumeros.replaceFirst("^0+", "");
        if (!semZerosIniciais.isEmpty()) {
            variacoes.add(semZerosIniciais);
        }

        return variacoes;
    }

    private String aplicarMascaraBitrix(String documento) {
        if (documento == null || documento.isBlank()) return "";

        String apenasNumeros = documento.replaceAll("\\D", "");

        if (apenasNumeros.length() <= 11) {
            String cpfPad = String.format("%11s", apenasNumeros).replace(' ', '0');
            return cpfPad.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        } 
        
        if (apenasNumeros.length() <= 14) {
            String cnpjPad = String.format("%14s", apenasNumeros).replace(' ', '0');
            return cnpjPad.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
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