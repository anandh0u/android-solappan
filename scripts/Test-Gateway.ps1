param([Parameter(Mandatory=$true)][string]$ProjectRef)
$ErrorActionPreference = 'Stop'
# Test-only accounts and IDs are created below and cleaned up in finally. Never print credentials.
$keys = npx --yes supabase@latest projects api-keys --project-ref $ProjectRef -o json | ConvertFrom-Json
if ($LASTEXITCODE -ne 0) { throw 'Could not obtain project test credentials.' }
$service = ($keys | Where-Object name -eq 'service_role').api_key
$public = ($keys | Where-Object name -eq 'anon').api_key
if (!$service -or !$public) { throw 'Required project API keys unavailable.' }
$url = "https://$ProjectRef.supabase.co"
$admin = @{apikey=$service;Authorization="Bearer $service"}
$created = @()
$responses = @()
function Check($condition, $label) { if (!$condition) { throw "FAILED: $label" }; Write-Output "PASS: $label" }
function Request($path, $headers, $body, $method='POST') {
    $args = @{Uri="$url/$path";Method=$method;Headers=$headers;SkipHttpErrorCheck=$true;TimeoutSec=100}
    if ($null -ne $body) { $args.ContentType='application/json'; $args.Body=($body | ConvertTo-Json -Depth 30 -Compress) }
    Invoke-WebRequest @args
}
try {
    $unauth = Request 'functions/v1/agent-gateway' @{apikey=$public} @{}
    Check ($unauth.StatusCode -eq 401) 'unauthenticated gateway denied'
    $sessionHeaders = @()
    for ($i=0; $i -lt 2; $i++) {
        $email = "sol-smoke-$([guid]::NewGuid().ToString('N'))@example.invalid"
        $password = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
        $new = Request 'auth/v1/admin/users' $admin @{email=$email;password=$password;email_confirm=$true}
        Check ($new.StatusCode -in 200,201) 'temporary test account created'
        $id = ($new.Content | ConvertFrom-Json).id
        $created += $id
        $login = Request 'auth/v1/token?grant_type=password' @{apikey=$public} @{email=$email;password=$password}
        Check ($login.StatusCode -eq 200) 'password authentication works'
        $token = ($login.Content | ConvertFrom-Json).access_token
        $sessionHeaders += @{apikey=$public;Authorization="Bearer $token"}
    }
    $body = @{input='Reply with the word READY.';instructions='Give a short answer.';tools=@()}
    $denied = Request 'functions/v1/agent-gateway' $sessionHeaders[0] $body
    Check ($denied.StatusCode -eq 429) 'verified but unapproved account denied'
    $rls = Request 'rest/v1/sol_beta_users?select=user_id' $sessionHeaders[0] $null 'GET'
    Check ($rls.StatusCode -in 401,403) 'client cannot read beta allowlist'
    $rpc = Request 'rest/v1/rpc/sol_reserve_request' $sessionHeaders[0] @{p_user=$created[0]}
    Check ($rpc.StatusCode -in 401,403) 'client cannot invoke quota reservation'
    $enable = Request 'rest/v1/sol_beta_users' $admin @{user_id=$created[0];enabled=$true}
    Check ($enable.StatusCode -eq 201) 'test account allowlisted'
    $response = Request 'functions/v1/agent-gateway' $sessionHeaders[0] $body
    Check ($response.StatusCode -eq 200) 'live gateway model request completed'
    $responseId = ($response.Content | ConvertFrom-Json).id
    $responses += $responseId
    $body.previous_response_id = $responseId
    $foreign = Request 'functions/v1/agent-gateway' $sessionHeaders[1] $body
    Check ($foreign.StatusCode -eq 403) 'cross-account continuation denied'
    $continued = Request 'functions/v1/agent-gateway' $sessionHeaders[0] $body
    Check ($continued.StatusCode -eq 200) 'same-account continuation completed'
    $responses += ($continued.Content | ConvertFrom-Json).id
    # Test a daily bucket to avoid timing-dependent failures at minute rollover.
    $bucket = "day:$($created[0]):$([DateTime]::UtcNow.ToString('yyyyMMdd'))"
    $usage = Request "rest/v1/sol_usage?bucket=eq.$bucket&select=requests" $admin $null 'GET'
    Check (($usage.Content | ConvertFrom-Json)[0].requests -eq 2) 'both model requests incremented quota'
    $quotaHeaders = @{apikey=$service;Authorization="Bearer $service";Prefer='resolution=merge-duplicates'}
    $seed = Request 'rest/v1/sol_usage' $quotaHeaders @{bucket=$bucket;requests=100;expires_at=[DateTime]::UtcNow.AddHours(25).ToString('o')}
    Check ($seed.StatusCode -in 200,201) 'test-only daily bucket set to limit'
    $reserve = Request 'rest/v1/rpc/sol_reserve_request' $admin @{p_user=$created[0]}
    Check ($reserve.StatusCode -eq 200 -and ($reserve.Content | ConvertFrom-Json) -eq $false) 'quota rejects excess requests'
    $limited = Request 'functions/v1/agent-gateway' $sessionHeaders[0] $body
    Check ($limited.StatusCode -eq 429) 'gateway enforces exhausted quota'
} finally {
    foreach ($id in $created) {
        $removed = Request "auth/v1/admin/users/$id" $admin $null 'DELETE'
        Check ($removed.StatusCode -eq 200) 'temporary test account removed'
        $pattern = [Uri]::EscapeDataString("*:$id`:*")
        $null = Request "rest/v1/sol_usage?bucket=like.$pattern" $admin $null 'DELETE'
    }
    $config = Get-Content -LiteralPath (Join-Path $PSScriptRoot '../local.properties') | ConvertFrom-StringData
    foreach ($id in $responses) {
        $removed = Invoke-WebRequest -Uri "https://api.openai.com/v1/responses/$id" -Method Delete -Headers @{Authorization="Bearer $($config.OPENAI_API_KEY)"} -SkipHttpErrorCheck
        Check ($removed.StatusCode -eq 200) 'test provider response deleted'
    }
}
