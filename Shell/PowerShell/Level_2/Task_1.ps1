# Калькулятор ИМТ с классами
class BMICalculator {
    [double]$Weight
    [double]$Height
    [double]$BMI
    [string]$Category
    
    BMICalculator([double]$weight, [double]$height) {
        $this.Weight = $weight
        $this.Height = $height
        $this.CalculateBMI()
    }
    
    [void]CalculateBMI() {
        $this.BMI = [math]::Round($this.Weight / ($this.Height * $this.Height), 2)
        $this.DetermineCategory()
    }
    
    [void]DetermineCategory() {
        if ($this.BMI -lt 18.5) {
            $this.Category = "Underweight"
        } elseif ($this.BMI -lt 25) {
            $this.Category = "Normal weight"
        } elseif ($this.BMI -lt 30) {
            $this.Category = "Overweight"
        } else {
            $this.Category = "Obese"
        }
    }
    
    [string]GetRecommendation() {
        switch ($this.Category) {
            "Underweight" { return "Consider increasing calorie intake and strength training" }
            "Normal weight" { return "Maintain your current healthy lifestyle" }
            "Overweight" { return "Consider moderate exercise and balanced diet" }
            "Obese" { return "Consult with healthcare professional for guidance" }
            default { return "No specific recommendation" }
        }
    }
}

# Основная программа
Write-Host "=== BMI Calculator ===" -ForegroundColor Green

do {
    try {
        $weight = [double](Read-Host "Enter your weight in kg")
        $height = [double](Read-Host "Enter your height in meters")
        
        if ($weight -le 0 -or $height -le 0) {
            throw "Weight and height must be positive numbers"
        }
        
        $bmiCalc = [BMICalculator]::new($weight, $height)
        
        Write-Host "`n--- Results ---" -ForegroundColor Yellow
        Write-Host "Weight: $($bmiCalc.Weight) kg"
        Write-Host "Height: $($bmiCalc.Height) m"
        Write-Host "BMI: $($bmiCalc.BMI)"
        Write-Host "Category: $($bmiCalc.Category)" -ForegroundColor $(if ($bmiCalc.Category -eq "Normal weight") { "Green" } else { "Yellow" })
        Write-Host "Recommendation: $($bmiCalc.GetRecommendation())"
        
        # Сохранение в историю
        $historyEntry = @{
            Timestamp = Get-Date
            Weight = $bmiCalc.Weight
            Height = $bmiCalc.Height
            BMI = $bmiCalc.BMI
            Category = $bmiCalc.Category
        }
        
        $historyFile = "bmi_history.json"
        $history = @()
        if (Test-Path $historyFile) {
            $history = Get-Content $historyFile | ConvertFrom-Json
        }
        $history += $historyEntry
        $history | ConvertTo-Json | Set-Content $historyFile
        
        Write-Host "`nSaved to history: $historyFile" -ForegroundColor Cyan
        
    } catch {
        Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    $continue = Read-Host "`nCalculate another BMI? (y/n)"
} while ($continue -eq 'y' -or $continue -eq 'Y')

Write-Host "Thank you for using BMI Calculator!" -ForegroundColor Green
