#!/bin/sh
# Read-only helpers for the Maven Central Portal (OSSRH-compatible staging shim
# + Portal API). Credentials are read from a Gradle properties file
# (default F:/gradle.properties, override with VMFTEXT_PUBLISH_PROPS) and are
# never printed.
#
#   scripts/central-staging.sh repos [open|closed|released]  staging repositories: key, state, deployment id
#   scripts/central-staging.sh status <deploymentId>          deployment state + purls (Portal API)
#   scripts/central-staging.sh props                           the -P arguments for Gradle (for $(...))
set -e
PROPS="${VMFTEXT_PUBLISH_PROPS:-/f/gradle.properties}"
[ -f "$PROPS" ] || { echo "credentials file not found: $PROPS (mount the drive or set VMFTEXT_PUBLISH_PROPS)" >&2; exit 2; }
user=$(grep -E '^mavenCentralUsername' "$PROPS" | sed -E 's/^[^=]*=//' | tr -d '\r')
pass=$(grep -E '^mavenCentralPassword' "$PROPS" | sed -E 's/^[^=]*=//' | tr -d '\r')
bearer=$(printf '%s:%s' "$user" "$pass" | base64 | tr -d '\n')
case "$1" in
  repos)
    curl -s -H "Authorization: Bearer $bearer" \
      "https://ossrh-staging-api.central.sonatype.com/manual/search/repositories?state=${2:-open}" \
      | python -c 'import json,sys; d=json.load(sys.stdin); [print(r.get("key"), r.get("state"), r.get("portal_deployment_id")) for r in d.get("repositories",[])] or print("(no repositories)")'
    ;;
  status)
    [ -n "$2" ] || { echo "usage: $0 status <deploymentId>" >&2; exit 2; }
    curl -s -X POST -H "Authorization: Bearer $bearer" \
      "https://central.sonatype.com/api/v1/publisher/status?id=$2" \
      | python -c 'import json,sys; d=json.load(sys.stdin); print("state:", d.get("deploymentState")); print("purls:", d.get("purls")); print("errors:", d.get("errors"))'
    ;;
  props)
    grep -E '^[A-Za-z]' "$PROPS" | tr -d '\r' | sed 's/^/-P/'
    ;;
  *) echo "usage: $0 repos [state] | status <deploymentId> | props" >&2; exit 2 ;;
esac
