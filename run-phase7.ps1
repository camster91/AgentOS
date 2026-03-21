Set-Location "C:\Users\camst\.openclaw\workspace\AgentOS"
$prompt = Get-Content -Raw "phase7-prompt.txt"
$result = claude --permission-mode bypassPermissions --print $prompt 2>&1
$result | Out-File -FilePath "phase7-output.txt" -Encoding UTF8
exit $LASTEXITCODE
