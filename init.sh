#!/usr/bin/env bash
#
# init.sh — Quick initializer to configure new project from template.
#
# Usage:
#   ./init.sh
#   ./init.sh --app-name "MyCoolApp" --app-id "com.example.mycoolapp" [-y]
#
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MOBILE_DIR="$REPO_ROOT/MobileApp"

if [ ! -f "$MOBILE_DIR/scripts/refactor_package.sh" ]; then
  echo "Error: Could not locate $MOBILE_DIR/scripts/refactor_package.sh" >&2
  exit 1
fi

# Interactive prompt if no arguments are passed in a terminal
if [ -t 0 ] && [ $# -eq 0 ]; then
  echo "=========================================================="
  echo "  KMP Starter Kit — New Project Setup"
  echo "=========================================================="
  read -r -p "Enter App Display Name (e.g. FoodDelivery): " APP_NAME
  read -r -p "Enter Application / Bundle ID (e.g. com.company.food): " APP_ID

  if [ -z "$APP_NAME" ] || [ -z "$APP_ID" ]; then
    echo "Error: App Name and App ID cannot be empty." >&2
    exit 1
  fi

  exec "$MOBILE_DIR/scripts/refactor_package.sh" --app-name "$APP_NAME" --app-id "$APP_ID"
fi

exec "$MOBILE_DIR/scripts/refactor_package.sh" "$@"
