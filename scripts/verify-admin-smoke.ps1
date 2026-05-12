param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = "admin@123"
)

$ErrorActionPreference = "Stop"

function Step($label, [scriptblock]$action) {
    Write-Host "`n==> $label" -ForegroundColor Cyan
    & $action
    Write-Host "PASS: $label" -ForegroundColor Green
}

function Assert-True($condition, $message) {
    if (-not $condition) {
        throw $message
    }
}

Write-Host "Admin smoke verifier" -ForegroundColor Yellow
Write-Host "Base URL: $BaseUrl"

$script:token = $null
$script:routesBefore = @()
$script:driversBefore = @()
$script:paymentsBefore = @()
$script:dashboardBefore = $null
$script:newRoute = $null
$script:newDriver = $null
$script:newAssignment = $null

Step "Login as admin" {
    $loginBody = @{
        username = $AdminUsername
        password = $AdminPassword
    } | ConvertTo-Json
    $loginRes = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/auth/admin/login" -ContentType "application/json" -Body $loginBody
    $script:token = $loginRes.data.token.ToString().Trim()
    Assert-True ($script:token -and $script:token.Length -gt 20) "Admin login token missing"
}

$headers = @{
    Authorization = "Bearer $script:token"
    Accept = "application/json"
}

Step "Read baseline admin data" {
    $script:dashboardBefore = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/v1/admin/dashboard" -Headers $headers
    $script:driversBefore = (Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/v1/admin/drivers" -Headers $headers).data
    $script:routesBefore = (Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/v1/admin/routes" -Headers $headers).data
    $script:paymentsBefore = (Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/v1/admin/payments" -Headers $headers).data

    Assert-True ($script:dashboardBefore.success -eq $true) "Dashboard call failed"
    Assert-True ($script:driversBefore -ne $null) "Drivers response missing"
    Assert-True ($script:routesBefore -ne $null) "Routes response missing"
    Assert-True ($script:paymentsBefore -ne $null) "Payments response missing"
}

Step "Create route via admin API" {
    $routeBody = @{
        startCityId = 1
        endCityId = 2
        baseFare = 460
        defaultSeats = 6
    } | ConvertTo-Json
    $routeRes = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/admin/routes" -Headers $headers -ContentType "application/json" -Body $routeBody
    $script:newRoute = $routeRes.data
    Assert-True ($routeRes.success -eq $true) "Route creation call failed"
    Assert-True ($script:newRoute.routePlanId -gt 0) "Route ID missing after create"
}

Step "Create driver via admin API" {
    $suffix = Get-Random -Minimum 1000 -Maximum 9999
    $driverBody = @{
        fullName = "Smoke Admin Driver"
        phone = "90177$suffix"
        email = "smoke-driver-$suffix@greenmiles.in"
        address = "Hyderabad"
        vidProofNumber = "VID$suffix"
        licenseNumber = "LIC$suffix"
        status = "ACTIVE"
        rating = 4.6
        vehicleNumber = "TS09SM$suffix"
        rcNumber = "RCSM$suffix"
        vehicleType = "KIA_CARENS_7_SEATER"
        totalSeats = 7
        carStatus = "ACTIVE"
    } | ConvertTo-Json
    $driverRes = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/admin/drivers" -Headers $headers -ContentType "application/json" -Body $driverBody
    $script:newDriver = $driverRes.data
    Assert-True ($driverRes.success -eq $true) "Driver creation call failed"
    Assert-True ($script:newDriver.driverId -gt 0) "Driver ID missing after create"
    Assert-True ($script:newDriver.carId -gt 0) "Car ID missing for new driver"
}

Step "Assign ride to created route/driver" {
    $startTime = (Get-Date).AddHours(2).ToString("yyyy-MM-ddTHH:mm:ss", [System.Globalization.CultureInfo]::InvariantCulture)
    $assignBody = @{
        routePlanId = [int64]$script:newRoute.routePlanId
        driverId = [int64]$script:newDriver.driverId
        carId = [int64]$script:newDriver.carId
        startTime = $startTime
        availableSeats = 4
    } | ConvertTo-Json
    $assignRes = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/admin/ride-assignments" -Headers $headers -ContentType "application/json" -Body $assignBody
    $script:newAssignment = $assignRes.data
    Assert-True ($assignRes.success -eq $true) "Ride assignment call failed"
    Assert-True ($script:newAssignment.rideId -gt 0) "Ride ID missing after assignment"
}

Step "Verify post-state" {
    $driversAfter = (Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/v1/admin/drivers" -Headers $headers).data
    $routesAfter = (Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/v1/admin/routes" -Headers $headers).data
    $auditAfter = (Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/v1/admin/audit-logs?page=0&size=5" -Headers $headers).data
    $dashboardAfter = (Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/v1/admin/dashboard" -Headers $headers).data

    Assert-True (($driversAfter.Count -ge $script:driversBefore.Count + 1)) "Driver count did not increase"
    Assert-True (($routesAfter.Count -ge $script:routesBefore.Count + 1)) "Route count did not increase"
    Assert-True ($auditAfter.totalItems -ge 0) "Audit response invalid"
    Assert-True ($dashboardAfter.totalRoutePlans -ge $script:dashboardBefore.data.totalRoutePlans) "Dashboard route plan metric regressed"
}

Write-Host "`nSmoke verification complete." -ForegroundColor Green
Write-Host ("Created routeId={0}, driverId={1}, carId={2}, rideId={3}" -f $script:newRoute.routePlanId, $script:newDriver.driverId, $script:newDriver.carId, $script:newAssignment.rideId)
