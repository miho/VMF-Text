#!/bin/sh
# Downloads the example programs of grammars-v4/javascript/javascript (at the
# commit the grammar files were taken from) into samples/. They carry mixed
# licenses, so they are fetched on demand instead of being committed.
set -e
REF=7aa2e6b3ea4cc8de503b95a10c9a06e9265d291e
mkdir -p samples
curl -s "https://api.github.com/repos/antlr/grammars-v4/contents/javascript/javascript/examples?ref=$REF" \
  | python -c 'import json,sys; d=json.load(sys.stdin); [print(f["download_url"]) for f in d if f["type"]=="file" and (f["name"].endswith(".js") or f["name"].endswith(".js.errors"))]' \
  | tr -d '\r' | while read -r u; do (cd samples && curl -s -O "$u"); done
echo "samples: $(ls samples/*.js | wc -l) files"
