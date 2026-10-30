/**
 * Composition root: the only layer allowed to know about every other layer. Wires infra adapters
 * into use cases, use cases into controllers, and controllers into Javalin routes. Holds the {@code
 * Application} entry point. Named {@code application}, not {@code main}, to avoid colliding with
 * the Gradle {@code main} source set ({@code src/main/java}, {@code src/main/resources}) this
 * package itself lives under.
 */
package com.washflow.application;
