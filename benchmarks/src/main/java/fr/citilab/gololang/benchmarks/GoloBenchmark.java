package fr.citilab.gololang.benchmarks;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import fr.insalyon.citi.golo.runtime.GoloClassLoader;
import groovy.lang.GroovyClassLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class GoloBenchmark extends AbstractBenchmark {

  private static String GOLO_SRC_DIR = "src/main/golo/".replace('/', File.separatorChar);
  private static String GROOVY_SRC_DIR = "src/main/groovy/".replace('/', File.separatorChar);
  private static String CLOJURE_SRC_DIR = "src/main/clojure/".replace('/', File.separatorChar);

  private static GoloClassLoader goloClassLoader;
  private static GroovyClassLoader groovyClassLoader;

  public static GoloClassLoader goloClassLoader() {
    if (goloClassLoader == null) {
      goloClassLoader = new GoloClassLoader();
    }
    return goloClassLoader;
  }

  public static GroovyClassLoader groovyClassLoader() {
    if (groovyClassLoader == null) {
      groovyClassLoader = new GroovyClassLoader();
    }
    return groovyClassLoader;
  }

  public static Class<?> loadGoloModule(String goloSourceFilename) {
    try (FileInputStream in = new FileInputStream(GOLO_SRC_DIR + goloSourceFilename)) {
      return goloClassLoader().load(goloSourceFilename, in);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Class<?> loadGroovyClass(String groovySourceFilename) {
    try {
      return groovyClassLoader().parseClass(new File(GROOVY_SRC_DIR + groovySourceFilename));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static clojure.lang.Var clojureReference(String clojureSourceFilename, String namespace, String referenceName) {
    try {
      clojure.lang.Compiler.loadFile(CLOJURE_SRC_DIR + clojureSourceFilename);
      return clojure.lang.RT.var(namespace, referenceName);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
