# Система управления пользователями AD
Write-Host "=== Active Directory User Management ===" -ForegroundColor Green

# Проверка доступности модуля ActiveDirectory
if (-not (Get-Module -ListAvailable -Name ActiveDirectory)) {
    Write-Host "Active Directory module is not available." -ForegroundColor Red
    Write-Host "This script requires RSAT (Remote Server Administration Tools) to be installed." -ForegroundColor Yellow
    return
}

Import-Module ActiveDirectory

class ADUserManager {
    [string]$Domain
    
    ADUserManager() {
        $this.Domain = (Get-ADDomain).DNSRoot
    }
    
    [array]FindUsers([hashtable]$criteria) {
        $filter = ""
        $properties = @("Name", "SamAccountName", "Enabled", "LastLogonDate", "Created")
        
        if ($criteria.ContainsKey("Name")) {
            $filter += "(Name -like '*$($criteria['Name'])*')"
        }
        
        if ($criteria.ContainsKey("Enabled")) {
            $filter += " -and (Enabled -eq `$$($criteria['Enabled']))"
        }
        
        if ($criteria.ContainsKey("Department")) {
            $filter += " -and (Department -like '*$($criteria['Department'])*')"
            $properties += "Department"
        }
        
        if ($criteria.ContainsKey("LastLogonDays")) {
            $date = (Get-Date).AddDays(-$criteria["LastLogonDays"])
            $filter += " -and (LastLogonDate -ge '$($date.ToString('yyyy-MM-dd'))')"
        }
        
        if (-not $filter) {
            $filter = "*"
        }
        
        try {
            $users = Get-ADUser -Filter $filter -Properties $properties | 
                    Select-Object Name, SamAccountName, UserPrincipalName, Enabled, 
                                Department, LastLogonDate, Created, DistinguishedName
            return $users
        }
        catch {
            Write-Error "Failed to search users: $($_.Exception.Message)"
            return @()
        }
    }
    
    [bool]CreateUser([hashtable]$userInfo) {
        try {
            # Генерация пароля если не предоставлен
            if (-not $userInfo.ContainsKey("Password")) {
                $userInfo["Password"] = $this.GenerateComplexPassword()
            }
            
            # Базовые параметры
            $newUserParams = @{
                Name = $userInfo["Name"]
                SamAccountName = $userInfo["SamAccountName"]
                UserPrincipalName = $userInfo["SamAccountName"] + "@" + $this.Domain
                AccountPassword = (ConvertTo-SecureString -String $userInfo["Password"] -AsPlainText -Force)
                Enabled = $true
            }
            
            # Дополнительные параметры
            if ($userInfo.ContainsKey("GivenName")) {
                $newUserParams["GivenName"] = $userInfo["GivenName"]
            }
            
            if ($userInfo.ContainsKey("Surname")) {
                $newUserParams["Surname"] = $userInfo["Surname"]
            }
            
            if ($userInfo.ContainsKey("Department")) {
                $newUserParams["Department"] = $userInfo["Department"]
            }
            
            if ($userInfo.ContainsKey("Path")) {
                $newUserParams["Path"] = $userInfo["Path"]
            }
            
            # Создание пользователя
            New-ADUser @newUserParams
            
            Write-Host "User created successfully: $($userInfo['SamAccountName'])" -ForegroundColor Green
            Write-Host "Generated password: $($userInfo['Password'])" -ForegroundColor Yellow
            Write-Host "Please change the password on first login." -ForegroundColor Yellow
            
            return $true
        }
        catch {
            Write-Error "Failed to create user: $($_.Exception.Message)"
            return $false
        }
    }
    
    [bool]ModifyUser([string]$samAccountName, [hashtable]$changes) {
        try {
            $user = Get-ADUser -Identity $samAccountName -ErrorAction Stop
            
            $setParams = @{}
            
            foreach ($key in $changes.Keys) {
                switch ($key) {
                    "Department" { $setParams["Department"] = $changes[$key] }
                    "Title" { $setParams["Title"] = $changes[$key] }
                    "Office" { $setParams["Office"] = $changes[$key] }
                    "Manager" { 
                        $manager = Get-ADUser -Filter "SamAccountName -eq '$($changes[$key])'"
                        if ($manager) {
                            $setParams["Manager"] = $manager.DistinguishedName
                        }
                    }
                    "Enabled" { $setParams["Enabled"] = [bool]$changes[$key] }
                }
            }
            
            if ($setParams.Count -gt 0) {
                Set-ADUser -Identity $samAccountName @setParams
                Write-Host "User modified successfully: $samAccountName" -ForegroundColor Green
                return $true
            } else {
                Write-Host "No valid changes specified" -ForegroundColor Yellow
                return $false
            }
        }
        catch {
            Write-Error "Failed to modify user: $($_.Exception.Message)"
            return $false
        }
    }
    
    [bool]DeleteUser([string]$samAccountName) {
        try {
            $user = Get-ADUser -Identity $samAccountName -ErrorAction Stop
            
            $confirmation = Read-Host "Are you sure you want to delete user $samAccountName? (y/n)"
            if ($confirmation -eq 'y' -or $confirmation -eq 'Y') {
                Remove-ADUser -Identity $samAccountName -Confirm:$false
                Write-Host "User deleted successfully: $samAccountName" -ForegroundColor Green
                return $true
            } else {
                Write-Host "Deletion cancelled" -ForegroundColor Yellow
                return $false
            }
        }
        catch {
            Write-Error "Failed to delete user: $($_.Exception.Message)"
            return $false
        }
    }
    
    [bool]ManageGroupMembership([string]$samAccountName, [string]$groupName, [string]$action) {
        try {
            $user = Get-ADUser -Identity $samAccountName -ErrorAction Stop
            $group = Get-ADGroup -Identity $groupName -ErrorAction Stop
            
            switch ($action) {
                "Add" {
                    Add-ADGroupMember -Identity $groupName -Members $samAccountName
                    Write-Host "User $samAccountName added to group $groupName" -ForegroundColor Green
                }
                "Remove" {
                    Remove-ADGroupMember -Identity $groupName -Members $samAccountName -Confirm:$false
                    Write-Host "User $samAccountName removed from group $groupName" -ForegroundColor Green
                }
            }
            
            return $true
        }
        catch {
            Write-Error "Failed to manage group membership: $($_.Exception.Message)"
            return $false
        }
    }
    
    [string]GenerateComplexPassword() {
        $length = 12
        $chars = @{
            Lower = "abcdefghijklmnopqrstuvwxyz"
            Upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            Numbers = "0123456789"
            Special = "!@#$%^&*"
        }
        
        $password = @()
        
        # Гарантировать минимум по одному символу каждого типа
        $password += $chars.Lower[(Get-Random -Maximum $chars.Lower.Length)]
        $password += $chars.Upper[(Get-Random -Maximum $chars.Upper.Length)]
        $password += $chars.Numbers[(Get-Random -Maximum $chars.Numbers.Length)]
        $password += $chars.Special[(Get-Random -Maximum $chars.Special.Length)]
        
        # Заполнить оставшуюся длину случайными символами
        $allChars = $chars.Lower + $chars.Upper + $chars.Numbers + $chars.Special
        for ($i = $password.Length; $i -lt $length; $i++) {
            $password += $allChars[(Get-Random -Maximum $allChars.Length)]
        }
        
        # Перемешать пароль
        $password = ($password | Sort-Object {Get-Random}) -join ''
        
        return $password
    }
    
    [void]GenerateUserReport([string]$outputPath) {
        try {
            $users = Get-ADUser -Filter * -Properties Department, Title, LastLogonDate, Created, Enabled |
                    Select-Object Name, SamAccountName, Department, Title, Enabled, 
                                LastLogonDate, Created, @{
                                Name = "LastLogonDays"; 
                                Expression = { 
                                    if ($_.LastLogonDate) { 
                                        [math]::Round(((Get-Date) - $_.LastLogonDate).TotalDays, 0) 
                                    } else { "Never" } 
                                }
                            }
            
            $report = @{
                Generated = Get-Date
                Domain = $this.Domain
                TotalUsers = $users.Count
                EnabledUsers = ($users | Where-Object Enabled).Count
                DisabledUsers = ($users | Where-Object { -not $_.Enabled }).Count
                UsersByDepartment = $users | Group-Object Department | 
                                  Select-Object Name, Count | Sort-Object Count -Descending
                InactiveUsers = $users | Where-Object { $_.LastLogonDays -ne "Never" -and $_.LastLogonDays -gt 90 } |
                              Select-Object Name, SamAccountName, LastLogonDays
                UserDetails = $users
            }
            
            $report | ConvertTo-Json -Depth 5 | Set-Content $outputPath
            Write-Host "User report generated: $outputPath" -ForegroundColor Cyan
        }
        catch {
            Write-Error "Failed to generate report: $($_.Exception.Message)"
        }
    }
}

# Основное меню
function Show-MainMenu {
    Write-Host "`n=== AD User Management System ===" -ForegroundColor Green
    Write-Host "1. Search Users"
    Write-Host "2. Create User"
    Write-Host "3. Modify User"
    Write-Host "4. Delete User"
    Write-Host "5. Manage Group Membership"
    Write-Host "6. Generate User Report"
    Write-Host "7. Exit"
    
    return Read-Host "`nSelect option (1-7)"
}

# Инициализация менеджера
$adManager = [ADUserManager]::new()

Write-Host "Connected to domain: $($adManager.Domain)" -ForegroundColor Green

do {
    $choice = Show-MainMenu
    
    switch ($choice) {
        "1" {
            Write-Host "`n--- Search Users ---" -ForegroundColor Yellow
            $criteria = @{}
            
            $name = Read-Host "Name (or part of name, press Enter to skip)"
            if ($name) { $criteria["Name"] = $name }
            
            $department = Read-Host "Department (press Enter to skip)"
            if ($department) { $criteria["Department"] = $department }
            
            $enabled = Read-Host "Enabled only? (y/n, press Enter for all)"
            if ($enabled -eq 'y') { $criteria["Enabled"] = $true }
            elseif ($enabled -eq 'n') { $criteria["Enabled"] = $false }
            
            $users = $adManager.FindUsers($criteria)
            if ($users) {
                $users | Format-Table -AutoSize
            } else {
                Write-Host "No users found matching the criteria" -ForegroundColor Yellow
            }
        }
        "2" {
            Write-Host "`n--- Create User ---" -ForegroundColor Yellow
            $userInfo = @{}
            
            $userInfo["Name"] = Read-Host "Full Name"
            $userInfo["SamAccountName"] = Read-Host "Username (SamAccountName)"
            $userInfo["GivenName"] = Read-Host "First Name"
            $userInfo["Surname"] = Read-Host "Last Name"
            $userInfo["Department"] = Read-Host "Department"
            
            $adManager.CreateUser($userInfo)
        }
        "3" {
            Write-Host "`n--- Modify User ---" -ForegroundColor Yellow
            $samAccountName = Read-Host "Username to modify"
            $changes = @{}
            
            $department = Read-Host "New Department (press Enter to skip)"
            if ($department) { $changes["Department"] = $department }
            
            $title = Read-Host "New Title (press Enter to skip)"
            if ($title) { $changes["Title"] = $title }
            
            $enabled = Read-Host "Enable/Disable? (e/d, press Enter to skip)"
            if ($enabled -eq 'e') { $changes["Enabled"] = $true }
            elseif ($enabled -eq 'd') { $changes["Enabled"] = $false }
            
            if ($changes.Count -gt 0) {
                $adManager.ModifyUser($samAccountName, $changes)
            }
        }
        "4" {
            Write-Host "`n--- Delete User ---" -ForegroundColor Yellow
            $samAccountName = Read-Host "Username to delete"
            $adManager.DeleteUser($samAccountName)
        }
        "5" {
            Write-Host "`n--- Manage Group Membership ---" -ForegroundColor Yellow
            $samAccountName = Read-Host "Username"
            $groupName = Read-Host "Group Name"
            $action = Read-Host "Action (Add/Remove)"
            
            $adManager.ManageGroupMembership($samAccountName, $groupName, $action)
        }
        "6" {
            Write-Host "`n--- Generate User Report ---" -ForegroundColor Yellow
            $outputPath = "AD_User_Report_$(Get-Date -Format 'yyyyMMdd_HHmmss').json"
            $adManager.GenerateUserReport($outputPath)
        }
        "7" {
            Write-Host "Goodbye!" -ForegroundColor Green
        }
        default {
            Write-Host "Invalid option" -ForegroundColor Red
        }
    }
    
    if ($choice -ne "7") {
        Read-Host "`nPress Enter to continue..."
    }
} while ($choice -ne "7")
