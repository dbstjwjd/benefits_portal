#!/bin/bash
#
# IDIS DB 백업. 하루 한 번 돌리고 7일치만 남긴다.
#
# 설치:
#   sudo cp deploy/backup.sh /opt/idis/backup.sh
#   sudo chown idis:idis /opt/idis/backup.sh
#   sudo chmod 700 /opt/idis/backup.sh
#   sudo mkdir -p /opt/idis/backup && sudo chown idis:idis /opt/idis/backup
#
# 비밀번호를 명령줄에 두면 ps 에 노출되므로 ~/.my.cnf 를 쓴다.
#   sudo -u idis nano /home/idis/.my.cnf
#     [client]
#     user=idis
#     password=여기에_DB_비밀번호
#   sudo -u idis chmod 600 /home/idis/.my.cnf
#
# crontab (idis 사용자):
#   sudo -u idis crontab -e
#     # 매일 새벽 3시 30분 DB 백업
#     30 3 * * * /opt/idis/backup.sh >> /var/log/idis/backup.log 2>&1

set -euo pipefail

DB_NAME="idis"
BACKUP_DIR="/opt/idis/backup"
UPLOAD_DIR="/opt/idis/uploads"
KEEP_DAYS=7

STAMP="$(date +%Y%m%d-%H%M%S)"
DUMP="${BACKUP_DIR}/idis-${STAMP}.sql.gz"

mkdir -p "$BACKUP_DIR"

echo "[$(date '+%F %T')] 백업 시작 → ${DUMP}"

# --single-transaction: InnoDB 를 잠그지 않고 일관된 스냅숏을 뜬다
mysqldump --single-transaction --quick --routines --default-character-set=utf8mb4 \
    "$DB_NAME" | gzip > "$DUMP"

# 선택지 이미지는 DB 에 없으므로 같이 묶어 둔다 (있을 때만)
if [ -d "$UPLOAD_DIR" ] && [ -n "$(ls -A "$UPLOAD_DIR" 2>/dev/null)" ]; then
    tar -czf "${BACKUP_DIR}/uploads-${STAMP}.tar.gz" -C "$(dirname "$UPLOAD_DIR")" "$(basename "$UPLOAD_DIR")"
fi

# 빈 파일이 남으면 백업이 된 줄 알고 넘어가므로 확인하고 지운다
if [ ! -s "$DUMP" ]; then
    echo "[$(date '+%F %T')] 실패: 덤프가 비어 있습니다" >&2
    rm -f "$DUMP"
    exit 1
fi

find "$BACKUP_DIR" -name 'idis-*.sql.gz'    -mtime +${KEEP_DAYS} -delete
find "$BACKUP_DIR" -name 'uploads-*.tar.gz' -mtime +${KEEP_DAYS} -delete

echo "[$(date '+%F %T')] 완료 ($(du -h "$DUMP" | cut -f1)), ${KEEP_DAYS}일 지난 파일 정리함"
