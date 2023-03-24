java_binary(
    name = "totaldiff",
    srcs = glob(["src/main/java/com/mss/**/*.java"]) + glob(["resources/**/*"]),
    main_class = "com.mss.totaldiff.Main",
    deps = [
        "@maven//:com_fasterxml_jackson_core_jackson_core",
    ],
)

load("@rules_jvm_external//:defs.bzl", "artifact")
load("@contrib_rules_jvm//java:defs.bzl", "JUNIT5_DEPS", "java_test_suite")

java_test_suite(
    name = "mytest",
    srcs = glob(["src/test/java/**/*.java"]),
    runner = "junit5",
    test_suffixes = ["Tests.java"],
    runtime_deps = JUNIT5_DEPS,
    deps = [
        ":totaldiff",
        "@maven//:com_fasterxml_jackson_core_jackson_core",
        artifact("org.junit.jupiter:junit-jupiter-api"),
        artifact("org.junit.jupiter:junit-jupiter-params"),
    ],
)