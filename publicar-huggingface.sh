#!/usr/bin/env bash
# Publica o backend (Java) num Space do Hugging Face: hospedagem gratuita, sem cartão.
# Uso: ./publicar-huggingface.sh SEU_USUARIO_DO_HUGGING_FACE [NOME_DO_SPACE]
# Antes, crie o Space no site (New Space -> SDK Docker -> Blank). Passo a passo no README.
set -euo pipefail
cd "$(dirname "$0")"
RAIZ="$(pwd)"

USUARIO="${1:-}"
SPACE="${2:-prospectradar-api}"
if [ -z "$USUARIO" ]; then
  echo "Uso: ./publicar-huggingface.sh SEU_USUARIO_DO_HUGGING_FACE [NOME_DO_SPACE]"
  exit 1
fi

echo "Cole o token de acesso do Hugging Face (tipo Write). Ele não aparece na tela nem fica salvo:"
read -rs TOKEN
echo
[ -n "$TOKEN" ] || { echo "Token vazio."; exit 1; }

REPO="https://huggingface.co/spaces/$USUARIO/$SPACE"
REPO_COM_TOKEN="https://$USUARIO:$TOKEN@huggingface.co/spaces/$USUARIO/$SPACE"
PASTA="$(mktemp -d)"
trap 'rm -rf "$PASTA"' EXIT

echo "Baixando o Space $USUARIO/$SPACE..."
if ! git -c credential.helper= clone -q "$REPO_COM_TOKEN" "$PASTA/space" 2>/dev/null; then
  echo "Não consegui abrir $REPO."
  echo "Confira se o Space já foi criado no site, se o usuário está certo e se o token é do tipo Write."
  exit 1
fi

cd "$PASTA/space"
# Substitui tudo pelo conteúdo atual do backend
git rm -rq --ignore-unmatch . >/dev/null
cp -R "$RAIZ/backend/.mvn" "$RAIZ/backend/src" .
cp "$RAIZ/backend/Dockerfile" "$RAIZ/backend/pom.xml" "$RAIZ/backend/mvnw" .
cp "$RAIZ/deploy/huggingface/README.md" .
chmod +x mvnw
git add -A

if git diff --cached --quiet; then
  echo "Nada mudou desde a última publicação."
else
  git -c user.name="$USUARIO" -c user.email="$USUARIO@users.noreply.huggingface.co" \
    commit -qm "Atualiza o backend ($(cd "$RAIZ" && git rev-parse --short HEAD 2>/dev/null || date +%F))"
  echo "Enviando..."
  git -c credential.helper= push -q "$REPO_COM_TOKEN" HEAD:main
fi

SUBDOMINIO="$(echo "$USUARIO-$SPACE" | tr '[:upper:]' '[:lower:]' | tr '._' '--')"
echo
echo "Pronto. Acompanhe a construção (uns 5 a 10 minutos) em:"
echo "  $REPO   (aba Logs)"
echo "Quando aparecer Running, o endereço do backend é:"
echo "  https://$SUBDOMINIO.hf.space"
echo "Teste: https://$SUBDOMINIO.hf.space/actuator/health"
