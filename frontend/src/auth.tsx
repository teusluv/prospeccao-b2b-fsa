import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { api, lerToken, registrarAoExpirar, salvarToken } from './api/cliente'
import type { Usuario } from './api/tipos'

interface AuthCtx {
  usuario: Usuario | null
  carregando: boolean
  entrar: (email: string, senha: string) => Promise<void>
  sair: () => void
}

const Ctx = createContext<AuthCtx | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null)
  const [carregando, setCarregando] = useState(() => lerToken() !== null)

  const sair = useCallback(() => {
    salvarToken(null)
    setUsuario(null)
  }, [])

  useEffect(() => {
    registrarAoExpirar(() => setUsuario(null))
    if (!lerToken()) return
    api.me()
      .then(setUsuario)
      .catch(() => salvarToken(null))
      .finally(() => setCarregando(false))
  }, [])

  const entrar = useCallback(async (email: string, senha: string) => {
    const resp = await api.login(email, senha)
    salvarToken(resp.token)
    setUsuario(resp.usuario)
  }, [])

  return <Ctx.Provider value={{ usuario, carregando, entrar, sair }}>{children}</Ctx.Provider>
}

export function useAuth() {
  const ctx = useContext(Ctx)
  if (!ctx) throw new Error('useAuth fora do AuthProvider')
  return ctx
}
