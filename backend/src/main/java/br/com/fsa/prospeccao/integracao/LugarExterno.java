package br.com.fsa.prospeccao.integracao;

/** Empresa encontrada numa fonte externa (Google Maps ou OpenStreetMap), já no formato comum. */
public record LugarExterno(
        String idExterno,
        String nome,
        String categoria,
        String endereco,
        String cidade,
        String uf,
        String telefone,
        String email,
        String site,
        String mapaUrl,
        boolean fechado) {
}
