/**
 * Composition roots, one per aggregate: each builds the aggregate's use cases (with their JDBI
 * repositories and adapters) and controllers. Routes ask a module for a controller.
 */
package com.washflow.application.modules;
