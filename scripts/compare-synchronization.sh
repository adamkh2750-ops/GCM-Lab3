#!/usr/bin/env bash
# The production sources are never changed. Only a temporary copy loses its locks.
set -eu
if [ "$#" -ne 1 ]; then
    echo 'Usage: bash scripts/compare-synchronization.sh BLOCK_SECONDS'
    exit 1
fi
task_project_dir="$(cd "$(dirname "$0")/.." && pwd)"
task_javac="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
task_java="${JAVA_HOME:+$JAVA_HOME/bin/}java"
task_temp_dir="$(mktemp -d)"
trap 'rm -rf "$task_temp_dir"' EXIT
task_fx_cp=""
for task_module in javafx-base javafx-graphics javafx-controls javafx-fxml; do
    task_jar="$HOME/.m2/repository/org/openjfx/$task_module/21.0.6/$task_module-21.0.6-linux.jar"
    if [ ! -f "$task_jar" ]; then
        echo 'Run the Maven build first. This shell script expects Linux JavaFX dependencies.'
        exit 1
    fi
    task_fx_cp="${task_fx_cp:+$task_fx_cp:}$task_jar"
done
for task_variant in synchronized unsynchronized; do
    mkdir -p "$task_temp_dir/$task_variant/src" "$task_temp_dir/$task_variant/classes"
    cp "$task_project_dir"/src/main/java/com/example/gcm/*.java "$task_temp_dir/$task_variant/src/"
    if [ "$task_variant" = unsynchronized ]; then
        sed 's/synchronized //g' "$task_temp_dir/$task_variant/src/UserState.java" > "$task_temp_dir/UserState.tmp"
        mv "$task_temp_dir/UserState.tmp" "$task_temp_dir/$task_variant/src/UserState.java"
    fi
    "$task_javac" -cp "$task_fx_cp" -d "$task_temp_dir/$task_variant/classes" "$task_temp_dir/$task_variant/src/"*.java
    echo "VARIANT: $task_variant"
    "$task_java" -cp "$task_temp_dir/$task_variant/classes:$task_fx_cp" com.example.gcm.ConcurrencyDemo "$1"
done
