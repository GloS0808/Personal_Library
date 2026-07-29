$gradleFile = "app/build.gradle.kts"

if (Test-Path $gradleFile) {
    $content = Get-Content $gradleFile -Raw

    # Increment versionCode
    if ($content -match 'versionCode\s*=\s*(\d+)') {
        $oldVc = [int]$matches[1]
        $newVc = $oldVc + 1
        $content = $content -replace "versionCode\s*=\s*$oldVc", "versionCode = $newVc"
        Write-Host "Incremented versionCode: $oldVc -> $newVc"
    }

    # Increment versionName
    if ($content -match 'versionName\s*=\s*"([\d.]+)(.*?)"') {
        $fullVersion = $matches[1]
        $suffix = $matches[2]

        $parts = $fullVersion.Split('.')
        $lastIndex = $parts.Length - 1
        $lastPart = $parts[$lastIndex]

        $newVal = [int]$lastPart + 1
        # Preserve leading zeros if any
        $newLastPart = $newVal.ToString().PadLeft($lastPart.Length, '0')
        $parts[$lastIndex] = $newLastPart
        $newVersion = [string]::Join(".", $parts)

        $oldLine = "versionName = `"$fullVersion$suffix`""
        $newLine = "versionName = `"$newVersion$suffix`""
        $content = $content.Replace($oldLine, $newLine)
        Write-Host "Incremented versionName: $fullVersion$suffix -> $newVersion$suffix"
    }

    Set-Content $gradleFile $content -NoNewline
    Write-Host "Successfully updated $gradleFile"
} else {
    Write-Error "Could not find $gradleFile"
}
