#!/bin/sh
# Publishes the version in config/common.properties to Maven Central as ONE
# deployment (core + gradle-plugin + plugin marker), or to the Central
# snapshot repository for a -SNAPSHOT version. Run from the repository root
# after `sh ./build-and-test-all.sh` is green on the release commit.
#
#   sh scripts/publish-central.sh            # manual review: press Publish on the Portal
#   sh scripts/publish-central.sh automatic  # hand off with publishing_type=automatic
#
# Credentials: VMFTEXT_PUBLISH_PROPS (default F:/gradle.properties), see
# docs/RELEASING.md. The Gradle Plugin Portal step (publishPlugins) is separate.
set -e
cd "$(dirname "$0")/.."
VERSION=$(grep -E '^publication.version' config/common.properties | sed -E 's/^[^=]*=\s*//' | tr -d '\r ')
PROPS=$(sh scripts/central-staging.sh props)
echo "==> publishing VMF-Text $VERSION"

echo "==> core: upload"
(cd core && sh ./gradlew publishMavenJavaPublicationToMavenCentralRepository --no-daemon $PROPS)
echo "==> gradle-plugin: upload plugin + marker"
(cd gradle-plugin && sh ./gradlew publishAllPublicationsToMavenCentralRepository --no-daemon $PROPS)

case "$VERSION" in
  *-SNAPSHOT)
    echo "==> SNAPSHOT: no staging hand-off needed; available at https://central.sonatype.com/repository/maven-snapshots/"
    exit 0 ;;
esac

echo "==> staging repositories (open)"
sh scripts/central-staging.sh repos open
KEY=$(sh scripts/central-staging.sh repos open | awk 'NR==1 && $1 != "(no" {print $1}')
[ -n "$KEY" ] || { echo "no open staging repository found" >&2; exit 1; }
TYPE="${1:-user_managed}"
echo "==> hand-off of $KEY (publishing_type=$TYPE)"
(cd core && sh ./gradlew releaseToCentralPortal -PsonatypeNamespace=eu.mihosoft -PrepositoryKey="$KEY" -PpublishingType="$TYPE" --no-daemon $(sh scripts/central-staging.sh props | grep mavenCentral))
echo "==> released repositories"
sh scripts/central-staging.sh repos released
DEP=$(sh scripts/central-staging.sh repos released | awk '$3 != "None" {print $3}' | head -1)
[ -n "$DEP" ] && { echo "==> deployment $DEP"; sh scripts/central-staging.sh status "$DEP"; }
echo "==> done. Check https://central.sonatype.com/publishing/deployments and the Usage Center."
