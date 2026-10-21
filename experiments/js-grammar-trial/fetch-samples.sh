#!/bin/sh
# Downloads the sample programs from grammars-v4/javascript/ecmascript/examples
# into samples/. They carry mixed licenses, so they are fetched on demand
# instead of being committed to this repository.
set -e
mkdir -p samples
curl -s "https://api.github.com/repos/antlr/grammars-v4/contents/javascript/ecmascript/examples" \
  | python -c 'import json,sys; d=json.load(sys.stdin); [print(f["download_url"]) for f in d if f["type"]=="file" and (f["name"].endswith(".js") or f["name"].endswith(".js.errors"))]' \
  | tr -d '\r' | while read -r u; do (cd samples && curl -s -O "$u"); done
echo "samples: $(ls samples/*.js | wc -l) files"
