#!/bin/bash

# Конфигурация
LOG_FILE="system_monitor.log"
WEB_DIR="monitor_dashboard"
INTERVAL=2

# Функция получения системных метрик
get_system_metrics() {
    local metrics=()
    
    # CPU usage
    metrics+=("cpu:$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | cut -d'%' -f1)")
    
    # Memory usage
    metrics+=("memory:$(free | grep Mem | awk '{printf "%.1f", $3/$2 * 100.0}')")
    
    # Disk usage
    metrics+=("disk:$(df / | awk 'NR==2 {print $5}' | cut -d'%' -f1)")
    
    # Network - получение полученных/переданных байт
    local rx_bytes=$(cat /sys/class/net/$(ip route | awk '/default/ {print $5}')/statistics/rx_bytes 2>/dev/null || echo "0")
    local tx_bytes=$(cat /sys/class/net/$(ip route | awk '/default/ {print $5}')/statistics/tx_bytes 2>/dev/null || echo "0")
    metrics+=("network_rx:$rx_bytes")
    metrics+=("network_tx:$tx_bytes")
    
    # Load average
    metrics+=("load:$(cat /proc/loadavg | awk '{print $1}')")
    
    printf '%s\n' "${metrics[@]}"
}

# Функция генерации JSON данных
generate_json_data() {
    local timestamp=$(date +%s)
    local metrics_json=""
    
    while IFS=: read -r key value; do
        if [ -n "$metrics_json" ]; then
            metrics_json="$metrics_json,"
        fi
        metrics_json="$metrics_json\"$key\":$value"
    done < <(get_system_metrics)
    
    cat > "$WEB_DIR/data.json" << EOF
{
    "timestamp": $timestamp,
    "metrics": {
        $metrics_json
    }
}
EOF
}

# Функция создания веб-интерфейса
create_web_interface() {
    mkdir -p "$WEB_DIR"
    
    cat > "$WEB_DIR/index.html" << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>System Monitor Dashboard</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }
        .dashboard { max-width: 1200px; margin: 0 auto; }
        .header { background: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .metrics-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin-bottom: 20px; }
        .metric-card { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .metric-value { font-size: 24px; font-weight: bold; margin: 10px 0; }
        .metric-good { color: #28a745; }
        .metric-warning { color: #ffc107; }
        .metric-danger { color: #dc3545; }
        .charts { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
        .chart-container { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .alert { padding: 10px; border-radius: 4px; margin: 10px 0; }
        .alert-warning { background: #fff3cd; border: 1px solid #ffeaa7; }
        .alert-danger { background: #f8d7da; border: 1px solid #f5c6cb; }
    </style>
</head>
<body>
    <div class="dashboard">
        <div class="header">
            <h1>🖥️ System Monitor Dashboard</h1>
            <p>Last updated: <span id="lastUpdate">-</span></p>
        </div>

        <div id="alertsContainer"></div>

        <div class="metrics-grid" id="metricsGrid">
            <!-- Metrics will be populated by JavaScript -->
        </div>

        <div class="charts">
            <div class="chart-container">
                <canvas id="cpuChart"></canvas>
            </div>
            <div class="chart-container">
                <canvas id="memoryChart"></canvas>
            </div>
        </div>
    </div>

    <script>
        let cpuChart, memoryChart;
        let cpuData = [], memoryData = [], timestamps = [];

        function updateMetrics(data) {
            document.getElementById('lastUpdate').textContent = new Date(data.timestamp * 1000).toLocaleString();
            
            const metrics = data.metrics;
            const alertsContainer = document.getElementById('alertsContainer');
            alertsContainer.innerHTML = '';

            // Check for alerts
            if (metrics.cpu > 80) {
                showAlert('High CPU usage detected!', 'danger');
            }
            if (metrics.memory > 85) {
                showAlert('High memory usage detected!', 'danger');
            }
            if (metrics.disk > 90) {
                showAlert('Low disk space!', 'warning');
            }

            // Update metrics grid
            const metricsGrid = document.getElementById('metricsGrid');
            metricsGrid.innerHTML = `
                <div class="metric-card">
                    <h3>CPU Usage</h3>
                    <div class="metric-value ${getMetricClass(metrics.cpu, 70, 85)}">${metrics.cpu}%</div>
                </div>
                <div class="metric-card">
                    <h3>Memory Usage</h3>
                    <div class="metric-value ${getMetricClass(metrics.memory, 75, 90)}">${metrics.memory}%</div>
                </div>
                <div class="metric-card">
                    <h3>Disk Usage</h3>
                    <div class="metric-value ${getMetricClass(metrics.disk, 80, 95)}">${metrics.disk}%</div>
                </div>
                <div class="metric-card">
                    <h3>System Load</h3>
                    <div class="metric-value ${getMetricClass(metrics.load, 1.0, 2.0)}">${metrics.load}</div>
                </div>
            `;

            // Update charts
            updateCharts(metrics);
        }

        function getMetricClass(value, warning, danger) {
            if (value >= danger) return 'metric-danger';
            if (value >= warning) return 'metric-warning';
            return 'metric-good';
        }

        function showAlert(message, type) {
            const alertsContainer = document.getElementById('alertsContainer');
            const alert = document.createElement('div');
            alert.className = `alert alert-${type}`;
            alert.textContent = message;
            alertsContainer.appendChild(alert);
        }

        function updateCharts(metrics) {
            const now = new Date().toLocaleTimeString();
            
            // Add new data points
            cpuData.push(metrics.cpu);
            memoryData.push(metrics.memory);
            timestamps.push(now);

            // Keep only last 20 data points
            if (cpuData.length > 20) {
                cpuData.shift();
                memoryData.shift();
                timestamps.shift();
            }

            // Update or create charts
            if (!cpuChart) {
                createCharts();
            } else {
                cpuChart.update('none');
                memoryChart.update('none');
            }
        }

        function createCharts() {
            const ctx1 = document.getElementById('cpuChart').getContext('2d');
            const ctx2 = document.getElementById('memoryChart').getContext('2d');

            cpuChart = new Chart(ctx1, {
                type: 'line',
                data: {
                    labels: timestamps,
                    datasets: [{
                        label: 'CPU Usage %',
                        data: cpuData,
                        borderColor: 'rgb(255, 99, 132)',
                        tension: 0.1
                    }]
                },
                options: {
                    responsive: true,
                    scales: {
                        y: {
                            min: 0,
                            max: 100
                        }
                    }
                }
            });

            memoryChart = new Chart(ctx2, {
                type: 'line',
                data: {
                    labels: timestamps,
                    datasets: [{
                        label: 'Memory Usage %',
                        data: memoryData,
                        borderColor: 'rgb(54, 162, 235)',
                        tension: 0.1
                    }]
                },
                options: {
                    responsive: true,
                    scales: {
                        y: {
                            min: 0,
                            max: 100
                        }
                    }
                }
            });
        }

        // Poll for updates
        function fetchData() {
            fetch('data.json?' + new Date().getTime())
                .then(response => response.json())
                .then(data => updateMetrics(data))
                .catch(error => console.error('Error fetching data:', error));
        }

        // Update every 2 seconds
        setInterval(fetchData, 2000);
        fetchData(); // Initial load
    </script>
</body>
</html>
EOF

    echo "Web interface created in: $WEB_DIR/"
}

# Функция запуска веб-сервера
start_web_server() {
    local port="${1:-8080}"
    
    # Создание веб-интерфейса
    create_web_interface
    
    echo "Starting web server on port $port"
    echo "Open http://localhost:$port in your browser"
    echo "Press Ctrl+C to stop"
    
    # Запуск Python HTTP сервера
    cd "$WEB_DIR" && python3 -m http.server "$port" 2>/dev/null || \
    cd "$WEB_DIR" && python -m SimpleHTTPServer "$port" 2>/dev/null
}

# Функция мониторинга в реальном времени
start_monitoring() {
    echo "Starting system monitoring..."
    echo "Web dashboard: http://localhost:8080"
    echo "Log file: $LOG_FILE"
    echo "Press Ctrl+C to stop"
    
    # Перехват сигнала для graceful shutdown
    trap 'echo -e "\nMonitoring stopped"; exit 0' INT
    
    while true; do
        # Генерация JSON данных
        generate_json_data
        
        # Логирование в файл
        {
            echo "=== $(date) ==="
            get_system_metrics | while IFS=: read -r key value; do
                echo "$key: $value"
            done
            echo
        } >> "$LOG_FILE"
        
        sleep "$INTERVAL"
    done
}

main() {
    local mode="${1:-monitor}"
    
    case "$mode" in
        "monitor")
            start_monitoring
            ;;
        "server")
            start_web_server "$2"
            ;;
        "setup")
            create_web_interface
            echo "Web dashboard setup completed"
            ;;
        *)
            echo "Usage: $0 [monitor|server|setup]"
            echo "  monitor - Start system monitoring (default)"
            echo "  server [port] - Start web server (default port: 8080)"
            echo "  setup - Create web interface only"
            ;;
    esac
}

main "$@"
