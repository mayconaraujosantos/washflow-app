<#
  Windows PATH on this class of machine can end up with a malformed entry
  (e.g. an installer that appends `...\Tailscale"` with a stray trailing
  quote). Go binaries built with os/exec (kind, podman, kubectl plugins...)
  abort their PATH search entirely when a segment like that fails to stat,
  so `kind`'s podman provider fails with "executable file not found in
  %PATH%" even though podman.exe is right there in a later entry.

  This wrapper rebuilds PATH for the child process only (never touches the
  user's real environment) by dropping segments that contain characters
  invalid in a Windows path, or that don't resolve to a real directory, then
  runs the requested command with that sanitized PATH.
#>
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Exe,

    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Args
)

$invalidChars = [System.IO.Path]::GetInvalidPathChars() -join ''
$pattern = "[$([regex]::Escape($invalidChars))\`"]"

$clean = ($env:PATH -split ';') | Where-Object {
    $_ -and ($_ -notmatch $pattern) -and (Test-Path -LiteralPath $_ -PathType Container)
} | Select-Object -Unique

$env:PATH = $clean -join ';'

& $Exe @Args
exit $LASTEXITCODE
