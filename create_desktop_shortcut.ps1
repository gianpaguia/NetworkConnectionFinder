$projectDir = $PSScriptRoot
$desktopPath = [Environment]::GetFolderPath("Desktop")
$shortcutPath = Join-Path $desktopPath "Network Finder.lnk"
$targetBatch = Join-Path $projectDir "run_app.bat"

$wshShell = New-Object -ComObject WScript.Shell
$shortcut = $wshShell.CreateShortcut($shortcutPath)
$shortcut.TargetPath = $targetBatch
$shortcut.WorkingDirectory = $projectDir
$shortcut.Description = "Social Network Connection Finder"
$shortcut.WindowStyle = 1
$shortcut.IconLocation = "$env:SystemRoot\System32\shell32.dll,13"
$shortcut.Save()

Write-Host "Desktop shortcut created successfully at: $shortcutPath" -ForegroundColor Green
