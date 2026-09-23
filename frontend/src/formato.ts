import type { EtapaFunil, PorteEmpresa, SituacaoLugar, TipoInteracao } from './api/tipos'

export const ETAPAS: EtapaFunil[] = ['NOVO', 'CONTATADO', 'QUALIFICADO', 'PROPOSTA', 'NEGOCIACAO', 'GANHO', 'PERDIDO']

export const NOME_ETAPA: Record<EtapaFunil, string> = {
  NOVO: 'Novo',
  CONTATADO: 'Contatado',
  QUALIFICADO: 'Qualificado',
  PROPOSTA: 'Proposta',
  NEGOCIACAO: 'Negociação',
  GANHO: 'Ganho',
  PERDIDO: 'Perdido',
}

export const PORTES: PorteEmpresa[] = ['MEI', 'MICRO', 'PEQUENA', 'MEDIA', 'GRANDE']
export const NOME_PORTE: Record<PorteEmpresa, string> = {
  MEI: 'MEI', MICRO: 'Micro', PEQUENA: 'Pequena', MEDIA: 'Média', GRANDE: 'Grande',
}

export const TIPOS_INTERACAO: TipoInteracao[] = ['LIGACAO', 'WHATSAPP', 'EMAIL', 'REUNIAO', 'VISITA', 'OUTRO']
export const NOME_TIPO: Record<TipoInteracao, string> = {
  LIGACAO: 'Ligação', EMAIL: 'E-mail', WHATSAPP: 'WhatsApp', REUNIAO: 'Reunião', VISITA: 'Visita', OUTRO: 'Outro',
}

export const NOME_SITUACAO: Record<SituacaoLugar, string> = {
  IMPORTADA: 'Sem site',
  JA_CADASTRADA: 'Já cadastrada',
  TEM_SITE: 'Tem site',
  FECHADA: 'Fechada',
}

export const UFS = ['AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG', 'PA', 'PB', 'PR',
  'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO']

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 })
export const reais = (v: number | null | undefined) => (v == null ? '—' : moeda.format(v))

const dataHora = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
const data = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'medium' })
export const formatarDataHora = (iso: string | null) => (iso ? dataHora.format(new Date(iso)) : '—')
export const formatarData = (iso: string | null) => (iso ? data.format(new Date(iso)) : '—')

export function formatarCnpj(cnpj: string | null) {
  if (!cnpj || cnpj.length !== 14) return cnpj ?? '—'
  return cnpj.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5')
}

export function nomeEmpresa(e: { nomeFantasia: string | null; razaoSocial: string }) {
  return e.nomeFantasia || e.razaoSocial
}

/** Link de WhatsApp a partir de um telefone brasileiro. */
export function linkWhatsapp(telefone: string | null) {
  if (!telefone) return null
  let digitos = telefone.replace(/\D/g, '')
  if (digitos.length < 10) return null
  if (!digitos.startsWith('55')) digitos = '55' + digitos
  return `https://wa.me/${digitos}`
}

/** Converte ISO para o valor de um <input type="datetime-local"> (hora local). */
export function paraInputDataHora(iso: string | null) {
  if (!iso) return ''
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function deInputDataHora(valor: string) {
  return valor ? new Date(valor).toISOString() : null
}
