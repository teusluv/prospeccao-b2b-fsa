import { createContext, useCallback, useContext, useRef, useState, type ReactNode } from 'react'

const Ctx = createContext<(msg: string) => void>(() => {})

export function ToastProvider({ children }: { children: ReactNode }) {
  const [msg, setMsg] = useState<string | null>(null)
  const timer = useRef<number | undefined>(undefined)
  const mostrar = useCallback((texto: string) => {
    setMsg(texto)
    window.clearTimeout(timer.current)
    timer.current = window.setTimeout(() => setMsg(null), 3500)
  }, [])
  return (
    <Ctx.Provider value={mostrar}>
      {children}
      {msg && <div className="toast" role="status">{msg}</div>}
    </Ctx.Provider>
  )
}

export const useToast = () => useContext(Ctx)
