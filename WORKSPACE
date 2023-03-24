
# Note: use the following to see the location for the worksapce bazel_tools:
# bazel query --output=build //external:bazel_tools
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

RULES_JVM_EXTERNAL_TAG = "5.1"
RULES_JVM_EXTERNAL_SHA = "8c3b207722e5f97f1c83311582a6c11df99226e65e2471086e296561e57cc954"

http_archive(
    name = "rules_jvm_external",
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    sha256 = RULES_JVM_EXTERNAL_SHA,
    url = "https://github.com/bazelbuild/rules_jvm_external/releases/download/5.1/rules_jvm_external-%s.tar.gz" % RULES_JVM_EXTERNAL_TAG,
)

load("@rules_jvm_external//:repositories.bzl", "rules_jvm_external_deps")

rules_jvm_external_deps()

load("@rules_jvm_external//:setup.bzl", "rules_jvm_external_setup")

rules_jvm_external_setup()

load("@rules_jvm_external//:defs.bzl", "maven_install")


JUNIT_JUPITER_VERSION = "5.9.2"

JUNIT_PLATFORM_VERSION = "1.9.2"

maven_install(
    artifacts = [
        # external dependencies for junit framework
        "org.junit.platform:junit-platform-launcher:%s" % JUNIT_PLATFORM_VERSION,
        "org.junit.platform:junit-platform-reporting:%s" % JUNIT_PLATFORM_VERSION,
        "org.junit.jupiter:junit-jupiter-api:%s" % JUNIT_JUPITER_VERSION,
        "org.junit.jupiter:junit-jupiter-params:%s" % JUNIT_JUPITER_VERSION,
        "org.junit.jupiter:junit-jupiter-engine:%s" % JUNIT_JUPITER_VERSION,
        # jackson.core is external dependency for json generation/parsing
        "com.fasterxml.jackson.core:jackson-core:2.14.2",
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
    ],
)

#################################################################################
#################################################################################
#################################################################################
# Here is setup for bazel-contrib/rules_jvm bazel rules
# They are useful for junit and more
#
# See https://github.com/bazel-contrib/rules_jvm/releases for version and sha 

CONTRIB_RULES_JVM_VERSION = "0.12.0"

CONTRIB_RULES_JVM_SHA = "09c022847c96f24d085e2c82a6174f0ab98218e6e0903d0793d69af9f771a291"

http_archive(
    name = "contrib_rules_jvm",
    sha256 = CONTRIB_RULES_JVM_SHA,
    strip_prefix = "rules_jvm-%s" % CONTRIB_RULES_JVM_VERSION,
    url = "https://github.com/bazel-contrib/rules_jvm/archive/refs/tags/v%s.tar.gz" % CONTRIB_RULES_JVM_VERSION,
)

load("@contrib_rules_jvm//:repositories.bzl", "contrib_rules_jvm_deps")

contrib_rules_jvm_deps()

load("@contrib_rules_jvm//:setup.bzl", "contrib_rules_jvm_setup")

contrib_rules_jvm_setup()

# End of rules_jvm
#################################################################################
