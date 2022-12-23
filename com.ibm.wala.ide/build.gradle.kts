plugins {
  id("com.diffplug.eclipse.mavencentral")
  id("com.ibm.wala.gradle.java")
}

eclipse.project.natures("org.eclipse.pde.PluginNature")

eclipseMavenCentral {
  release(rootProject.extra["eclipseVersion"] as String) {
    api("org.eclipse.pde.core")
    listOf(
            "org.eclipse.core.commands",
            "org.eclipse.core.jobs",
            "org.eclipse.core.resources",
            "org.eclipse.core.runtime",
            "org.eclipse.equinox.common",
            "org.eclipse.jdt.core",
            "org.eclipse.jface",
            "org.eclipse.osgi",
            "org.eclipse.swt",
            "org.eclipse.ui.workbench",
        )
        .forEach { implementation(it) }
    useNativesForRunningPlatform()
    constrainTransitivesToThisRelease()
  }
}

dependencies {
  implementation(project(":com.ibm.wala.core"))
  implementation(project(":com.ibm.wala.util"))
}

configurations.all {
  resolutionStrategy.dependencySubstitution {
    substitute(module("xml-apis:xml-apis-ext"))
        .using(module("org.eclipse.birt.runtime:org.w3c.css.sac:1.3.1.v200903091627"))
        .because(
            "both provide several of the same classes, but org.w3c.css.sac includes everything we need from both")
  }
}