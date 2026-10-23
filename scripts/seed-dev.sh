#!/usr/bin/env bash
# Popula o ambiente local com dados de exemplo (fictícios) via API.
# Uso: API=http://localhost:8080 ADMIN_EMAIL=... ADMIN_PASSWORD=... scripts/seed-dev.sh
set -euo pipefail

API=${API:-http://localhost:8080}/api/v1
ADMIN_EMAIL=${ADMIN_EMAIL:-admin@monitorjudicial.local}
ADMIN_PASSWORD=${ADMIN_PASSWORD:-admin-dev-12345}

json() { python3 -c "import sys,json;print(json.load(sys.stdin)$1)"; }

TOKEN=$(curl -sf -X POST "$API/tokens" -H 'Content-Type: application/json' \
  -d "{\"grantType\":\"PASSWORD\",\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}" | json "['accessToken']")
H=(-H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json')

cnj() { # sequência, TR
  python3 - "$1" "$2" <<'PY'
import sys
n, tr = sys.argv[1].zfill(7), sys.argv[2]
a, j, o = '2026', '8', '0100'
dd = 98 - int(n + a + j + tr + o + '00') % 97
print(f"{n}-{dd:02d}.{a}.{j}.{tr}.{o}")
PY
}

client() { # nome documento email telefone whatsapp
  curl -sf -X POST "$API/clients" "${H[@]}" -d "{\"name\":\"$1\",\"document\":\"$2\",\"email\":\"$3\",\"phone\":\"$4\",\"notifyEmail\":true,\"notifyWhatsapp\":$5}" | json "['id']"
}

process() { # cliente sequência TR assunto
  curl -sf -X POST "$API/processes" "${H[@]}" -d "{\"clientId\":\"$1\",\"number\":\"$(cnj "$2" "$3")\",\"subject\":\"$4\"}" > /dev/null
}

C1=$(client "Construtora Horizonte Ltda" "11.222.333/0001-81" "juridico@horizonte.example" "11987654321" true)
C2=$(client "Maria Aparecida Souza" "529.982.247-25" "maria.souza@example.com" "21999990000" true)
C3=$(client "Transportes Serra Azul S.A." "45.723.174/0001-10" "contencioso@serraazul.example" "" false)

process "$C1" 1001234 26 "Cobrança"
process "$C1" 1001991 26 "Rescisão contratual"
process "$C2" 804512 19 "Indenização por danos morais"
process "$C3" 5012877 13 "Execução de título extrajudicial"
process "$C2" 11456 17 "Alimentos"
process "$C3" 703318 07 "Direito do consumidor"

curl -sf -X POST "$API/users" "${H[@]}" -d "{\"name\":\"Maria Aparecida Souza\",\"email\":\"maria@cliente.example\",\"password\":\"cliente-dev-123\",\"role\":\"CUSTOMER\",\"clientId\":\"$C2\"}" > /dev/null

echo "Dados de exemplo criados. Cliente de teste: maria@cliente.example / cliente-dev-123"
