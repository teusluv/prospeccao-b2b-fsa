import { useEffect, type ReactNode } from 'react'
import type { EtapaFunil } from '../api/tipos'
import { NOME_ETAPA } from '../formato'

export const corEtapa = (e: EtapaFunil) => `var(--etapa-${e.toLowerCase()})`

export function EtapaBadge({ etapa }: { etapa: EtapaFunil }) {
  return (
    <span className="badge" style={{ color: corEtapa(etapa) }}>
      <span className="ponto" />
      {NOME_ETAPA[etapa]}
    </span>
  )
}

export function Carregando() {
  return <div className="carregando"><div className="spinner" aria-label="Carregando" /></div>
}

export function Erro({ erro }: { erro: unknown }) {
  if (!erro) return null
  const msg = erro instanceof Error ? erro.message : String(erro)
  return <div className="alerta alerta-erro">{msg}</div>
}

export function Vazio({ titulo, children }: { titulo: string; children?: ReactNode }) {
  return <div className="vazio"><strong>{titulo}</strong>{children}</div>
}

export function Modal({ titulo, aoFechar, children, rodape }: {
  titulo: string; aoFechar: () => void; children: ReactNode; rodape?: ReactNode
}) {
  useEffect(() => {
    const esc = (e: KeyboardEvent) => e.key === 'Escape' && aoFechar()
    window.addEventListener('keydown', esc)
    return () => window.removeEventListener('keydown', esc)
  }, [aoFechar])
  return (
    <div className="fundo-modal" onMouseDown={(e) => e.target === e.currentTarget && aoFechar()}>
      <div className="card modal" role="dialog" aria-modal="true" aria-label={titulo}>
        <div className="modal-topo">
          <h2>{titulo}</h2>
          <button className="btn btn-sm btn-fantasma" onClick={aoFechar} aria-label="Fechar">✕</button>
        </div>
        <div className="modal-corpo">{children}</div>
        {rodape && <div className="modal-rodape">{rodape}</div>}
      </div>
    </div>
  )
}

/** Ícones de traço simples (24x24). */
const caminhos: Record<string, ReactNode> = {
  painel: <><rect x="3" y="3" width="7" height="9" rx="1.5" /><rect x="14" y="3" width="7" height="5" rx="1.5" /><rect x="14" y="12" width="7" height="9" rx="1.5" /><rect x="3" y="16" width="7" height="5" rx="1.5" /></>,
  empresas: <><path d="M3 21h18" /><path d="M5 21V7l7-4 7 4v14" /><path d="M9 21v-5h6v5" /><path d="M9 10h.01M15 10h.01" /></>,
  funil: <><path d="M3 4h18l-7 8v6l-4 2v-8z" /></>,
  radar: <><circle cx="12" cy="12" r="9" /><circle cx="12" cy="12" r="5" /><path d="M12 12l6-6" /></>,
  sino: <><path d="M6 8a6 6 0 1 1 12 0c0 7 3 9 3 9H3s3-2 3-9" /><path d="M10.3 21a1.9 1.9 0 0 0 3.4 0" /></>,
  usuarios: <><circle cx="9" cy="8" r="4" /><path d="M2 21a7 7 0 0 1 14 0" /><path d="M16 4a4 4 0 0 1 0 8M22 21a7 7 0 0 0-4-6.3" /></>,
  conta: <><circle cx="12" cy="8" r="4" /><path d="M4 21a8 8 0 0 1 16 0" /></>,
  menu: <><path d="M4 6h16M4 12h16M4 18h16" /></>,
}

export function Icone({ nome }: { nome: keyof typeof caminhos | string }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {caminhos[nome]}
    </svg>
  )
}
