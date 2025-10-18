#!/bin/bash

# Конфигурация API
WEATHER_API_KEY="your_api_key_here"  # Заменить на реальный ключ
TELEGRAM_BOT_TOKEN="your_bot_token_here"
TELEGRAM_CHAT_ID="your_chat_id_here"
GITHUB_USER="your_username_here"

# Функция получения данных о погоде
get_weather() {
    local city="${1:-Moscow}"
    
    log "Getting weather for: $city"
    
    # Использование OpenWeatherMap API
    local response=$(curl -s "http://api.openweathermap.org/data/2.5/weather?q=$city&appid=$WEATHER_API_KEY&units=metric" 2>/dev/null)
    
    if [ -z "$response" ] || echo "$response" | grep -q "error"; then
        echo "Failed to get weather data"
        return 1
    fi
    
    local temp=$(echo "$response" | grep -o '"temp":[^,]*' | cut -d: -f2)
    local humidity=$(echo "$response" | grep -o '"humidity":[^,]*' | cut -d: -f2)
    local description=$(echo "$response" | grep -o '"description":"[^"]*' | cut -d'"' -f4)
    
    echo "🌤️  Weather in $city:"
    echo "   Temperature: ${temp}°C"
    echo "   Humidity: ${humidity}%"
    echo "   Conditions: ${description}"
}

# Функция отправки уведомления в Telegram
send_telegram_message() {
    local message="$1"
    
    if [ "$TELEGRAM_BOT_TOKEN" = "your_bot_token_here" ]; then
        echo "Telegram bot token not configured"
        return 1
    fi
    
    log "Sending Telegram message..."
    
    local response=$(curl -s -X POST \
        "https://api.telegram.org/bot$TELEGRAM_BOT_TOKEN/sendMessage" \
        -d "chat_id=$TELEGRAM_CHAT_ID" \
        -d "text=$message" \
        -d "parse_mode=Markdown")
    
    if echo "$response" | grep -q '"ok":true'; then
        echo "✓ Telegram message sent"
        return 0
    else
        echo "✗ Failed to send Telegram message"
        return 1
    fi
}

# Функция парсинга RSS ленты
parse_rss_feed() {
    local rss_url="${1:-https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en}"
    local max_items=5
    
    log "Parsing RSS feed: $rss_url"
    
    local response=$(curl -s "$rss_url" 2>/dev/null)
    
    if [ -z "$response" ]; then
        echo "Failed to fetch RSS feed"
        return 1
    fi
    
    echo "📰 Latest News:"
    
    # Простой парсинг RSS с помощью grep и sed
    echo "$response" | grep -E '<title>|</title>' | \
    sed -e 's/<title>//g' -e 's/<\/title>//g' -e 's/<[^>]*>//g' | \
    head -$((max_items * 2)) | \
    while read -r line; do
        if [ -n "$line" ] && [ "$line" != "CDATA" ]; then
            echo "   • $line"
        fi
    done
}

# Функция работы с GitHub API
get_github_info() {
    local username="${1:-$GITHUB_USER}"
    
    log "Getting GitHub info for: $username"
    
    local response=$(curl -s "https://api.github.com/users/$username" 2>/dev/null)
    
    if [ -z "$response" ] || echo "$response" | grep -q "Not Found"; then
        echo "User not found: $username"
        return 1
    fi
    
    local name=$(echo "$response" | grep -o '"name":"[^"]*' | cut -d'"' -f4)
    local repos=$(echo "$response" | grep -o '"public_repos":[^,]*' | cut -d: -f2)
    local followers=$(echo "$response" | grep -o '"followers":[^,]*' | cut -d: -f2)
    local following=$(echo "$response" | grep -o '"following":[^,]*' | cut -d: -f2)
    
    echo "🐙 GitHub Profile: $name (@$username)"
    echo "   📚 Public Repositories: $repos"
    echo "   👥 Followers: $followers"
    echo "   🔄 Following: $following"
}

# Функция получения репозиториев пользователя
get_github_repos() {
    local username="${1:-$GITHUB_USER}"
    local max_repos=5
    
    log "Getting repositories for: $username"
    
    local response=$(curl -s "https://api.github.com/users/$username/repos?sort=updated&per_page=$max_repos" 2>/dev/null)
    
    if [ -z "$response" ]; then
        echo "Failed to fetch repositories"
        return 1
    fi
    
    echo "📦 Recent Repositories:"
    
    # Парсинг JSON ответа
    echo "$response" | grep -E '"name"|"description"|"stargazers_count"' | \
    while read -r line; do
        if echo "$line" | grep -q '"name"'; then
            repo_name=$(echo "$line" | cut -d'"' -f4)
        elif echo "$line" | grep -q '"description"'; then
            repo_desc=$(echo "$line" | cut -d'"' -f4)
            if [ "$repo_desc" = "null" ]; then
                repo_desc="No description"
            fi
        elif echo "$line" | grep -q '"stargazers_count"'; then
            stars=$(echo "$line" | grep -o '[0-9]*')
            echo "   ⭐ $repo_name ($stars stars)"
            echo "      $repo_desc"
            echo
        fi
    done
}

# Функция создания дашборда
create_dashboard() {
    local dashboard_file="api_dashboard_$(date +%Y%m%d_%H%M%S).html"
    
    log "Creating dashboard: $dashboard_file"
    
    # Получение данных для дашборда
    local weather_info=$(get_weather "Moscow" | sed 's/&/\\&/g')
    local github_info=$(get_github_info "$GITHUB_USER" | sed 's/&/\\&/g')
    
    cat > "$dashboard_file" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>API Integration Dashboard</title>
    <style>
        body { 
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
            margin: 0; 
            padding: 20px; 
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
        }
        .dashboard { 
            max-width: 1200px; 
            margin: 0 auto; 
        }
        .header { 
            text-align: center; 
            color: white; 
            margin-bottom: 30px;
        }
        .grid { 
            display: grid; 
            grid-template-columns: repeat(auto-fit, minmax(350px, 1fr)); 
            gap: 20px; 
        }
        .card { 
            background: white; 
            padding: 25px; 
            border-radius: 15px; 
            box-shadow: 0 10px 30px rgba(0,0,0,0.2);
            transition: transform 0.3s ease;
        }
        .card:hover {
            transform: translateY(-5px);
        }
        .card h2 { 
            color: #333; 
            border-bottom: 2px solid #667eea; 
            padding-bottom: 10px; 
            margin-top: 0;
        }
        .news-item { 
            border-left: 3px solid #667eea; 
            padding-left: 15px; 
            margin: 10px 0; 
        }
        .weather-icon { 
            font-size: 48px; 
            text-align: center; 
            margin: 10px 0; 
        }
        .stats { 
            display: grid; 
            grid-template-columns: 1fr 1fr; 
            gap: 10px; 
            margin-top: 15px;
        }
        .stat-item { 
            text-align: center; 
            padding: 10px; 
            background: #f8f9fa; 
            border-radius: 8px; 
        }
    </style>
</head>
<body>
    <div class="dashboard">
        <div class="header">
            <h1>🚀 API Integration Dashboard</h1>
            <p>Real-time data from multiple sources</p>
        </div>

        <div class="grid">
            <div class="card">
                <h2>🌤️ Weather</h2>
                <div class="weather-icon">☀️</div>
                <pre>$(echo "$weather_info")</pre>
            </div>

            <div class="card">
                <h2>🐙 GitHub Profile</h2>
                <pre>$(echo "$github_info")</pre>
            </div>

            <div class="card">
                <h2>📰 Latest News</h2>
                <div id="newsContent">
                    <!-- News will be loaded here -->
                    <p>Loading news...</p>
                </div>
            </div>

            <div class="card">
                <h2>🔔 Notifications</h2>
                <div id="notificationArea">
                    <p>Send a test notification to Telegram:</p>
                    <button onclick="sendTestNotification()">Send Test Message</button>
                </div>
            </div>
        </div>
    </div>

    <script>
        function sendTestNotification() {
            const button = event.target;
            button.disabled = true;
            button.textContent = 'Sending...';
            
            fetch('notification.php?action=test')
                .then(response => response.text())
                .then(data => {
                    alert('Notification sent!');
                    button.disabled = false;
                    button.textContent = 'Send Test Message';
                })
                .catch(error => {
                    alert('Error sending notification');
                    button.disabled = false;
                    button.textContent = 'Send Test Message';
                });
        }

        // Simulate loading news
        setTimeout(() => {
            document.getElementById('newsContent').innerHTML = \`
                <div class="news-item">
                    <strong>Technology Update</strong>
                    <p>New developments in AI and machine learning are changing the industry.</p>
                </div>
                <div class="news-item">
                    <strong>Open Source Release</strong>
                    <p>New version of popular framework released with performance improvements.</p>
                </div>
            \`;
        }, 2000);
    </script>
</body>
</html>
EOF

    echo "✓ Dashboard created: $dashboard_file"
    echo "Open this file in your browser to view the dashboard"
}

# Функция логирования
log() {
    local message="$1"
    local timestamp=$(date +"%Y-%m-%d %H:%M:%S")
    echo "[$timestamp] $message"
}

# Основная функция
main() {
    local command="${1:-dashboard}"
    
    case "$command" in
        "weather")
            get_weather "$2"
            ;;
        "telegram")
            send_telegram_message "$2"
            ;;
        "news")
            parse_rss_feed "$2"
            ;;
        "github")
            get_github_info "$2"
            ;;
        "repos")
            get_github_repos "$2"
            ;;
        "dashboard")
            create_dashboard
            ;;
        "all")
            echo "=== Integrated API Information ==="
            echo
            get_weather "Moscow"
            echo
            get_github_info "$GITHUB_USER"
            echo
            get_github_repos "$GITHUB_USER"
            echo
            parse_rss_feed
            ;;
        *)
            echo "Usage: $0 [weather|telegram|news|github|repos|dashboard|all]"
            echo
            echo "Examples:"
            echo "  $0 weather London"
            echo "  $0 telegram \"Hello from Bash!\""
            echo "  $0 news"
            echo "  $0 github torvalds"
            echo "  $0 repos"
            echo "  $0 dashboard"
            echo "  $0 all"
            ;;
    esac
}

main "$@"
