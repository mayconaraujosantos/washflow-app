package com.washflow.application.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Precedence: process environment, then the .env file. Production never reads the .env values,
 * except APP_ENV itself (which is how it knows it is production).
 */
public final class CompositeEnvSource implements EnvSource {

  static final String APP_ENV = "APP_ENV";

  private final List<EnvSource> sources;

  private CompositeEnvSource(List<EnvSource> sources) {
    this.sources = sources;
  }

  public static EnvSource fromEnvironmentAndDotenv() {
    Map<String, String> dotenv =
        Dotenv.configure().ignoreIfMissing().load().entries().stream()
            .collect(Collectors.toMap(DotenvEntry::getKey, DotenvEntry::getValue));
    return of(System.getenv(), dotenv);
  }

  public static EnvSource of(Map<String, String> processEnv, Map<String, String> dotenv) {
    EnvSource process = fromMap(processEnv);
    EnvSource file = fromMap(dotenv);
    String environment =
        process.get(APP_ENV).or(() -> file.get(APP_ENV)).orElse(AppEnvironment.DEVELOPMENT);
    EnvSource appEnv = key -> APP_ENV.equals(key) ? Optional.of(environment) : Optional.empty();
    if (AppEnvironment.isProduction(environment)) {
      return new CompositeEnvSource(List.of(appEnv, process));
    }
    return new CompositeEnvSource(List.of(appEnv, process, file));
  }

  private static EnvSource fromMap(Map<String, String> values) {
    return key -> Optional.ofNullable(values.get(key)).filter(value -> !value.isBlank());
  }

  @Override
  public Optional<String> get(String key) {
    return sources.stream().map(source -> source.get(key)).flatMap(Optional::stream).findFirst();
  }
}
