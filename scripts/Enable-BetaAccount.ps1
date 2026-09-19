param(
    [Parameter(Mandatory=$true)][string]$ProjectRef,
    [Parameter(Mandatory=$true)][string]$Email
)
$ErrorActionPreference = 'Stop'
# Owner CLI authentication required; never embed these administrative credentials in Android.
$keys = npx --yes supabase@latest projects api-keys --project-ref $ProjectRef -o json | ConvertFrom-Json
if ($LASTEXITCODE -ne 0) { throw 'Supabase CLI authentication is required.' }
$service = ($keys | Where-Object name -eq 'service_role').api_key
if (!$service) { throw 'Administrative project key unavailable.' }
$url = "https://$ProjectRef.supabase.co"
$headers = @{apikey=$service;Authorization="Bearer $service"}
$matches = @()
for ($page=1; $page -le 100; $page++) {
    $result = Invoke-RestMethod -Uri "$url/auth/v1/admin/users?page=$page&per_page=100" -Headers $headers
    $matches += @($result.users | Where-Object { $_.email -ieq $Email.Trim() })
    if ($result.users.Count -lt 100) { break }
}
if ($matches.Count -ne 1) { throw 'Expected one existing account. Create and verify it in SOL first.' }
if (!$matches[0].email_confirmed_at -or $matches[0].is_anonymous) { throw 'Email verification is required before approval.' }
$headers.Prefer = 'resolution=merge-duplicates'
$body = @{user_id=$matches[0].id;enabled=$true} | ConvertTo-Json -Compress
$null = Invoke-RestMethod -Uri "$url/rest/v1/sol_beta_users" -Method Post -Headers $headers -ContentType application/json -Body $body
Write-Output 'Verified account enabled for the private beta.'
