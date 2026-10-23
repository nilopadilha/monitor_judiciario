rootProject.name = "monitor-judicial"

include(
    ":shared",
    ":modules:authentication",
    ":modules:clients",
    ":modules:tribunals",
    ":modules:processes",
    ":modules:djen",
    ":modules:monitoring",
    ":modules:notifications",
    ":modules:audit",
    ":modules:dashboard",
    ":apps:api",
)
