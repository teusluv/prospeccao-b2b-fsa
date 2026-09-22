package br.com.fsa.prospeccao.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class ProspeccaoDtos {

    private ProspeccaoDtos() {
    }

    public record BuscaGoogleMapsRequest(
            @NotBlank @Size(max = 100) String termo,
            @NotBlank @Size(max = 100) String cidade,
            @Pattern(regexp = "^[A-Za-z]{2}$", message = "deve ser a sigla do estado (ex.: BA)") String uf,
            @Min(1) @Max(60) Integer maxResultados,
            Boolean redeSocialContaComoSemSite,
            Boolean simular) {
    }

    public enum Situacao {
        IMPORTADA,
        JA_CADASTRADA,
        TEM_SITE,
        FECHADA
    }

    public record LugarEncontrado(String googlePlaceId, String nome, String categoria, String endereco,
                                  String telefone, String site, String googleMapsUrl, Situacao situacao,
                                  Long empresaId) {
    }

    public record BuscaGoogleMapsResponse(int encontradas, int semSite, int importadas, int jaCadastradas,
                                          boolean simulacao, List<LugarEncontrado> lugares) {
    }
}
