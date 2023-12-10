plugins {
	id("dev.slne.java-library-conventions")
}

dependencies {
	compileOnlyApi(libs.surf.data.api)
	compileOnlyApi(libs.fast.util)
}
