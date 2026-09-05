#!/bin/bash
REPO="/c/Users/Nikola/AndroidStudioProjects/4a-extern/Feeder"
CLT="/c/Users/Nikola/AndroidStudioProjects/compose-lint-thesis"
JARW="C:/Users/Nikola/AndroidStudioProjects/compose-lint-thesis/lint-rules/build/libs/lint-rules.jar"
METRIKA="$CLT/metrika/build/install/metrika/bin/metrika"
OUT="$CLT/docs/faza4-proba"; mkdir -p "$OUT"
CSV="$OUT/feeder-trend.csv"; LOG="$OUT/eval-log.txt"
rm -f "$CSV" "$LOG"
export JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
export ANDROID_HOME='C:\Users\Nikola\AppData\Local\Android\Sdk'
JHOME='-Dorg.gradle.java.home=C:\Program Files\Android\Android Studio\jbr'
CSVW=$(cygpath -w "$CSV"); REPOW=$(cygpath -w "$REPO")
cd "$REPO" || exit 1
git checkout -q -- . 2>/dev/null; git checkout -q master 2>/dev/null
months=$(python -c "
import datetime
d=datetime.date(2024,8,1); end=datetime.date(2026,8,1)
while d<=end:
    print(d.strftime('%Y-%m')); d=(d.replace(day=28)+datetime.timedelta(days=7)).replace(day=1)")
ok=0; fail=0
echo "START $(date)" >>"$LOG"
for ym in $months; do
  y=${ym%-*}; m=${ym#*-}
  nm=$(python -c "import datetime;d=datetime.date($y,$m,1);n=(d.replace(day=28)+datetime.timedelta(days=7)).replace(day=1);print(n.strftime('%Y-%m-01'))")
  H=$(git log --reverse --since="$ym-01T00:00:00" --until="${nm}T00:00:00" --format=%H master 2>/dev/null | head -1)
  if [ -z "$H" ]; then echo "$ym: nema komita, preskacem" >>"$LOG"; continue; fi
  DATE=$(git show -s --format=%ad --date=short "$H")
  git checkout -q -- . 2>/dev/null
  if ! git checkout -q "$H" 2>>"$LOG"; then echo "$ym $H: CHECKOUT FAIL" >>"$LOG"; fail=$((fail+1)); continue; fi
  DEP=$(grep -n "^dependencies {" app/build.gradle.kts | head -1 | cut -d: -f1)
  [ -n "$DEP" ] && sed -i "${DEP}a\    lintChecks(files(\"$JARW\"))" app/build.gradle.kts
  sed -i 's/ignoreWarnings = true/ignoreWarnings = false/' app/build.gradle.kts 2>/dev/null
  rm -f app/build/reports/lint-results-*Debug.xml
  if grep -q 'fdroid' app/build.gradle.kts; then TASK=lintFdroidDebug; else TASK=lintDebug; fi
  ./gradlew.bat -q "$JHOME" --rerun-tasks ":app:$TASK" >/dev/null 2>>"$LOG"
  XML=$(ls app/build/reports/lint-results-*Debug.xml 2>/dev/null | head -1)
  if [ -z "$XML" ] || [ ! -s "$XML" ]; then
     ALT=lintDebug; [ "$TASK" = "lintDebug" ] && ALT=lintFdroidDebug
     ./gradlew.bat -q "$JHOME" --rerun-tasks ":app:$ALT" >/dev/null 2>>"$LOG"
     XML=$(ls app/build/reports/lint-results-*Debug.xml 2>/dev/null | head -1)
  fi
  if [ -z "$XML" ] || [ ! -s "$XML" ]; then echo "$ym $H ($DATE): BUILD/LINT FAIL" >>"$LOG"; fail=$((fail+1)); git checkout -q -- .; continue; fi
  "$METRIKA" izvestaj --projekat feeder --xml "$(cygpath -w "$XML")" --izvor "$REPOW" --commit "$H" --datum "$DATE" --csv "$CSVW" >>"$LOG" 2>&1 \
    && { echo "$ym $H ($DATE): OK"; ok=$((ok+1)); } >>"$LOG" || { echo "$ym $H ($DATE): METRIKA FAIL"; fail=$((fail+1)); } >>"$LOG"
  git checkout -q -- .
done
echo "GOTOVO ok=$ok fail=$fail $(date)" >>"$LOG"
git checkout -q master 2>/dev/null
