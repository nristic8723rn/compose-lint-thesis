#!/bin/bash
# Faza 4b evaluacija: Feeder kroz istoriju (prvi komit svakog meseca).
REPO="/c/Users/Nikola/AndroidStudioProjects/4a-extern/Feeder"
CLT="/c/Users/Nikola/AndroidStudioProjects/compose-lint-thesis"
JARW="C:/Users/Nikola/AndroidStudioProjects/compose-lint-thesis/lint-rules/build/libs/lint-rules.jar"
METRIKA="$CLT/metrika/build/install/metrika/bin/metrika"
OUT="$CLT/docs/faza4-proba"; mkdir -p "$OUT"
CSV="$OUT/feeder-trend.csv"; LOG="$OUT/eval-log.txt"
START_Y=${START_Y:-2024}; START_M=${START_M:-8}; END_Y=${END_Y:-2026}; END_M=${END_M:-8}
export JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
export ANDROID_HOME='C:\Users\Nikola\AppData\Local\Android\Sdk'
JHOME='-Dorg.gradle.java.home=C:\Program Files\Android\Android Studio\jbr'
CSVW=$(cygpath -w "$CSV"); REPOW=$(cygpath -w "$REPO")
cd "$REPO" || exit 1
git checkout -f -q master 2>/dev/null
# 1) prekompute komiti (na cistom master-u, pre ikakvog checkout-a)
python - "$START_Y" "$START_M" "$END_Y" "$END_M" <<'PY' > /tmp/feeder_months.txt
import sys,datetime
sy,sm,ey,em=map(int,sys.argv[1:5])
d=datetime.date(sy,sm,1); end=datetime.date(ey,em,1)
while d<=end:
    nd=(d.replace(day=28)+datetime.timedelta(days=7)).replace(day=1)
    print(d.isoformat(), nd.isoformat()); d=nd
PY
: > /tmp/feeder_commits.txt
while read since until; do
  H=$(git log --reverse --since="$since" --until="$until" --format=%H master 2>/dev/null | head -1)
  [ -n "$H" ] && echo "$since $H $(git show -s --format=%ad --date=short "$H")" >> /tmp/feeder_commits.txt
done < /tmp/feeder_months.txt
echo "START $(date) | tacaka za obradu: $(wc -l < /tmp/feeder_commits.txt)" >> "$LOG"
# 2) petlja: checkout -f -> wire -> lint --rerun-tasks -> metrika
ok=0; fail=0
while read ym H DATE; do
  if [ -f "$CSV" ] && grep -q ",$H," "$CSV"; then echo "$ym $H ($DATE): vec u CSV, preskacem (resume)" >>"$LOG"; ok=$((ok+1)); continue; fi
  git checkout -f -q "$H" 2>>"$LOG"
  if [ "$(git rev-parse HEAD 2>/dev/null)" != "$H" ]; then echo "$ym $H ($DATE): CHECKOUT FAIL (HEAD != cilj)" >>"$LOG"; fail=$((fail+1)); continue; fi
  DEP=$(grep -n "^dependencies {" app/build.gradle.kts | head -1 | cut -d: -f1)
  [ -n "$DEP" ] && sed -i "${DEP}a\    lintChecks(files(\"$JARW\"))" app/build.gradle.kts
  sed -i 's/ignoreWarnings = true/ignoreWarnings = false/' app/build.gradle.kts 2>/dev/null
  rm -f app/build/reports/lint-results-*.xml
  if grep -q '"fdroid"' app/build.gradle.kts; then TASK=lintFdroidDebug; else TASK=lintDebug; fi
  ./gradlew.bat -q "$JHOME" --no-daemon --rerun-tasks ":app:$TASK" >/dev/null 2>>"$LOG"
  XML=$(ls app/build/reports/lint-results-*.xml 2>/dev/null | head -1)
  if [ -z "$XML" ] || [ ! -s "$XML" ]; then
    ALT=lintDebug; [ "$TASK" = "lintDebug" ] && ALT=lintFdroidDebug
    ./gradlew.bat -q "$JHOME" --no-daemon --rerun-tasks ":app:$ALT" >/dev/null 2>>"$LOG"
    XML=$(ls app/build/reports/lint-results-*.xml 2>/dev/null | head -1)
  fi
  if [ -z "$XML" ] || [ ! -s "$XML" ]; then echo "$ym $H ($DATE): BUILD/LINT FAIL" >>"$LOG"; fail=$((fail+1)); continue; fi
  if "$METRIKA" izvestaj --projekat feeder --xml "$(cygpath -w "$XML")" --izvor "$REPOW" --commit "$H" --datum "$DATE" --csv "$CSVW" >>"$LOG" 2>&1; then
    echo "$ym $H ($DATE): OK" >>"$LOG"; ok=$((ok+1))
  else echo "$ym $H ($DATE): METRIKA FAIL" >>"$LOG"; fail=$((fail+1)); fi
done < /tmp/feeder_commits.txt
echo "GOTOVO ok=$ok fail=$fail $(date)" >>"$LOG"
git checkout -f -q master 2>/dev/null
