package br.com.fsa.prospeccao.service;

import br.com.fsa.prospeccao.domain.Empresa;
import br.com.fsa.prospeccao.domain.EtapaFunil;
import br.com.fsa.prospeccao.exception.RecursoNaoEncontradoException;
import br.com.fsa.prospeccao.exception.RegraNegocioException;
import br.com.fsa.prospeccao.repository.EmpresaRepository;
import br.com.fsa.prospeccao.repository.UsuarioRepository;
import br.com.fsa.prospeccao.web.dto.EmpresaDtos.EmpresaRequest;
import br.com.fsa.prospeccao.web.dto.EmpresaDtos.EmpresaResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class EmpresaService {

    private final EmpresaRepository empresas;
    private final UsuarioRepository usuarios;
    private final Validator validator;

    public EmpresaService(EmpresaRepository empresas, UsuarioRepository usuarios, Validator validator) {
        this.empresas = empresas;
        this.usuarios = usuarios;
        this.validator = validator;
    }

    public record Filtro(String busca, EtapaFunil etapa, String segmento, String cidade, String uf,
                         Long responsavelId, Boolean semSite) {
    }

    @Transactional(readOnly = true)
    public Page<EmpresaResponse> listar(Filtro filtro, Pageable pageable) {
        return empresas.findAll(especificacao(filtro), pageable).map(EmpresaResponse::de);
    }

    @Transactional(readOnly = true)
    public EmpresaResponse buscar(Long id) {
        return EmpresaResponse.de(buscarEntidade(id));
    }

    /** Uso interno por outros serviços, dentro da transação deles. */
    @Transactional(readOnly = true)
    public Empresa buscarEntidade(Long id) {
        return empresas.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Empresa", id));
    }

    public EmpresaResponse criar(EmpresaRequest req, Long usuarioLogadoId) {
        Empresa e = new Empresa();
        aplicar(e, req);
        if (e.getResponsavel() == null) {
            e.setResponsavel(usuarios.getReferenceById(usuarioLogadoId));
        }
        return EmpresaResponse.de(empresas.save(e));
    }

    public EmpresaResponse atualizar(Long id, EmpresaRequest req) {
        Empresa e = buscarEntidade(id);
        aplicar(e, req);
        return EmpresaResponse.de(e);
    }

    public EmpresaResponse mudarEtapa(Long id, EtapaFunil etapa, String motivoPerda) {
        Empresa e = buscarEntidade(id);
        definirEtapa(e, etapa, motivoPerda);
        return EmpresaResponse.de(e);
    }

    public void excluir(Long id) {
        empresas.delete(buscarEntidade(id));
    }

    /**
     * Importa empresas de um CSV com cabeçalho. Colunas reconhecidas (qualquer ordem, separador ";" ou ","):
     * razaoSocial, nomeFantasia, cnpj, segmento, porte, cidade, uf, site, telefone, email, origem, valorEstimado.
     */
    public ImportacaoResultado importarCsv(List<String> linhas, Long usuarioLogadoId) {
        List<String> erros = new ArrayList<>();
        int importadas = 0;
        int ignoradas = 0;
        if (linhas.isEmpty()) {
            throw new RegraNegocioException("Arquivo vazio");
        }
        char separador = linhas.get(0).contains(";") ? ';' : ',';
        List<String> cabecalho = Csv.dividir(linhas.get(0).replace("﻿", ""), separador).stream()
                .map(c -> c.trim().toLowerCase(Locale.ROOT))
                .toList();
        if (!cabecalho.contains("razaosocial")) {
            throw new RegraNegocioException("O CSV precisa ter a coluna 'razaoSocial'");
        }
        for (int i = 1; i < linhas.size(); i++) {
            String linha = linhas.get(i);
            if (linha.isBlank()) {
                continue;
            }
            int numero = i + 1;
            try {
                List<String> valores = Csv.dividir(linha, separador);
                EmpresaRequest req = Csv.paraEmpresa(cabecalho, valores);
                var violacoes = validator.validate(req);
                if (!violacoes.isEmpty()) {
                    ConstraintViolation<EmpresaRequest> v = violacoes.iterator().next();
                    throw new RegraNegocioException(v.getPropertyPath() + " " + v.getMessage());
                }
                String cnpj = Cnpj.normalizar(req.cnpj());
                if (cnpj != null && empresas.existsByCnpj(cnpj)) {
                    ignoradas++;
                    erros.add("Linha " + numero + ": CNPJ " + cnpj + " já cadastrado (ignorada)");
                    continue;
                }
                criar(req, usuarioLogadoId);
                importadas++;
            } catch (RegraNegocioException | IllegalArgumentException ex) {
                ignoradas++;
                erros.add("Linha " + numero + ": " + ex.getMessage());
            }
        }
        return new ImportacaoResultado(importadas, ignoradas, erros);
    }

    public record ImportacaoResultado(int importadas, int ignoradas, List<String> erros) {
    }

    private void aplicar(Empresa e, EmpresaRequest req) {
        if (req.razaoSocial() == null || req.razaoSocial().isBlank()) {
            throw new RegraNegocioException("Razão social é obrigatória");
        }
        String cnpj = Cnpj.normalizar(req.cnpj());
        if (cnpj != null) {
            if (!Cnpj.valido(cnpj)) {
                throw new RegraNegocioException("CNPJ inválido: " + req.cnpj());
            }
            boolean duplicado = e.getId() == null ? empresas.existsByCnpj(cnpj)
                    : empresas.existsByCnpjAndIdNot(cnpj, e.getId());
            if (duplicado) {
                throw new RegraNegocioException("Já existe uma empresa com o CNPJ " + cnpj);
            }
        }
        e.setRazaoSocial(req.razaoSocial().trim());
        e.setNomeFantasia(limpar(req.nomeFantasia()));
        e.setCnpj(cnpj);
        e.setSegmento(limpar(req.segmento()));
        e.setPorte(req.porte());
        e.setCidade(limpar(req.cidade()));
        e.setUf(req.uf() == null || req.uf().isBlank() ? null : req.uf().trim().toUpperCase(Locale.ROOT));
        e.setSite(limpar(req.site()));
        e.setTelefone(limpar(req.telefone()));
        e.setEmail(limpar(req.email()));
        e.setOrigem(limpar(req.origem()));
        e.setValorEstimado(req.valorEstimado());
        e.setObservacoes(limpar(req.observacoes()));
        if (req.etapa() != null) {
            definirEtapa(e, req.etapa(), e.getMotivoPerda());
        }
        if (req.responsavelId() != null) {
            e.setResponsavel(usuarios.findById(req.responsavelId())
                    .orElseThrow(() -> new RegraNegocioException("Responsável não encontrado: " + req.responsavelId())));
        }
    }

    private static void definirEtapa(Empresa e, EtapaFunil etapa, String motivoPerda) {
        if (etapa == EtapaFunil.PERDIDO && (motivoPerda == null || motivoPerda.isBlank())) {
            throw new RegraNegocioException("Informe o motivo da perda ao mover para PERDIDO");
        }
        e.setEtapa(etapa);
        e.setMotivoPerda(etapa == EtapaFunil.PERDIDO ? motivoPerda.trim() : null);
    }

    private static String limpar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static Specification<Empresa> especificacao(Filtro f) {
        List<Specification<Empresa>> specs = new ArrayList<>();
        if (f.busca() != null && !f.busca().isBlank()) {
            String termo = "%" + f.busca().trim().toLowerCase(Locale.ROOT) + "%";
            String digitos = f.busca().replaceAll("\\D", "");
            specs.add((root, q, cb) -> {
                var condicoes = new ArrayList<jakarta.persistence.criteria.Predicate>();
                condicoes.add(cb.like(cb.lower(root.get("razaoSocial")), termo));
                condicoes.add(cb.like(cb.lower(root.get("nomeFantasia")), termo));
                if (!digitos.isEmpty()) {
                    condicoes.add(cb.like(root.get("cnpj"), "%" + digitos + "%"));
                }
                return cb.or(condicoes.toArray(jakarta.persistence.criteria.Predicate[]::new));
            });
        }
        if (f.etapa() != null) {
            specs.add((root, q, cb) -> cb.equal(root.get("etapa"), f.etapa()));
        }
        if (f.segmento() != null && !f.segmento().isBlank()) {
            specs.add((root, q, cb) -> cb.equal(cb.lower(root.get("segmento")), f.segmento().trim().toLowerCase(Locale.ROOT)));
        }
        if (f.cidade() != null && !f.cidade().isBlank()) {
            specs.add((root, q, cb) -> cb.equal(cb.lower(root.get("cidade")), f.cidade().trim().toLowerCase(Locale.ROOT)));
        }
        if (f.uf() != null && !f.uf().isBlank()) {
            specs.add((root, q, cb) -> cb.equal(root.get("uf"), f.uf().trim().toUpperCase(Locale.ROOT)));
        }
        if (Boolean.TRUE.equals(f.semSite())) {
            specs.add((root, q, cb) -> cb.or(cb.isNull(root.get("site")), cb.equal(cb.trim(root.get("site")), "")));
        } else if (Boolean.FALSE.equals(f.semSite())) {
            specs.add((root, q, cb) -> cb.and(cb.isNotNull(root.get("site")), cb.notEqual(cb.trim(root.get("site")), "")));
        }
        if (f.responsavelId() != null) {
            specs.add((root, q, cb) -> cb.equal(root.get("responsavel").get("id"), f.responsavelId()));
        }
        return Specification.allOf(specs);
    }
}
