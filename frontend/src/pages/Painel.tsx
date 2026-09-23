import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/cliente'
import type { Dashboard, Interacao } from '../api/tipos'
import { Carregando, corEtapa, Erro, Vazio } from '../components/ui'
import { formatarDataHora, NOME_ETAPA, reais } from '../formato'
import { useAuth } from '../auth'

export default function Painel() {
  const { usuario } = useAuth()
  const [dados, setDados] = useState<Dashboard | null>(null)
  const [followUps, setFollowUps] = useState<Interacao[]>([])
  const [erro, setErro] = useState<unknown>(null)

  useEffect(() => {
    Promise.all([api.dashboard(), api.followUps(null, true)])
      .then(([d, f]) => { setDados(d); setFollowUps(f) })
      .catch(setErro)
  }, [])

  if (erro) return <Erro erro={erro} />
  if (!dados) return <Carregando />

  const maior = Math.max(1, ...dados.funil.map((f) => f.quantidade))

  return (
    <>
      <div className="topo">
        <div>
          <h1>Olá, {usuario?.nome.split(' ')[0]}</h1>
          <p>Resumo da sua prospecção.</p>
        </div>
        <div className="acoes">
          <Link className="btn" to="/empresas">Ver empresas</Link>
          <Link className="btn btn-primario" to="/buscar">Buscar empresas sem site</Link>
        </div>
      </div>

      <div className="metricas">
        <div className="card metrica"><span>Empresas no funil</span><strong>{dados.totalEmpresas}</strong><small>{dados.interacoesUltimos7Dias} contatos nos últimos 7 dias</small></div>
        <div className="card metrica"><span>Valor em aberto</span><strong>{reais(dados.valorEmAberto)}</strong><small>negócios em andamento</small></div>
        <div className="card metrica"><span>Valor ganho</span><strong>{reais(dados.valorGanho)}</strong><small>conversão de {(dados.taxaConversao * 100).toFixed(0)}%</small></div>
        <div className={`card metrica${dados.followUpsAtrasados ? ' alerta-metrica' : ''}`}>
          <span>Follow-ups atrasados</span><strong>{dados.followUpsAtrasados}</strong><small>{dados.followUpsPendentes} pendentes no total</small>
        </div>
      </div>

      <div className="detalhe">
        <div className="card card-pad">
          <div className="card-titulo"><h2>Funil de vendas</h2><Link to="/funil" className="pequeno">Abrir quadro →</Link></div>
          <div className="barra-funil">
            {dados.funil.map((f) => (
              <div className="barra-linha" key={f.etapa}>
                <span>{NOME_ETAPA[f.etapa]}</span>
                <div className="barra-trilho">
                  <div className="barra-cheia" style={{ width: `${(f.quantidade / maior) * 100}%`, background: corEtapa(f.etapa) }}>
                    {f.quantidade > 0 && f.quantidade}
                  </div>
                </div>
                <span className="valor">{reais(f.valor)}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="card card-pad">
          <div className="card-titulo"><h2>Seus retornos para hoje</h2><Link to="/follow-ups" className="pequeno">Ver todos →</Link></div>
          {followUps.length === 0 ? (
            <Vazio titulo="Nada atrasado">Tudo em dia por aqui.</Vazio>
          ) : (
            followUps.slice(0, 6).map((f) => (
              <div className="contato" key={f.id}>
                <div className="contato-topo">
                  <Link to={`/empresas/${f.empresaId}`}><strong>{f.empresaNome}</strong></Link>
                  <span className="badge badge-perigo">{formatarDataHora(f.dataFollowUp)}</span>
                </div>
                <p className="muted pequeno">{f.proximoPasso || f.descricao}</p>
              </div>
            ))
          )}
        </div>
      </div>
    </>
  )
}
