import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/cliente'
import type { Empresa, EtapaFunil } from '../api/tipos'
import { useToast } from '../components/Toast'
import { Carregando, corEtapa, Erro } from '../components/ui'
import { ETAPAS, NOME_ETAPA, nomeEmpresa, reais } from '../formato'
import { MotivoPerdaModal } from './EmpresaDetalhe'

export default function Funil() {
  const toast = useToast()
  const [empresas, setEmpresas] = useState<Empresa[] | null>(null)
  const [erro, setErro] = useState<unknown>(null)
  const [arrastando, setArrastando] = useState<number | null>(null)
  const [alvo, setAlvo] = useState<EtapaFunil | null>(null)
  const [perdendo, setPerdendo] = useState<number | null>(null)
  const [total, setTotal] = useState(0)

  useEffect(() => {
    api.empresas({ size: 500, sort: 'atualizadoEm,desc' })
      .then((p) => { setEmpresas(p.conteudo); setTotal(p.totalElementos) })
      .catch(setErro)
  }, [])

  async function mover(id: number, etapa: EtapaFunil, motivo?: string) {
    const atual = empresas?.find((e) => e.id === id)
    if (!atual || atual.etapa === etapa) return
    if (etapa === 'PERDIDO' && !motivo) return setPerdendo(id)
    const anterior = empresas
    setEmpresas((l) => l!.map((e) => (e.id === id ? { ...e, etapa } : e)))
    try {
      const salvo = await api.mudarEtapa(id, etapa, motivo)
      setEmpresas((l) => l!.map((e) => (e.id === id ? salvo : e)))
    } catch (err) {
      setEmpresas(anterior)
      toast(err instanceof Error ? err.message : 'Erro ao mover')
    }
  }

  if (erro) return <Erro erro={erro} />
  if (!empresas) return <Carregando />

  return (
    <>
      <div className="topo">
        <div>
          <h1>Funil de vendas</h1>
          <p>Arraste os cartões entre as colunas para mudar a etapa.{total > empresas.length && ` Mostrando as ${empresas.length} mais recentes de ${total}.`}</p>
        </div>
      </div>
      <div className="kanban">
        {ETAPAS.map((etapa) => {
          const lista = empresas.filter((e) => e.etapa === etapa)
          const soma = lista.reduce((s, e) => s + (e.valorEstimado ?? 0), 0)
          return (
            <div key={etapa} className={`coluna${alvo === etapa ? ' alvo' : ''}`}
              onDragOver={(e) => { e.preventDefault(); setAlvo(etapa) }}
              onDragLeave={() => setAlvo((a) => (a === etapa ? null : a))}
              onDrop={(e) => { e.preventDefault(); setAlvo(null); if (arrastando) mover(arrastando, etapa) }}>
              <div className="coluna-topo">
                <h3><span className="ponto" style={{ background: corEtapa(etapa) }} />{NOME_ETAPA[etapa]}</h3>
                <small>{lista.length} · {reais(soma)}</small>
              </div>
              {lista.map((e) => (
                <div key={e.id} className={`cartao${arrastando === e.id ? ' arrastando' : ''}`} draggable
                  onDragStart={() => setArrastando(e.id)} onDragEnd={() => setArrastando(null)}>
                  <Link to={`/empresas/${e.id}`}><strong>{nomeEmpresa(e)}</strong></Link>
                  <div className="linha"><span>{e.cidade ?? e.segmento ?? '—'}</span><span>{reais(e.valorEstimado)}</span></div>
                  <div className="linha">
                    <span>{e.responsavel?.nome ?? ''}</span>
                    <select className="btn-sm" style={{ width: 'auto', height: 26, fontSize: 12 }} value={e.etapa}
                      onChange={(ev) => mover(e.id, ev.target.value as EtapaFunil)} aria-label="Mudar etapa">
                      {ETAPAS.map((et) => <option key={et} value={et}>{NOME_ETAPA[et]}</option>)}
                    </select>
                  </div>
                </div>
              ))}
            </div>
          )
        })}
      </div>
      {perdendo && <MotivoPerdaModal aoFechar={() => setPerdendo(null)} aoConfirmar={(m) => { const id = perdendo; setPerdendo(null); mover(id, 'PERDIDO', m) }} />}
    </>
  )
}
