#!/bin/bash
# Meri JEDAN komit Feeder-a (faza 4c, lokalizacija preloma). Args: <hash> <author-date>
H="$1"; DATE="$2"
REPO="/c/Users/Nikola/AndroidStudioProjects/4a-extern/Feeder"
CLT="/c/Users/Nikola/AndroidStudioProjects/compose-lint-thesis"
JARW="C:/Users/Nikola/AndroidStudioProjects/compose-lint-thesis/lint-rules/build/libs/lint-rules.jar"
METRIKA="$CLT/metrika/build/install/metrika/bin/metrika"
CSV="$CLT/docs/faza4-proba/feeder-trend.csv"; LOG="$CLT/docs/faza4-proba/eval-log.txt"
export JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
export ANDROID_HOME='C:\Users\Nikola\AppData\Local\Android\Sdk'
JHOME='-Dorg.gradle.java.home=C:\Program Files\Android\Android Studio\jbr'
cd "$REPO" || exit 1
H=$(git rev-parse "$H" 2>/dev/null) || { echo "$1: nepoznat hash" >>"$LOG"; exit 1; }
if grep -q ",$H," "$CSV" 2>/dev/null; then echo "$H ($DATE): vec u CSV" >>"$LOG"; exit 0; fi
git checkout -f -q "$H" 2>>"$LOG"
if [ "$(git rev-parse HEAD 2>/dev/null)" != "$H" ]; then echo "$H ($DATE): CHECKOUT FAIL" >>"$LOG"; exit 1; fi
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
if [ -z "$XML" ] || [ ! -s "$XML" ]; then echo "$H ($DATE): BUILD/LINT FAIL" >>"$LOG"; git checkout -f -q master; exit 1; fi
"$METRIKA" izvestaj --projekat feeder --xml "$(cygpath -w "$XML")" --izvor "$(cygpath -w "$REPO")" --commit "$H" --datum "$DATE" --csv "$(cygpath -w "$CSV")" >>"$LOG" 2>&1 \
  && echo "$H ($DATE): OK" >>"$LOG" || echo "$H ($DATE): METRIKA FAIL" >>"$LOG"
git checkout -f -q master 2>/dev/null
