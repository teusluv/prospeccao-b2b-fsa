import type {
  BuscaResponse, Contato, ContatoForm, Dashboard, Empresa, EmpresaForm, EtapaFunil,
  Fonte, Fontes, ImportacaoResponse, Interacao, InteracaoForm, LoginResponse, Pagina, Perfil, Usuario,
} from './tipos'

const BASE = (import.meta.env.VITE_API_URL as string | undefined)?.replace(/\/$/, '') ?? ''
const CHAVE_TOKEN = 'prospectradar.token'

export class ErroApi extends Error {
  constructor(public status: number, message: string, public campos?: Record<string, string>) {
    super(message)
  }
}

export function lerToken(): string | null {
  try {
    return localStorage.getItem(CHAVE_TOKEN)
  } catch {
    return null
  }
}

export function salvarToken(token: string | null) {
  try {
    if (token) localStorage.setItem(CHAVE_TOKEN, token)
    else localStorage.removeItem(CHAVE_TOKEN)
  } catch {
    // navegador sem storage: o login vale só enquanto a aba estiver aberta
  }
}

let aoExpirar: () => void = () => {}
export function registrarAoExpirar(fn: () => void) {
  aoExpirar = fn
}

async function req<T>(metodo: string, caminho: string, corpo?: unknown): Promise<T> {
  const headers: Record<string, string> = {}
  const token = lerToken()
  if (token) headers.Authorization = `Bearer ${token}`
  let body: BodyInit | undefined
  if (corpo instanceof FormData) {
    body = corpo
  } else if (corpo !== undefined) {
    headers['Content-Type'] = 'application/json'
    body = JSON.stringify(corpo)
  }

  let resp: Response
  try {
    resp = await fetch(BASE + caminho, { method: metodo, headers, body })
  } catch {
    throw new ErroApi(0, BASE
      ? 'Não foi possível falar com o servidor. Se ele estava parado, pode levar até 1 minuto para acordar — tente de novo.'
      : 'Não foi possível falar com o servidor. O backend está rodando?')
  }

  if (resp.status === 401 && token) {
    salvarToken(null)
    aoExpirar()
  }
  if (!resp.ok) {
    let mensagem = `Erro ${resp.status}`
    let campos: Record<string, string> | undefined
    try {
      const erro = await resp.json()
      mensagem = erro.detail || erro.title || mensagem
      campos = erro.campos
    } catch {
      // resposta sem corpo JSON
    }
    if (resp.status === 401 && !token) mensagem = 'E-mail ou senha inválidos'
    if (resp.status === 403) mensagem = 'Você não tem permissão para esta ação'
    throw new ErroApi(resp.status, mensagem, campos)
  }
  if (resp.status === 204) return undefined as T
  return resp.json() as Promise<T>
}

function qs(params: Record<string, string | number | boolean | null | undefined>) {
  const s = new URLSearchParams()
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null && v !== '') s.set(k, String(v))
  }
  const txt = s.toString()
  return txt ? `?${txt}` : ''
}

export const api = {
  login: (email: string, senha: string) => req<LoginResponse>('POST', '/api/auth/login', { email, senha }),
  me: () => req<Usuario>('GET', '/api/auth/me'),
  trocarSenha: (senhaAtual: string, novaSenha: string) =>
    req<void>('POST', '/api/auth/trocar-senha', { senhaAtual, novaSenha }),

  dashboard: () => req<Dashboard>('GET', '/api/dashboard'),

  empresas: (filtro: {
    busca?: string; etapa?: EtapaFunil | ''; segmento?: string; cidade?: string; uf?: string
    responsavelId?: number | null; semSite?: boolean | null; page?: number; size?: number; sort?: string
  }) => req<Pagina<Empresa>>('GET', '/api/empresas' + qs(filtro)),
  empresa: (id: number) => req<Empresa>('GET', `/api/empresas/${id}`),
  criarEmpresa: (dados: EmpresaForm) => req<Empresa>('POST', '/api/empresas', dados),
  atualizarEmpresa: (id: number, dados: EmpresaForm) => req<Empresa>('PUT', `/api/empresas/${id}`, dados),
  mudarEtapa: (id: number, etapa: EtapaFunil, motivoPerda?: string) =>
    req<Empresa>('PATCH', `/api/empresas/${id}/etapa`, { etapa, motivoPerda }),
  excluirEmpresa: (id: number) => req<void>('DELETE', `/api/empresas/${id}`),
  importarCsv: (arquivo: File) => {
    const fd = new FormData()
    fd.append('arquivo', arquivo)
    return req<ImportacaoResponse>('POST', '/api/empresas/importar', fd)
  },

  contatos: (empresaId: number) => req<Contato[]>('GET', `/api/empresas/${empresaId}/contatos`),
  criarContato: (empresaId: number, dados: ContatoForm) =>
    req<Contato>('POST', `/api/empresas/${empresaId}/contatos`, dados),
  atualizarContato: (id: number, dados: ContatoForm) => req<Contato>('PUT', `/api/contatos/${id}`, dados),
  excluirContato: (id: number) => req<void>('DELETE', `/api/contatos/${id}`),

  interacoes: (empresaId: number, page = 0) =>
    req<Pagina<Interacao>>('GET', `/api/empresas/${empresaId}/interacoes${qs({ page, size: 50 })}`),
  registrarInteracao: (empresaId: number, dados: InteracaoForm) =>
    req<Interacao>('POST', `/api/empresas/${empresaId}/interacoes`, dados),
  excluirInteracao: (id: number) => req<void>('DELETE', `/api/interacoes/${id}`),
  followUps: (ate: string | null, meus: boolean) => req<Interacao[]>('GET', '/api/follow-ups' + qs({ ate, meus })),
  concluirFollowUp: (id: number) => req<Interacao>('PATCH', `/api/interacoes/${id}/concluir-follow-up`),

  fontes: () => req<Fontes>('GET', '/api/prospeccao/fontes'),
  buscarSemSite: (dados: {
    fonte: Fonte; termo: string; cidade: string; uf?: string; maxResultados?: number
    redeSocialContaComoSemSite?: boolean; simular?: boolean
  }) => req<BuscaResponse>('POST', '/api/prospeccao/buscar', dados),

  usuarios: () => req<Usuario[]>('GET', '/api/usuarios'),
  criarUsuario: (dados: { nome: string; email: string; senha: string; perfil: Perfil }) =>
    req<Usuario>('POST', '/api/usuarios', dados),
  atualizarUsuario: (id: number, dados: { nome: string; perfil: Perfil; ativo: boolean; novaSenha?: string }) =>
    req<Usuario>('PUT', `/api/usuarios/${id}`, dados),
}
