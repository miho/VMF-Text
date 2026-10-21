#!/bin/sh
# Downloads the sample programs from grammars-v4/python/python3/{small,large}
# into samples/. They carry their own licenses, so they are fetched on demand
# instead of being committed to this repository.
set -e
mkdir -p samples
for d in small large; do
  curl -s "https://api.github.com/repos/antlr/grammars-v4/contents/python/python3/$d" \
    | python -c 'import json,sys; d=json.load(sys.stdin); [print(f["download_url"]) for f in d if f["type"]=="file" and (f["name"].endswith(".py") or f["name"].endswith(".py.errors"))]' \
    | tr -d '\r' | while read -r u; do (cd samples && curl -s -O "$u"); done
done
echo "samples: $(ls samples/*.py | wc -l) files"
