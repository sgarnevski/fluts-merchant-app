#!/usr/bin/env bash
# Builds the core of this platform - flut-core, flut-common, flut-rest from
# https://github.com/sgarnevski/fluts-merchant - and installs it into the local Maven
# repository (~/.m2), where this project's build finds it as version 1.0.0-SNAPSHOT.
#
#   ./install-core.sh                      clone (or update) the core into .core/ and install it
#   FLUTS_MERCHANT_DIR=../fluts-merchant ./install-core.sh
#                                          install from an existing checkout instead (e.g. with local changes)
#   FLUTS_MERCHANT_REF=<branch|tag|sha> ./install-core.sh
#                                          use another revision of the core (default: main)
set -euo pipefail

REPO_URL="${FLUTS_MERCHANT_URL:-https://github.com/sgarnevski/fluts-merchant.git}"
REF="${FLUTS_MERCHANT_REF:-main}"
DIR="${FLUTS_MERCHANT_DIR:-}"

if [[ -z "$DIR" ]]; then
    DIR="$(cd "$(dirname "$0")" && pwd)/.core/fluts-merchant"
    if [[ -d "$DIR/.git" ]]; then
        git -C "$DIR" fetch --quiet origin "$REF"
        git -C "$DIR" checkout --quiet --detach FETCH_HEAD
    else
        mkdir -p "$(dirname "$DIR")"
        git clone --quiet "$REPO_URL" "$DIR"
        git -C "$DIR" checkout --quiet "$REF"
    fi
fi

echo "Installing the core from $DIR ($(git -C "$DIR" rev-parse --short HEAD 2>/dev/null || echo 'not a git checkout'))"
"$DIR/mvnw" -f "$DIR/pom.xml" -q -B install -DskipTests -Djacoco.skip=true -Dpmd.skip=true
echo "Done: com.fluts:flut-core / flut-common / flut-rest 1.0.0-SNAPSHOT are in the local Maven repository."
