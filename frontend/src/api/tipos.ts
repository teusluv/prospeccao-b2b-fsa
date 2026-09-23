export type Perfil = 'ADMIN' | 'VENDEDOR'
export type EtapaFunil = 'NOVO' | 'CONTATADO' | 'QUALIFICADO' | 'PROPOSTA' | 'NEGOCIACAO' | 'GANHO' | 'PERDIDO'
export type PorteEmpresa = 'MEI' | 'MICRO' | 'PEQUENA' | 'MEDIA' | 'GRANDE'
export type TipoInteracao = 'LIGACAO' | 'EMAIL' | 'WHATSAPP' | 'REUNIAO' | 'VISITA' | 'OUTRO'

export interface Usuario {
  id: number
  nome: string
  email: string
  perfil: Perfil
  ativo: boolean
  criadoEm: string
}

export interface UsuarioResumo {
  id: number
  nome: string
}

export interface LoginResponse {
  token: string
  tipo: string
  expiraEm: string
  usuario: Usuario
}

export interface Empresa {
  id: number
  razaoSocial: string
  nomeFantasia: string | null
  cnpj: string | null
  segmento: string | null
  porte: PorteEmpresa | null
  cidade: string | null
  uf: string | null
  site: string | null
  telefone: string | null
  email: string | null
  origem: string | null
  etapa: EtapaFunil
  valorEstimado: number | null
  motivoPerda: string | null
  observacoes: string | null
  responsavel: UsuarioResumo | null
  criadoEm: string
  atualizadoEm: string
}

export type EmpresaForm = {
  razaoSocial: string
  nomeFantasia?: string | null
  cnpj?: string | null
  segmento?: string | null
  porte?: PorteEmpresa | null
  cidade?: string | null
  uf?: string | null
  site?: string | null
  telefone?: string | null
  email?: string | null
  origem?: string | null
  etapa?: EtapaFunil | null
  valorEstimado?: number | null
  observacoes?: string | null
  responsavelId?: number | null
}

export interface Contato {
  id: number
  empresaId: number
  nome: string
  cargo: string | null
  email: string | null
  telefone: string | null
  linkedin: string | null
  principal: boolean
  criadoEm: string
}

export type ContatoForm = Omit<Contato, 'id' | 'empresaId' | 'criadoEm'>

export interface Interacao {
  id: number
  empresaId: number
  empresaNome: string
  contatoId: number | null
  contatoNome: string | null
  usuario: UsuarioResumo
  tipo: TipoInteracao
  descricao: string
  dataHora: string
  proximoPasso: string | null
  dataFollowUp: string | null
  followUpConcluido: boolean
  criadoEm: string
}

export interface InteracaoForm {
  tipo: TipoInteracao
  descricao: string
  contatoId?: number | null
  proximoPasso?: string | null
  dataFollowUp?: string | null
}

export interface Pagina<T> {
  conteudo: T[]
  pagina: number
  tamanho: number
  totalElementos: number
  totalPaginas: number
}

export interface Dashboard {
  totalEmpresas: number
  funil: { etapa: EtapaFunil; quantidade: number; valor: number }[]
  valorEmAberto: number
  valorGanho: number
  taxaConversao: number
  followUpsPendentes: number
  followUpsAtrasados: number
  interacoesUltimos7Dias: number
}

export type SituacaoLugar = 'IMPORTADA' | 'JA_CADASTRADA' | 'TEM_SITE' | 'FECHADA'

export type Fonte = 'OPENSTREETMAP' | 'GOOGLE_MAPS'

export interface LugarEncontrado {
  idExterno: string
  nome: string | null
  categoria: string | null
  endereco: string | null
  telefone: string | null
  site: string | null
  mapaUrl: string | null
  situacao: SituacaoLugar
  empresaId: number | null
}

export interface Fontes {
  googleMapsDisponivel: boolean
  categoriasOpenStreetMap: { chave: string; nome: string }[]
}

export interface BuscaResponse {
  fonte: Fonte
  encontradas: number
  semSite: number
  importadas: number
  jaCadastradas: number
  simulacao: boolean
  lugares: LugarEncontrado[]
}

export interface ImportacaoResponse {
  importadas: number
  ignoradas: number
  erros: string[]
}
