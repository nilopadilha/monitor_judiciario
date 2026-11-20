# Popula o ambiente local com dados de exemplo (fictícios) via API no Windows PowerShell.
# Uso: .\scripts\seed-dev.ps1 [-Api "http://localhost:8080"] [-AdminEmail "admin@monitorjudicial.local"] [-AdminPassword "admin-dev-12345"]
param(
    [string]$Api = $(if ($env:API) { $env:API } else { "http://localhost:8080" }),
    [string]$AdminEmail = $(if ($env:ADMIN_EMAIL) { $env:ADMIN_EMAIL } else { "admin@monitorjudicial.local" }),
    [string]$AdminPassword = $(if ($env:ADMIN_PASSWORD) { $env:ADMIN_PASSWORD } else { "admin-dev-12345" })
)

$ErrorActionPreference = "Stop"
$baseUri = "$($Api.TrimEnd('/'))/api/v1"

# 1. Autentica como ADMIN
$login = Invoke-RestMethod -Method Post -Uri "$baseUri/tokens" -ContentType "application/json" -Body (@{
    grantType = "PASSWORD"
    email     = $AdminEmail
    password  = $AdminPassword
} | ConvertTo-Json)

$headers = @{ Authorization = "Bearer $($login.accessToken)" }

# 2. Cria ou recupera os 3 Clientes de exemplo
$existingClients = (Invoke-RestMethod -Method Get -Uri "$baseUri/clients?size=100" -Headers $headers).content

function Ensure-Client([string]$name, [string]$document, [string]$email, [string]$phone, [bool]$notifyEmail, [bool]$notifyWhatsapp) {
    $found = $existingClients | Where-Object { $_.name -eq $name } | Select-Object -First 1
    if ($found) { return $found.id }
    $created = Invoke-RestMethod -Method Post -Uri "$baseUri/clients" -Headers $headers -ContentType "application/json" -Body (@{
        name           = $name
        document       = $document
        email          = $email
        phone          = $phone
        notifyEmail    = $notifyEmail
        notifyWhatsapp = $notifyWhatsapp
    } | ConvertTo-Json)
    return $created.id
}

$c1 = Ensure-Client "Construtora Horizonte Ltda" "11.222.333/0001-81" "juridico@horizonte.example" "11987654321" $true $true
$c2 = Ensure-Client "Maria Aparecida Souza" "529.982.247-25" "maria.souza@example.com" "21999990000" $true $true
$c3 = Ensure-Client "Transportes Serra Azul S.A." "45.723.174/0001-10" "contencioso@serraazul.example" "" $true $false

# 3. Cria os 6 Processos (números CNJ validados ISO 7064 mod 97-10)
$existingProcesses = (Invoke-RestMethod -Method Get -Uri "$baseUri/processes?size=100" -Headers $headers).content
$processos = @(
    @{ clientId = $c1; number = "1001234-83.2026.8.26.0100"; subject = "Cobrança" },
    @{ clientId = $c1; number = "1001991-77.2026.8.26.0100"; subject = "Rescisão contratual" },
    @{ clientId = $c2; number = "0804512-71.2026.8.19.0100"; subject = "Indenização por danos morais" },
    @{ clientId = $c3; number = "5012877-30.2026.8.13.0100"; subject = "Execução de título extrajudicial" },
    @{ clientId = $c2; number = "0011456-79.2026.8.17.0100"; subject = "Alimentos" },
    @{ clientId = $c3; number = "0703318-61.2026.8.07.0100"; subject = "Direito do consumidor" }
)

foreach ($p in $processos) {
    $digits = $p.number -replace '\D', ''
    $alreadyExists = $existingProcesses | Where-Object { ($_.number -replace '\D', '') -eq $digits }
    if (-not $alreadyExists) {
        Invoke-RestMethod -Method Post -Uri "$baseUri/processes" -Headers $headers -ContentType "application/json" -Body ($p | ConvertTo-Json) | Out-Null
    }
}

# 4. Cria usuário de teste com perfil CUSTOMER (se ainda não existir)
$existingUsers = (Invoke-RestMethod -Method Get -Uri "$baseUri/users?size=100" -Headers $headers).content
if (-not ($existingUsers | Where-Object { $_.email -eq "maria@cliente.example" })) {
    Invoke-RestMethod -Method Post -Uri "$baseUri/users" -Headers $headers -ContentType "application/json" -Body (@{
        name     = "Maria Aparecida Souza"
        email    = "maria@cliente.example"
        password = "cliente-dev-123"
        role     = "CUSTOMER"
        clientId = $c2
    } | ConvertTo-Json) | Out-Null
}

# 5. Dispara uma execução de monitoramento
try {
    Invoke-RestMethod -Method Post -Uri "$baseUri/monitoring-executions" -Headers $headers | Out-Null
} catch {
    # Ignora 409 caso já haja uma execução em andamento
}

Write-Host "Dados de exemplo criados e monitoramento disparado. Cliente de teste: maria@cliente.example / cliente-dev-123" -ForegroundColor Green
