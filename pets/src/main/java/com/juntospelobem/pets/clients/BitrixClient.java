package com.juntospelobem.pets.clients;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.juntospelobem.pets.dtos.CardResponse;
import com.juntospelobem.pets.dtos.ClienteDados;
import com.juntospelobem.pets.exceptions.ClienteNaoEncontradoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Component
public class BitrixClient {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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
    }

    public String buscarEmailPorDocumento(String documento) {
        return buscarDadosClientePorDocumento(documento).email(); 
    }

    public List<CardResponse> buscarCardsPorDocumento(String documento) {
        Set<String> variacoes = gerarVariacoesDocumento(documento);
        if (variacoes.isEmpty()) return List.of();

        List<Map<String, Object>> items = List.of();

        for (String termoBusca : variacoes) {
            // 💡 Atualizado para chamar o novo método paginado
            items = executarConsultaCardsBitrixPaginado(termoBusca);
            if (!items.isEmpty()) {
                System.out.println("✅ " + items.size() + " Cards encontrados no Bitrix usando o termo: " + termoBusca);
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
            
            resultadosBusca = executarConsultaClientesBitrix(termoBusca, false);
            
            if (resultadosBusca.isEmpty() && termoBusca.replaceAll("\\D", "").length() >= 8) {
                System.out.println("🔄 Tentando busca aproximada (LIKE) para: " + termoBusca);
                resultadosBusca = executarConsultaClientesBitrix(termoBusca, true);
            }

            if (!resultadosBusca.isEmpty()) {
                System.out.println("✅ Cliente localizado no Bitrix usando o termo: " + termoBusca);
                break; 
            }
        }

        if (resultadosBusca.isEmpty()) {
            throw new ClienteNaoEncontradoException("Nenhum cliente encontrado com o documento informado.");
        }

        final Map<String, Object> contatoFallback = resultadosBusca.getFirst();

        return resultadosBusca.stream()
                .filter(item -> {
                    String email = obterEmailSeguro(item);
                    return email != null && !email.isBlank() && email.contains("@");
                })
                .findFirst() 
                .map(item -> new ClienteDados(obterIdSeguro(item), obterEmailSeguro(item)))
                .orElseGet(() -> new ClienteDados(obterIdSeguro(contatoFallback), obterEmailSeguro(contatoFallback)));
    }

    private List<Map<String, Object>> executarConsultaClientesBitrix(String valorBusca, boolean usarFiltroAproximado) {
        String filtroChave = usarFiltroAproximado ? "filter[%ufCrm78_1782267126]=" : "filter[ufCrm78_1782267126]=";

        String url = this.bitrixWebhookUrl + "crm.item.list.json?" +
                "entityTypeId=" + this.spaClientesId +
                "&filter[categoryId]=" + this.categoriaClientesId +
                "&" + filtroChave + codificarValorParametro(valorBusca) +
                "&select[]=id" +
                "&select[]=ufCrm78_1782267707" +
                "&select[]=ufCrm78_1782267174" +
                "&select[]=email" +
                "&order[id]=desc";

        return realizarRequisicaoGet(url, "Clientes");
    }

    // 💡 SOLUÇÃO SÊNIOR: Método dedicado para paginação de Cards via ponteiro 'next' e 'start'
    private List<Map<String, Object>> executarConsultaCardsBitrixPaginado(String valorCgc) {
        List<Map<String, Object>> todosOsCards = new ArrayList<>();
        int start = 0;
        boolean temMaisPaginas = true;

        while (temMaisPaginas) {
            String url = this.bitrixWebhookUrl + "crm.item.list.json?" +
                    "entityTypeId=" + this.spaCardsId +
                    "&filter[categoryId]=" + this.categoriaCardsId +
                    "&filter[ufCrm96Cgc]=" + codificarValorParametro(valorCgc) +
                    "&start=" + start +
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
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .header("Accept", "application/json")
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200 && response.body() != null) {
                    Map<?, ?> mapResponse = OBJECT_MAPPER.readValue(response.body(), Map.class);

                    if (mapResponse.get("result") instanceof Map<?, ?> result) {
                        if (result.get("items") instanceof List<?> itemsRaw) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> items = (List<Map<String, Object>>) itemsRaw;
                            todosOsCards.addAll(items);
                        }
                    }

                    // Verifica se existe o atributo 'next' indicando mais páginas
                    if (mapResponse.containsKey("next") && mapResponse.get("next") != null) {
                        start = Integer.parseInt(mapResponse.get("next").toString());
                    } else {
                        temMaisPaginas = false;
                    }
                } else {
                    System.err.println("⚠️ Resposta HTTP " + response.statusCode() + " do Bitrix (Cards Paginados)");
                    temMaisPaginas = false;
                }

            } catch (Exception e) {
                System.err.println("Erro ao buscar página de cards (" + start + ") no Bitrix: " + e.getMessage());
                temMaisPaginas = false;
            }
        }

        return todosOsCards;
    }

    private List<Map<String, Object>> realizarRequisicaoGet(String url, String contexto) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body() != null) {
                Map<?, ?> mapResponse = OBJECT_MAPPER.readValue(response.body(), Map.class);

                if (mapResponse.get("result") instanceof Map<?, ?> result) {
                    if (result.get("items") instanceof List<?> itemsRaw) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> items = (List<Map<String, Object>>) itemsRaw;
                        return items;
                    }
                }
            } else {
                System.err.println("⚠️ Resposta HTTP " + response.statusCode() + " do Bitrix (" + contexto + ")");
            }
            return List.of();

        } catch (Exception e) {
            System.err.println("Erro na chamada nativa ao Bitrix (" + contexto + "): " + e.getMessage());
            return List.of();
        }
    }

    private String codificarValorParametro(String valor) {
        if (valor == null) return "";
        return URLEncoder.encode(valor, StandardCharsets.UTF_8)
                .replace("%2F", "/")
                .replace("+", "%20");
    }

    private Set<String> gerarVariacoesDocumento(String documento) {
        if (documento == null || documento.isBlank()) return Collections.emptySet();

        Set<String> variacoes = new LinkedHashSet<>();
        String docLimpo = documento.trim();
        String apenasNumeros = docLimpo.replaceAll("\\D", "");

        variacoes.add(docLimpo);

        if (apenasNumeros.isBlank()) return variacoes;

        if (apenasNumeros.length() <= 11) {
            String cpfPad = padLeftZeros(apenasNumeros, 11);
            variacoes.add(formatarCpf(cpfPad));
            variacoes.add(cpfPad);
        } else {
            String cnpjPad = padLeftZeros(apenasNumeros, 14);
            variacoes.add(formatarCnpj(cnpjPad));
            variacoes.add(cnpjPad);
        }

        String semZerosIniciais = apenasNumeros.replaceFirst("^0+", "");
        if (!semZerosIniciais.isEmpty() && !semZerosIniciais.equals(apenasNumeros)) {
            variacoes.add(semZerosIniciais);
        }

        return variacoes;
    }

    private String padLeftZeros(String input, int targetLength) {
        if (input.length() >= targetLength) return input;
        return "0".repeat(targetLength - input.length()) + input;
    }

    private String formatarCpf(String cpf11) {
        if (cpf11.length() != 11) return cpf11;
        return cpf11.replaceFirst("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
    }

    private String formatarCnpj(String cnpj14) {
        if (cnpj14.length() != 14) return cnpj14;
        return cnpj14.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
    }

    private CardResponse converterParaCardResponse(Map<String, Object> deal) {
        String id = obterValorSeguro(deal, "id", "ID");
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

    private String obterEmailSeguro(Map<String, Object> item) {
        if (item.get("ufCrm78_1782267174") != null) return item.get("ufCrm78_1782267174").toString();
        if (item.get("email") != null) return item.get("email").toString();
        if (item.get("EMAIL") != null) return item.get("EMAIL").toString();
        return null;
    }

    private String obterIdSeguro(Map<String, Object> item) {
        if (item.get("ufCrm78_1782267707") != null) return item.get("ufCrm78_1782267707").toString();
        if (item.get("id") != null) return item.get("id").toString();
        if (item.get("ID") != null) return item.get("ID").toString();
        return "";
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