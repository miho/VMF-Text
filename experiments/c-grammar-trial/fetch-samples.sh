#!/bin/sh
# Downloads the 32 sample programs from grammars-v4/c/examples into samples/.
# They carry mixed licenses (several come from the GCC test suite), so they are
# fetched on demand instead of being committed to this repository.
set -e
mkdir -p samples
curl -s "https://api.github.com/repos/antlr/grammars-v4/contents/c/examples" \
  | python -c 'import json,sys; d=json.load(sys.stdin); [print(f["download_url"]) for f in d if f["type"]=="file" and (f["name"].endswith(".c") or f["name"].endswith(".c.errors"))]' \
  | tr -d '\r' | while read -r u; do (cd samples && curl -s -O "$u"); done
echo "samples: $(ls samples/*.c | wc -l) files"
