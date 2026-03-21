Set-Location "C:\Users\camst\.openclaw\workspace\AgentOS"
$prompt = Get-Content -Raw "phase6-prompt.txt"
$result = claude --permission-mode bypassPermissions --print $prompt 2>&1
$result | Out-File -FilePath "phase6-output.txt" -Encoding UTF8
exit $LASTEXITCODE
