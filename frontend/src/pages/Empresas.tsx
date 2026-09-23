import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../api/cliente'
import type { EtapaFunil, ImportacaoResponse, Pagina, Empresa } from '../api/tipos'
import EmpresaFormModal from '../components/EmpresaForm'
import { Carregando, Erro, EtapaBadge, Modal, Vazio } from '../components/ui'
import { useToast } from '../components/Toast'
import { ETAPAS, NOME_ETAPA, nomeEmpresa, reais, UFS } from '../formato'

export default function Empresas() {
  const [params, setParams] = useSearchParams()
  const navegar = useNavigate()
  const toast = useToast()
  const [pagina, setPagina] = useState<Pagina<Empresa> | null>(null)
  const [erro, setErro] = useState<unknown>(null)
  const [busca, setBusca] = useState(params.get('busca') ?? '')
  const [novo, setNovo] = useState(false)
  const [importacao, setImportacao] = useState<ImportacaoResponse | null>(null)
  const arquivo = useRef<HTMLInputElement>(null)
  const ultimaRequisicao = useRef(0)

  const filtro = {
    busca: params.get('busca') ?? '',
    etapa: (params.get('etapa') ?? '') as EtapaFunil | '',
    uf: params.get('uf') ?? '',
    cidade: params.get('cidade') ?? '',
    semSite: params.get('semSite') === 'true' ? true : params.get('semSite') === 'false' ? false : null,
    page: Number(params.get('page') ?? 0),
  }
  const chave = params.toString()

  function carregar() {
    setErro(null)
    // ignora respostas de filtros antigos que cheguem depois da atual
    const id = ++ultimaRequisicao.current
    api.empresas({ ...filtro, size: 20 })
      .then((p) => id === ultimaRequisicao.current && setPagina(p))
      .catch((e) => id === ultimaRequisicao.current && setErro(e))
  }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(carregar, [chave])

  // busca por texto com pequeno atraso enquanto digita
  useEffect(() => {
    const t = setTimeout(() => {
      if (busca !== filtro.busca) mudar('busca', busca)
    }, 350)
    return () => clearTimeout(t)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [busca])

  function mudar(chaveFiltro: string, valor: string) {
    const p = new URLSearchParams(params)
    if (valor) p.set(chaveFiltro, valor)
    else p.delete(chaveFiltro)
    if (chaveFiltro !== 'page') p.delete('page')
    setParams(p, { replace: true })
  }

  async function importar(f: File | undefined) {
    if (!f) return
    try {
      setImportacao(await api.importarCsv(f))
      carregar()
    } catch (err) {
      toast(err instanceof Error ? err.message : 'Falha na importação')
    } finally {
      if (arquivo.current) arquivo.current.value = ''
    }
  }

  return (
    <>
      <div className="topo">
        <div>
          <h1>Empresas</h1>
          <p>{pagina ? `${pagina.totalElementos} empresa(s) encontrada(s)` : 'Carregando…'}</p>
        </div>
        <div className="acoes">
          <input ref={arquivo} type="file" accept=".csv,text/csv" hidden onChange={(e) => importar(e.target.files?.[0])} />
          <button className="btn" onClick={() => arquivo.current?.click()}>Importar CSV</button>
          <button className="btn btn-primario" onClick={() => setNovo(true)}>+ Nova empresa</button>
        </div>
      </div>

      <div className="card">
        <div className="filtros">
          <input className="busca" placeholder="Buscar por nome ou CNPJ…" value={busca} onChange={(e) => setBusca(e.target.value)} />
          <select value={filtro.etapa} onChange={(e) => mudar('etapa', e.target.value)}>
            <option value="">Todas as etapas</option>
            {ETAPAS.map((e) => <option key={e} value={e}>{NOME_ETAPA[e]}</option>)}
          </select>
          <select value={params.get('semSite') ?? ''} onChange={(e) => mudar('semSite', e.target.value)}>
            <option value="">Com e sem site</option>
            <option value="true">Sem site</option>
            <option value="false">Com site</option>
          </select>
          <input placeholder="Cidade" value={filtro.cidade} onChange={(e) => mudar('cidade', e.target.value)} />
          <select value={filtro.uf} onChange={(e) => mudar('uf', e.target.value)}>
            <option value="">Todas as UFs</option>
            {UFS.map((u) => <option key={u}>{u}</option>)}
          </select>
        </div>

        {erro ? <div className="card-pad"><Erro erro={erro} /></div> : !pagina ? <Carregando /> : pagina.conteudo.length === 0 ? (
          <Vazio titulo="Nenhuma empresa encontrada">Cadastre uma empresa ou use “Buscar sem site” para importar do Google Maps.</Vazio>
        ) : (
          <>
            <div className="tabela-wrap">
              <table>
                <thead>
                  <tr><th>Empresa</th><th className="some-mobile">Cidade</th><th className="some-mobile">Telefone</th><th>Site</th><th>Etapa</th><th className="some-mobile">Valor</th><th className="some-mobile">Responsável</th></tr>
                </thead>
                <tbody>
                  {pagina.conteudo.map((e) => (
                    <tr key={e.id} className="clicavel" onClick={() => navegar(`/empresas/${e.id}`)}>
                      <td className="nome-cel"><strong>{nomeEmpresa(e)}</strong><span>{e.segmento ?? e.razaoSocial}</span></td>
                      <td className="some-mobile">{e.cidade ? `${e.cidade}${e.uf ? `/${e.uf}` : ''}` : '—'}</td>
                      <td className="some-mobile nowrap">{e.telefone ?? '—'}</td>
                      <td>{e.site ? <span className="badge badge-ok">Tem site</span> : <span className="badge badge-warn">Sem site</span>}</td>
                      <td><EtapaBadge etapa={e.etapa} /></td>
                      <td className="some-mobile nowrap">{reais(e.valorEstimado)}</td>
                      <td className="some-mobile">{e.responsavel?.nome ?? '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {pagina.totalPaginas > 1 && (
              <div className="paginacao">
                <span className="muted">Página {pagina.pagina + 1} de {pagina.totalPaginas}</span>
                <div className="acoes">
                  <button className="btn btn-sm" disabled={pagina.pagina === 0} onClick={() => mudar('page', String(pagina.pagina - 1))}>← Anterior</button>
                  <button className="btn btn-sm" disabled={pagina.pagina + 1 >= pagina.totalPaginas} onClick={() => mudar('page', String(pagina.pagina + 1))}>Próxima →</button>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {novo && <EmpresaFormModal aoFechar={() => setNovo(false)} aoSalvar={(e) => { setNovo(false); toast('Empresa cadastrada'); navegar(`/empresas/${e.id}`) }} />}

      {importacao && (
        <Modal titulo="Resultado da importação" aoFechar={() => setImportacao(null)}
          rodape={<button className="btn btn-primario" onClick={() => setImportacao(null)}>Ok</button>}>
          <div className="alerta alerta-ok">{importacao.importadas} empresa(s) importada(s).</div>
          {importacao.ignoradas > 0 && (
            <>
              <p><strong>{importacao.ignoradas} linha(s) ignorada(s):</strong></p>
              <ul className="pequeno muted">{importacao.erros.map((e, i) => <li key={i}>{e}</li>)}</ul>
            </>
          )}
          <p className="pequeno muted">Formato: CSV com cabeçalho. Coluna obrigatória <code>razaoSocial</code>; opcionais: nomeFantasia, cnpj, segmento, porte, cidade, uf, site, telefone, email, origem, valorEstimado.</p>
        </Modal>
      )}
    </>
  )
}
