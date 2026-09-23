#!/usr/bin/env bash
set -euo pipefail

printf '\n1. GET đầu tiên: cache miss\n'
curl -sS http://localhost:8080/api/products/P001
printf '\n\n2. GET thứ hai: cache hit\n'
curl -sS http://localhost:8080/api/products/P001
printf '\n\n3. UPDATE DB rồi evict cache\n'
curl -sS -X PUT http://localhost:8080/api/products/P001 \
  -H 'Content-Type: application/json' \
  -d '{"name":"iPhone 15 Pro","price":24990000,"description":"Phiên bản mới"}'
printf '\n\n4. GET lại: cache miss và nạp dữ liệu mới\n'
curl -sS http://localhost:8080/api/products/P001
printf '\n'
