#!/usr/bin/env bash
# README.md의 기여 기록 구간(CONTRIBUTIONS 마커 사이)을 git log로 다시 만든다.
#
# 사용법: ./scripts/update-contributions.sh   (레포 안 어디서 실행해도 된다)
#
# - 현재 브랜치(HEAD)의 커밋을 읽는다. merge 커밋은 뺀다.
# - 날짜는 한국 시간(KST, UTC+9) 기준이다. 커밋 시각(author date)의 타임스탬프에
#   9시간을 더해 직접 계산하므로 OS의 TZ 설정에 영향을 받지 않는다.
# - 같은 커밋 상태에서 여러 번 실행해도 README가 바뀌지 않는다.
# - macOS 기본 bash 3.2와 Git Bash(Windows)에서 돌도록 bash, git, awk만 쓴다.

set -eu

# ---- 설정: 필요하면 여기만 고친다 ----------------------------------------
# git author name=README에 표시할 이름 (한 줄에 하나). 목록에 없는 이름은 그대로 표시한다.
AUTHOR_MAP='
hmc1206=홍명철
YJlang=윤준하
'
# 커밋 링크의 기준 주소
REPO_URL='https://github.com/2026Ganzi-Thon/2026_GANZITHON_TEAM_23_BE'
# --------------------------------------------------------------------------

START_MARKER='<!-- CONTRIBUTIONS:START -->'
END_MARKER='<!-- CONTRIBUTIONS:END -->'

ROOT=$(cd "$(dirname "$0")/.." && pwd)
README="$ROOT/README.md"

die() {
    printf 'update-contributions: %s\n' "$1" >&2
    exit 1
}

[ -f "$README" ] || die "README.md가 없습니다: $README"
git -C "$ROOT" rev-parse --git-dir >/dev/null 2>&1 || die "git 저장소가 아닙니다: $ROOT"

# 바이트 단위로 처리해 한글이 OS 로케일에 따라 달리 다뤄지지 않게 한다
LC_ALL=C
export LC_ALL

# ---- 마커 확인: 각각 정확히 한 줄, START가 END보다 위 ----------------------
marker_info=$(START_MARKER="$START_MARKER" END_MARKER="$END_MARKER" awk '
    {
        t = $0
        gsub(/^[ \t]+/, "", t)
        gsub(/[ \t\r]+$/, "", t)
    }
    t == ENVIRON["START_MARKER"] { ns++; ls = NR }
    t == ENVIRON["END_MARKER"]   { ne++; le = NR }
    END { printf "%d %d %d %d\n", ns, ne, ls, le }
' "$README")
# shellcheck disable=SC2086
set -- $marker_info
if [ "$1" -ne 1 ] || [ "$2" -ne 1 ]; then
    die "README.md에 '$START_MARKER'와 '$END_MARKER'가 한 줄씩 있어야 합니다 (현재 START $1개, END $2개)."
fi
if [ "$3" -ge "$4" ]; then
    die "README.md에서 '$START_MARKER'가 '$END_MARKER'보다 위에 있어야 합니다."
fi

LOG_FILE=''
GEN_FILE=''
NEW_FILE=''
cleanup() { rm -f "$LOG_FILE" "$GEN_FILE" "$NEW_FILE"; }
trap cleanup EXIT

TMP_BASE="${TMPDIR:-/tmp}"
LOG_FILE=$(mktemp "$TMP_BASE/contrib-log.XXXXXX")
GEN_FILE=$(mktemp "$TMP_BASE/contrib-gen.XXXXXX")
NEW_FILE=$(mktemp "$TMP_BASE/contrib-new.XXXXXX")

# ---- git log 읽기 ----------------------------------------------------------
# 필드 구분자는 0x1f(단위 구분 문자). 커밋 제목에 거의 나오지 않는 문자다.
if git -C "$ROOT" rev-parse --verify -q HEAD >/dev/null 2>&1; then
    git -C "$ROOT" -c log.showSignature=false -c i18n.logOutputEncoding=UTF-8 \
        log --no-merges --format='%H%x1f%at%x1f%an%x1f%s' HEAD > "$LOG_FILE"
fi

# ---- 마커 사이에 넣을 내용 만들기 -------------------------------------------
AUTHOR_MAP="$AUTHOR_MAP" REPO_URL="$REPO_URL" awk -F '\037' '
# 마크다운을 깨는 문자를 이스케이프한다 (정규식 치환 대신 한 글자씩 붙여 awk 구현 차이를 피한다)
function esc(s,    out, i, c) {
    out = ""
    for (i = 1; i <= length(s); i++) {
        c = substr(s, i, 1)
        if (c == "<") out = out "&lt;"
        else if (c == ">") out = out "&gt;"
        else if (index("\\`*_[]|", c) > 0) out = out "\\" c
        else out = out c
    }
    return out
}

# 유닉스 타임스탬프 -> KST 날짜 (YYYY-MM-DD). 일수 -> 그레고리력 날짜 변환
function kst_date(ts,    days, z, era, doe, yoe, y, doy, mp, d, m) {
    days = int((ts + 32400) / 86400)
    z = days + 719468
    era = int(z / 146097)
    doe = z - era * 146097
    yoe = int((doe - int(doe / 1460) + int(doe / 36524) - int(doe / 146096)) / 365)
    y = yoe + era * 400
    doy = doe - (365 * yoe + int(yoe / 4) - int(yoe / 100))
    mp = int((5 * doy + 2) / 153)
    d = doy - int((153 * mp + 2) / 5) + 1
    m = (mp < 10) ? mp + 3 : mp - 9
    if (m <= 2) y++
    return sprintf("%04d-%02d-%02d", y, m, d)
}

BEGIN {
    nmap = split(ENVIRON["AUTHOR_MAP"], maplines, "\n")
    for (i = 1; i <= nmap; i++) {
        l = maplines[i]
        gsub(/^[ \t\r]+/, "", l)
        gsub(/[ \t\r]+$/, "", l)
        if (l == "" || substr(l, 1, 1) == "#") continue
        p = index(l, "=")
        if (p > 1) alias[substr(l, 1, p - 1)] = substr(l, p + 1)
    }
    url = ENVIRON["REPO_URL"]
}

NF >= 4 {
    c++
    hash[c] = $1
    ts[c] = $2 + 0
    who[c] = ($3 in alias) ? alias[$3] : $3
    subj = $4
    for (k = 5; k <= NF; k++) subj = subj " " $k
    title[c] = subj
}

END {
    if (c == 0) {
        print ""
        print "아직 커밋이 없습니다."
        print ""
        exit
    }

    # 최신 커밋이 위로 (같은 시각이면 git log 순서 유지)
    for (i = 1; i <= c; i++) ord[i] = i
    for (i = 2; i <= c; i++) {
        v = ord[i]; j = i - 1
        while (j >= 1 && ts[ord[j]] < ts[v]) { ord[j + 1] = ord[j]; j-- }
        ord[j + 1] = v
    }

    # 사람별 커밋 수와 최근 커밋 날짜
    np = 0
    for (i = 1; i <= c; i++) {
        n = who[ord[i]]
        if (!(n in cnt)) { np++; person[np] = n; latest[n] = kst_date(ts[ord[i]]) }
        cnt[n]++
    }
    for (i = 2; i <= np; i++) {
        v = person[i]; j = i - 1
        while (j >= 1 && cnt[person[j]] < cnt[v]) { person[j + 1] = person[j]; j-- }
        person[j + 1] = v
    }

    print ""
    print "**사람별 커밋 수** (merge 커밋 제외)"
    print ""
    print "| 이름 | 커밋 | 최근 커밋 |"
    print "|---|---|---|"
    for (i = 1; i <= np; i++) printf "| %s | %d | %s |\n", esc(person[i]), cnt[person[i]], latest[person[i]]

    prev = ""
    for (i = 1; i <= c; i++) {
        x = ord[i]
        day = kst_date(ts[x])
        if (day != prev) {
            print ""
            print "#### " day
            print ""
            prev = day
        }
        printf "- %s · [`%s`](%s/commit/%s) · %s\n", esc(who[x]), substr(hash[x], 1, 7), url, hash[x], esc(title[x])
    }
    print ""
}
' "$LOG_FILE" > "$GEN_FILE"

commit_count=$(awk 'END { print NR }' "$LOG_FILE")

# ---- README에 끼워 넣기 (마커 줄은 그대로 두고 사이만 바꾼다) ---------------
START_MARKER="$START_MARKER" END_MARKER="$END_MARKER" GEN_FILE="$GEN_FILE" awk '
    {
        t = $0
        gsub(/^[ \t]+/, "", t)
        gsub(/[ \t\r]+$/, "", t)
    }
    t == ENVIRON["START_MARKER"] {
        print
        cr = ($0 ~ /\r$/) ? "\r" : ""   # CRLF로 체크아웃된 README면 줄 끝을 맞춘다
        gen = ENVIRON["GEN_FILE"]
        while ((getline g < gen) > 0) print g cr
        close(gen)
        skip = 1
        next
    }
    t == ENVIRON["END_MARKER"] { skip = 0; print; next }
    !skip { print }
' "$README" > "$NEW_FILE"

if cmp -s "$README" "$NEW_FILE"; then
    printf '기여 기록: 변경 없음 (커밋 %s개)\n' "$commit_count"
else
    cat "$NEW_FILE" > "$README"
    printf '기여 기록: README.md 갱신 (커밋 %s개)\n' "$commit_count"
fi
