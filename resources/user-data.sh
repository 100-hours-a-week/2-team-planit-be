#!/bin/bash
set -euxo pipefail

# 0) 로그파일 생성
sudo mkdir -p /var/log/planit/was /var/log/planit/ai
sudo chmod 777 /var/log/planit/was /var/log/planit/ai

# 1) cw 설정파일
sudo tee /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json > /dev/null <<'EOF'
{
  "agent": {
    "metrics_collection_interval": 60,
    "region": "ap-northeast-2",
    "run_as_user": "root"
  },
  "logs": {
    "force_flush_interval": 5,
    "logs_collected": {
      "files": {
        "collect_list": [
          {
            "file_path": "/var/log/planit/was/*.log",
            "log_group_name": "/planit/v2/was",
            "log_stream_name": "was-{hostname}-{instance_id}",
            "timezone": "Local",
            "multi_line_start_pattern": "^[0-9]{4}-[0-9]{2}-[0-9]{2}T"
          },
          {
            "file_path": "/var/log/planit/ai/*.log",
            "log_group_name": "/planit/v2/ai",
            "log_stream_name": "ai-{hostname}-{instance_id}",
            "timezone": "Local",
            "multi_line_start_pattern": "^(INFO|ERROR|WARNING|DEBUG):\\s{2,}|^[0-9]{4}-[0-9]{2}-[0-9]{2}T"
          }
        ]
      }
    }
  },
  "metrics": {
    "append_dimensions": {
      "AutoScalingGroupName": "${aws:AutoScalingGroupName}",
      "InstanceId": "${aws:InstanceId}"
    },
    "aggregation_dimensions": [
      ["AutoScalingGroupName"]
    ],
    "metrics_collected": {
      "cpu": {"measurement": ["cpu_usage_idle","cpu_usage_user","cpu_usage_system","cpu_usage_iowait"], "totalcpu": true, "metrics_collection_interval": 60},
      "mem": {"measurement": ["mem_used_percent","mem_available","mem_total"], "metrics_collection_interval": 60},
      "swap": {"measurement": ["swap_used_percent"], "metrics_collection_interval": 60},
      "disk": {"measurement": ["used_percent","inodes_free"], "resources": ["*"], "metrics_collection_interval": 60},
      "net": {"measurement": ["bytes_sent","bytes_recv"], "metrics_collection_interval": 60}
    }
  }
}
EOF

sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
  -a fetch-config -m ec2 \
  -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json \
  -s

# 필수 패키지
if command -v apt-get >/dev/null 2>&1; then
  sudo apt-get update -y
  sudo apt-get install -y jq curl

  # AWS CLI가 없으면 snap으로 설치
  if ! command -v aws >/dev/null 2>&1; then
    echo "AWS CLI not found, installing via snap..."
    
    # snap이 없으면 설치
    if ! command -v snap >/dev/null 2>&1; then
      sudo apt-get install -y snapd
    fi

    sudo snap install aws-cli --classic
  fi

elif command -v yum >/dev/null 2>&1; then
  sudo yum install -y jq curl

  # Amazon Linux 등은 awscli 패키지 존재
  if ! command -v aws >/dev/null 2>&1; then
    sudo yum install -y awscli
  fi
fi

# 최종 확인 (필수 의존성이므로 없으면 종료)
if ! command -v aws >/dev/null 2>&1; then
  echo "AWS CLI installation failed. Exiting."
  exit 1
fi

# Docker 선기동 보장
if command -v systemctl >/dev/null 2>&1; then
  systemctl enable docker
  systemctl start docker || true
  for i in {1..60}; do
    if docker info >/dev/null 2>&1; then break; fi
    if [[ $i -eq 60 ]]; then echo "Docker not ready after 60s"; exit 1; fi
    sleep 1
  done
fi

REGION="ap-northeast-2"
ACCOUNT_ID="713881824287"
REPO_NAME="planit-was"
IMAGE_TAG="{{IMAGE_TAG}}"   # CD에서 sha-xxxxxxx 주입

APP_DIR="/opt/planit"
LOG_DIR="/var/log/planit/was"
PROXYSQL_CFG="${APP_DIR}/proxysql.cnf"
PROXYSQL_INIT_SQL="${APP_DIR}/proxysql-init.sql"

SSM_PREFIX="/planit/prod/was"   # Backend/WAS 전용 prefix (추천)

mkdir -p "${APP_DIR}" "${APP_DIR}/config" "${LOG_DIR}"
cd "${APP_DIR}"

get_ssm() {
  local name="$1"
  aws ssm get-parameter \
    --region "${REGION}" \
    --name "${name}" \
    --with-decryption \
    --query 'Parameter.Value' \
    --output text
}

# ===== SSM에서 환경값 로드 (민감정보 포함) =====
SPRING_DATASOURCE_USERNAME="$(get_ssm "${SSM_PREFIX}/SPRING_DATASOURCE_USERNAME")"
SPRING_DATASOURCE_PASSWORD="$(get_ssm "${SSM_PREFIX}/SPRING_DATASOURCE_PASSWORD")"
GOOGLE_MAPS_API_KEY="$(get_ssm "${SSM_PREFIX}/GOOGLE_MAPS_API_KEY")"
SPRING_DATA_REDIS_HOST="$(get_ssm "${SSM_PREFIX}/SPRING_DATA_REDIS_HOST")"
SPRING_DATA_REDIS_PORT="$(get_ssm "${SSM_PREFIX}/SPRING_DATA_REDIS_PORT")"
SPRING_DATA_REDIS_PASSWORD="$(get_ssm "${SSM_PREFIX}/SPRING_DATA_REDIS_PASSWORD")"
SPRING_DATA_MONGODB_URI="$(get_ssm "${SSM_PREFIX}/SPRING_DATA_MONGODB_URI")"
AI_BASE_URL="$(get_ssm "${SSM_PREFIX}/AI_BASE_URL")"
JWT_SECRET="$(get_ssm "${SSM_PREFIX}/JWT_SECRET")"

AWS_S3_BUCKET="$(get_ssm "${SSM_PREFIX}/AWS_S3_BUCKET")"
CLOUDFRONT_DOMAIN="$(get_ssm "${SSM_PREFIX}/CLOUDFRONT_DOMAIN")"

# MySQL 백엔드
MYSQL_V1_HOST="$(get_ssm "${SSM_PREFIX}/MYSQL_V1_HOST")"
MYSQL_V1_PORT="$(get_ssm "${SSM_PREFIX}/MYSQL_V1_PORT")"
MYSQL_V2_HOST="$(get_ssm "${SSM_PREFIX}/MYSQL_V2_HOST")"
MYSQL_V2_PORT="$(get_ssm "${SSM_PREFIX}/MYSQL_V2_PORT")"

# 앱은 ProxySQL 6033으로 접속
SPRING_DATASOURCE_URL="jdbc:mysql://proxysql:6033/planit_db?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=utf8"

APP_IMAGE="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/${REPO_NAME}:${IMAGE_TAG}"

# ProxySQL용 계정
DB_USER="${SPRING_DATASOURCE_USERNAME}"
DB_PASS="${SPRING_DATASOURCE_PASSWORD}"
DB_PASS_ESC="${DB_PASS//\'/\'\'}"

cat <<EOF > "${PROXYSQL_CFG}"
datadir="/var/lib/proxysql"
admin_variables=
{
  admin_credentials="admin:admin"
  mysql_ifaces="0.0.0.0:6032"
}
mysql_variables=
{
  threads=4
  max_connections=2048
  server_version="8.0.31"
  monitor_username="${DB_USER}"
  monitor_password="${DB_PASS}"
  connect_retries_on_failure=10
}
EOF

cat <<EOF > "${PROXYSQL_INIT_SQL}"
REPLACE INTO main.mysql_servers (hostname, port, hostgroup_id, max_connections, comment) VALUES
('${MYSQL_V1_HOST}', ${MYSQL_V1_PORT}, 0, 200, 'v1-writer'),
('${MYSQL_V2_HOST}', ${MYSQL_V2_PORT}, 1, 200, 'v2-reader');
LOAD MYSQL SERVERS TO RUNTIME; SAVE MYSQL SERVERS TO DISK;

REPLACE INTO main.mysql_users (username, password, default_hostgroup, default_schema, max_connections, active, transaction_persistent) VALUES
('${DB_USER}', '${DB_PASS_ESC}', 0, 'planit_db', 100, 1, 1);
LOAD MYSQL USERS TO RUNTIME; SAVE MYSQL USERS TO DISK;

REPLACE INTO main.mysql_query_rules (rule_id, active, match_pattern, destination_hostgroup, apply, comment) VALUES
(1, 1, '^SELECT ', 1, 1, 'read-to-v2'),
(2, 1, '^select ', 1, 1, 'read-to-v2-lower');
LOAD MYSQL QUERY RULES TO RUNTIME; SAVE MYSQL QUERY RULES TO DISK;
EOF

# ECR 로그인
aws ecr get-login-password --region "${REGION}" \
  | docker login --username AWS --password-stdin "${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"

cat <<EOF > docker-compose.yml
services:
  proxysql:
    image: proxysql/proxysql:2.5.5
    container_name: planit-proxysql
    ports:
      - "6033:6033"
      - "6032:6032"
    volumes:
      - /opt/planit/proxysql.cnf:/etc/proxysql.cnf
      - proxysql_data:/var/lib/proxysql
    command: ["proxysql", "-f", "-c", "/etc/proxysql.cnf"]
    restart: unless-stopped

  app:
    image: ${APP_IMAGE}
    container_name: planit-app
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: "prod,ai"
      TZ: Asia/Seoul
      SPRING_DATASOURCE_URL: "${SPRING_DATASOURCE_URL}"
      SPRING_DATASOURCE_USERNAME: "${SPRING_DATASOURCE_USERNAME}"
      SPRING_DATASOURCE_PASSWORD: "${SPRING_DATASOURCE_PASSWORD}"
      GOOGLE_MAPS_API_KEY: "${GOOGLE_MAPS_API_KEY}"
      SPRING_DATA_REDIS_HOST: "${SPRING_DATA_REDIS_HOST}"
      SPRING_DATA_REDIS_PORT: "${SPRING_DATA_REDIS_PORT}"
      SPRING_DATA_REDIS_PASSWORD: "${SPRING_DATA_REDIS_PASSWORD}"
      JWT_SECRET: "${JWT_SECRET}"
      AWS_S3_BUCKET: "${AWS_S3_BUCKET}"
      AWS_REGION: "${REGION}"
      CLOUDFRONT_DOMAIN: "${CLOUDFRONT_DOMAIN}"
      SPRING_DATA_MONGODB_URI: "${SPRING_DATA_MONGODB_URI}"
      AI_BASE_URL: "${SPRING_DATA_MONGODB_URI}"
    volumes:
      - /var/log/planit/was:/var/log/planit/was
      - ${APP_DIR}/config:/app/config
    depends_on:
      - proxysql
    restart: unless-stopped

volumes:
  proxysql_data:
EOF

MYSQL_CLIENT_IMAGE="public.ecr.aws/docker/library/mysql:8.0"

docker compose pull
docker pull "${MYSQL_CLIENT_IMAGE}"

docker compose up -d proxysql
echo "ProxySQL 기동 대기 후 초기 설정 로드..."
sleep 45

for i in {1..15}; do
  if docker run --rm --network container:planit-proxysql \
    -v "${PROXYSQL_INIT_SQL}:/init.sql:ro" \
    "${MYSQL_CLIENT_IMAGE}" \
    sh -c "mysql -h127.0.0.1 -P6032 -uadmin -padmin < /init.sql"; then
    echo "ProxySQL init OK"
    break
  fi
  echo "ProxySQL init attempt $i/15 failed, retrying in 3s..."
  sleep 3
  [[ $i -eq 15 ]] && { echo "ProxySQL init failed after 15 attempts"; exit 1; }
done

docker compose up -d app