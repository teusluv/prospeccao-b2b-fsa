#!/usr/bin/env bash
# Liga o backend (Java) e o site (React) juntos. Uso: ./iniciar.sh   — Ctrl+C desliga os dois.
set -e
cd "$(dirname "$0")"

if [ ! -f backend/.env ]; then
  cp backend/.env.example backend/.env
  echo "Criei o arquivo backend/.env. Abra, coloque a senha do banco e rode ./iniciar.sh de novo."
  echo "  open -e backend/.env"
  exit 1
fi

set -a
. backend/.env
set +a

# Node instalado pelo nvm
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && . "$NVM_DIR/nvm.sh"
command -v npm >/dev/null || { echo "Node.js não encontrado. Instale em https://nodejs.org"; exit 1; }

if [ ! -d frontend/node_modules ]; then
  echo "Instalando dependências do site (só na primeira vez)..."
  (cd frontend && npm install)
fi

# Ao sair (Ctrl+C ou erro), desliga tudo que este script abriu
trap 'trap - EXIT; kill 0' EXIT INT TERM

echo "Ligando o backend (pode levar ~30s)..."
(cd backend && ./mvnw -q spring-boot:run > ../backend.log 2>&1) &
BACKEND=$!

until curl -s localhost:8080/actuator/health >/dev/null 2>&1; do
  if ! kill -0 "$BACKEND" 2>/dev/null; then
    echo
    echo "O backend não subiu. Motivo:"
    grep -iE "caused by|tenant|password|route to host|refused|timeout|FATAL" backend.log | grep -v "^\s*at " | tail -3
    echo
    echo "Log completo em backend.log"
    exit 1
  fi
  sleep 2
done
echo "Backend no ar em http://localhost:8080"

echo "Ligando o site..."
cd frontend
npm run dev -- --open
