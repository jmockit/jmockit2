package acceptanceTests;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.junit.*;

import org.jmockit.*;
import static org.jmockit.Expectation.*;

public final class RecordingAndVerificationAPITest
{
   @Mocked Dependency dependency;

   @Test
   public void recordAndVerifyExpectations(@Mocked Consumer<String> mockAction)
   {
      Value value = new Value();

      record(() -> mockAction.accept(any())).result = 1;
      record(() -> mockAction.andThen(null)).result = new IOException();
      record(dependency::isEmpty).result = true;
      record(() -> dependency.remove(same(value))).result = true;

      record(() -> dependency.doSomething(any())).delegate = new Delegate() {
         @SuppressWarnings("unused")
         void delegate(List<?> list) { System.out.println(list); }
      };

      mockAction.accept("");
      mockAction.andThen(System.out::println);
      dependency.clear();
      dependency.add(1);
      dependency.doSomethingElse(true, "testing");
      dependency.add(2);
      dependency.remove(value);

      verify(() -> dependency.add(any(), null));
      verify(() -> mockAction.accept("")).minTimes = 1;
      verify(() -> mockAction.andThen(notNull())).times = 0;
      verify(() -> dependency.add(as(i -> i > 1), notNull())).maxTimes = 2;
      verify(() -> dependency.add(as(item -> item instanceof String)));

      verifyInOrder(() -> {
         dependency.add(1);
         dependency.add(2);
      });

      verifyAll(() -> {
         mockAction.accept(any());
         mockAction.andThen(null);
      });
   }

   static class Dependency
   {
      final int i;
      final String s;

      static Dependency create() { return new Dependency(); }

      Dependency() { i = 0; s = ""; }
      Dependency(int i, String s) { this.i = i; this.s = s; }

      boolean add(Object o) { return o != null; }
      boolean add(int index, Object o) { return index >= 0 && o != null; }
      boolean remove(Object o) { return o == null; }
      void clear() {}
      boolean isEmpty() { return false; }
      <T> void doSomething(List<T> list) { System.out.println(list); }
      <T> void doSomethingElse(boolean b, String str) { System.out.println(b + str); }
   }

   static class Value {}
}
