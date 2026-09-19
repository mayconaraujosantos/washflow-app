/**
 * Composition root: the only layer allowed to know about every other layer.
 * Wires infra adapters into use cases, use cases into controllers, and
 * controllers into Javalin routes. Holds the {@code Main} entry point.
 */
package com.washflow.main;
