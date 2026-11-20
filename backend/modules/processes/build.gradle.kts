plugins {
    id("mj.module-conventions")
}

dependencies {
    implementation(project(":modules:clients"))
    implementation(project(":modules:lawyers"))
    implementation(project(":modules:tribunals"))
}
